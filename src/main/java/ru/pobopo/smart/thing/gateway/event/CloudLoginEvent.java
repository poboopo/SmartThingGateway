package ru.pobopo.smart.thing.gateway.event;

import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import ru.pobopo.smart.thing.gateway.model.AuthenticatedCloudUser;

import java.util.Objects;

public class CloudLoginEvent extends ApplicationEvent {
    private final AuthenticatedCloudUser authenticatedCloudUser;

    public CloudLoginEvent(Object source, AuthenticatedCloudUser authenticatedCloudUser) {
        super(source);
        Objects.requireNonNull(authenticatedCloudUser);
        this.authenticatedCloudUser = authenticatedCloudUser;
    }

    @NonNull
    public AuthenticatedCloudUser getAuthorizedCloudUser() {
        return authenticatedCloudUser;
    }
}
