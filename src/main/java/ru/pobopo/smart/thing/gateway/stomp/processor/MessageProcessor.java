package ru.pobopo.smart.thing.gateway.stomp.processor;

import ru.pobopo.smartthing.model.stomp.ResponseMessage;

public interface MessageProcessor {
    ResponseMessage process(Object payload) throws Exception;
}
