package ru.pobopo.smartthing.gateway.stomp;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.event.CloudLogoutEvent;
import ru.pobopo.smartthing.gateway.exception.LogoutException;
import ru.pobopo.smartthing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smartthing.gateway.service.CloudService;
import ru.pobopo.smartthing.gateway.stomp.processor.MessageProcessor;
import ru.pobopo.smartthing.model.stomp.BaseMessage;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

import java.lang.reflect.Type;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomStompSessionHandler extends StompSessionHandlerAdapter {
    private final static String TOPIC = "/secured/queue/gateway/";

    @Setter
    private Consumer<CloudConnectionStatus> statusConsumer;

    private final MessageProcessorFactory processorFactory;
    private final CloudService cloudService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        log.error("Failed to process payload: {}", new String(payload), exception);
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Connected to cloud STOMP ws! Subscribing to topic: {}", TOPIC);
        session.subscribe(TOPIC, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return BaseMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ResponseMessage message = processPayload(payload);
                if (message != null) {
                    cloudService.sendResponse(message);
                }
            }
        });

        statusConsumer.accept(CloudConnectionStatus.CONNECTED);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("Stomp transport error: {} ({})", exception.getMessage(), exception.getClass());
        if (exception instanceof ConnectionLostException) {
            statusConsumer.accept(CloudConnectionStatus.CONNECTION_LOST);
        }
    }

    private ResponseMessage processPayload(Object payload) {
        log.info("Got message! {}", payload);
        BaseMessage base = (BaseMessage) payload;
        MessageProcessor processor = processorFactory.getProcessor(base);
        if (processor == null) {
            return ResponseMessage.builder()
                    .requestId(base.getId())
                    .success(false)
                    .error("Can't select processor for message")
                    .build();
        }

        try {
            log.info("Starting message processing with class {}", processor.getClass().getName());
            return ResponseMessage.builder()
                    .data(processor.process(payload))
                    .requestId(base.getId())
                    .success(true)
                    .build();
        } catch (LogoutException exception) {
            applicationEventPublisher.publishEvent(new CloudLogoutEvent(this));
            return null;
        } catch (Exception exception) {
            log.error("Failed to process message", exception);
            return ResponseMessage.builder()
                    .requestId(base.getId())
                    .error(exception.getMessage())
                    .stack(ExceptionUtils.getStackTrace(exception))
                    .success(false)
                    .build();
        } finally {
            log.info("Message processing finished");
        }
    }
}