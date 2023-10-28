package ru.pobopo.smart.thing.gateway.event;

import org.springframework.context.ApplicationEvent;

public class CloudInfoUpdated extends ApplicationEvent {

    public CloudInfoUpdated(Object source) {
        super(source);
    }
}
