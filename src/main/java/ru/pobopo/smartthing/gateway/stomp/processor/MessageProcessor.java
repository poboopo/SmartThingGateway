package ru.pobopo.smartthing.gateway.stomp.processor;

public interface MessageProcessor {
    Object process(Object payload) throws Exception;
}
