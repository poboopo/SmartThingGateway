package ru.pobopo.smart.thing.gateway.stomp.processor;

import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;

public interface MessageProcessor {
    MessageResponse process(Object payload) throws Exception;
}
