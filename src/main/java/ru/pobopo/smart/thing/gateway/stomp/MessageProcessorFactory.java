package ru.pobopo.smart.thing.gateway.stomp;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import ru.pobopo.smart.thing.gateway.stomp.processor.MessageProcessor;
import ru.pobopo.smartthing.model.stomp.BaseMessage;
import ru.pobopo.smartthing.model.stomp.GatewayMessageType;

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
