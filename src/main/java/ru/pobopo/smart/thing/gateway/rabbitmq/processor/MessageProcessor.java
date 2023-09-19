package ru.pobopo.smart.thing.gateway.rabbitmq.processor;

import ru.pobopo.smart.thing.gateway.rabbitmq.message.MessageResponse;

public interface MessageProcessor {
    MessageResponse process(String message) throws Exception;
}
