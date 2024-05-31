package ru.pobopo.smart.thing.gateway.stomp.processor;

public interface MessageProcessor {
    Object process(Object payload) throws Exception;
}
