package ru.pobopo.smart.thing.gateway.event;

import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import ru.pobopo.smart.thing.gateway.model.AuthorizedCloudUser;

import java.util.Objects;

public class AuthorizedEvent extends ApplicationEvent {
    private final AuthorizedCloudUser authorizedCloudUser;

    public AuthorizedEvent(Object source, AuthorizedCloudUser authorizedCloudUser) {
        super(source);
        Objects.requireNonNull(authorizedCloudUser);
        this.authorizedCloudUser = authorizedCloudUser;
    }

    @NonNull
    public AuthorizedCloudUser getAuthorizedCloudUser() {
        return authorizedCloudUser;
    }
}
