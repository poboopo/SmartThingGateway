package ru.pobopo.smart.thing.gateway.event;

import org.springframework.context.ApplicationEvent;

public class RabbitConnectionCloseEvent extends ApplicationEvent {

    public RabbitConnectionCloseEvent(Object source) {
        super(source);
    }
}
