package ru.pobopo.smart.thing.gateway.event;

import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@ToString
public class CloudConnectionEvent extends ApplicationEvent {
    private final CloudConnectionEventType eventType;

    public CloudConnectionEvent(Object source, CloudConnectionEventType eventType) {
        super(source);
        this.eventType = eventType;
    }

    public CloudConnectionEventType getEventType() {
        return eventType;
    }
}
