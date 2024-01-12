package ru.pobopo.smart.thing.gateway.stomp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.stomp.message.BaseMessage;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;
import ru.pobopo.smart.thing.gateway.stomp.processor.MessageProcessor;

import java.lang.reflect.Type;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomStompSessionHandler extends StompSessionHandlerAdapter {
    @Getter
    @Setter
    private GatewayInfo gatewayInfo;
    private final MessageProcessorFactory processorFactory;
    private final CloudService cloudService;

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        log.error("Failed to process payload: {}", new String(payload), exception);
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        String topic = "/secured/queue/gateway/";
        log.info("Connected to cloud STOMP ws! Subscribing to topic: {}", topic);
        session.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return BaseMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                log.info("Got message! {}", payload);
                BaseMessage base = (BaseMessage) payload;
                MessageProcessor processor = processorFactory.getProcessor(base);
                if (processor == null) {
                  log.error("Can't find message processor");
                  return;
                }

                try {
                  MessageResponse messageResponse = processor.process(payload);
                  if (messageResponse != null) {
                      messageResponse.setRequestId(base.getRequestId());
                      log.info("Sending response: {}", messageResponse);
                      cloudService.sendResponse(messageResponse);
                  } else {
                      log.warn("Empty message response!");
                  }
                } catch (LogoutException exception) {
                  throw exception;
                } catch (Exception exception) {
                  log.error("Failed to process message", exception);
                }
            }
        });
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        super.handleTransportError(session, exception);
        log.error("Stomp transport error", exception);
    }
}