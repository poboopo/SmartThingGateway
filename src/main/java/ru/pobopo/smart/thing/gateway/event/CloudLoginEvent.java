package ru.pobopo.smart.thing.gateway.event;

import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import ru.pobopo.smart.thing.gateway.model.CloudIdentity;

import java.util.Objects;

public class CloudLoginEvent extends ApplicationEvent {
    private final CloudIdentity cloudIdentity;

    public CloudLoginEvent(Object source, CloudIdentity cloudIdentity) {
        super(source);
        Objects.requireNonNull(cloudIdentity);
        this.cloudIdentity = cloudIdentity;
    }

    @NonNull
    public CloudIdentity getAuthorizedCloudUser() {
        return cloudIdentity;
    }
}
