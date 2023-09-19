package ru.pobopo.smart.thing.gateway.rabbitmq;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.GatewayMessageType;
import ru.pobopo.smart.thing.gateway.rabbitmq.processor.MessageProcessor;

@Slf4j
public class MessageProcessorFactory {
    private final Map<GatewayMessageType, MessageProcessor> processorMap = new HashMap<>();

    public void addProcessor(GatewayMessageType type, MessageProcessor processor) {
        processorMap.put(type, processor);
    }

    public MessageProcessor getProcessor(String type) {
        GatewayMessageType messageType = GatewayMessageType.fromValue(type);
        if (messageType == null) {
            log.warn("There is no such message type: {}", type);
            return null;
        }
        return processorMap.get(messageType);
    }
}
