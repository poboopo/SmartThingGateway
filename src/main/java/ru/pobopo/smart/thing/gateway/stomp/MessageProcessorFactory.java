package ru.pobopo.smart.thing.gateway.stomp;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import ru.pobopo.smart.thing.gateway.stomp.message.BaseMessage;
import ru.pobopo.smart.thing.gateway.stomp.message.GatewayMessageType;
import ru.pobopo.smart.thing.gateway.stomp.processor.MessageProcessor;

@Slf4j
public class MessageProcessorFactory {
    private final Map<GatewayMessageType, MessageProcessor> processorMap = new HashMap<>();

    public void addProcessor(GatewayMessageType type, MessageProcessor processor) {
        processorMap.put(type, processor);
    }

    public MessageProcessor getProcessor(BaseMessage message) {
        if (message.getType() == null) {
            log.warn("Message type is missing!");
            return null;
        }
        return processorMap.get(message.getType());
    }
}
