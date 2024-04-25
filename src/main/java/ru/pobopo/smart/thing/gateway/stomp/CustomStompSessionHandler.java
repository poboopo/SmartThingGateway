package ru.pobopo.smart.thing.gateway.stomp;

import com.fasterxml.jackson.databind.util.ExceptionUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.controller.model.SendNotificationRequest;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;
import ru.pobopo.smart.thing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.model.Notification;
import ru.pobopo.smart.thing.gateway.model.NotificationType;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.stomp.message.BaseMessage;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;
import ru.pobopo.smart.thing.gateway.stomp.processor.MessageProcessor;

import java.lang.reflect.Type;
import java.net.ConnectException;
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
                cloudService.sendResponse(processPayload(payload));
                // session.send("/response", processPayload(payload));?
            }
        });


        cloudService.notification(
                new SendNotificationRequest(new Notification(
                        "I am online!!!",
                        NotificationType.INFO
                ))
        );
        statusConsumer.accept(CloudConnectionStatus.CONNECTED);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("Stomp transport error: {} ({})", exception.getMessage(), exception.getClass());
        if (exception instanceof ConnectionLostException) {
            statusConsumer.accept(CloudConnectionStatus.CONNECTION_LOST);
        }
    }

    private MessageResponse processPayload(Object payload) {
        log.info("Got message! {}", payload);
        BaseMessage base = (BaseMessage) payload;
        MessageProcessor processor = processorFactory.getProcessor(base);
        if (processor == null) {
            return MessageResponse.builder()
                    .requestId(base.getRequestId())
                    .success(false)
                    .error("Can't select processor for message")
                    .build();
        }

        try {
            MessageResponse messageResponse = processor.process(payload);
            messageResponse.setRequestId(base.getRequestId());
            return messageResponse;
        } catch (LogoutException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to process message", exception);
            return MessageResponse.builder()
                    .requestId(base.getRequestId())
                    .error(exception.getMessage())
                    .stack(ExceptionUtils.getStackTrace(exception))
                    .success(false)
                    .build();
        }
    }
}