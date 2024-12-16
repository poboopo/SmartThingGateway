package ru.pobopo.smartthing.gateway.stomp.processor;

import ru.pobopo.smartthing.model.InternalHttpResponse;

public interface MessageProcessor {
    InternalHttpResponse process(Object payload) throws Exception;
}
