package ru.pobopo.smart.thing.gateway.event;

import org.springframework.context.ApplicationEvent;

public class CloudLogoutEvent extends ApplicationEvent {
    public CloudLogoutEvent(Object source) {
        super(source);
    }
}
