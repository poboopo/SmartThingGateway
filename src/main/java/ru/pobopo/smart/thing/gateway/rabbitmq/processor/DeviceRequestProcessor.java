package ru.pobopo.smart.thing.gateway.rabbitmq.processor;

import ru.pobopo.smart.thing.gateway.rabbitmq.message.MessageResponse;

public class DeviceRequestProcessor implements MessageProcessor{

    @Override
    public MessageResponse process(String message) {
        return null;
    }
}
