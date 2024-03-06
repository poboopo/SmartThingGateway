package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.AuthenticatedCloudUser;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;
import ru.pobopo.smart.thing.gateway.service.MessageBrokerService;

import java.util.Objects;

@RestController
@RequestMapping("/cloud")
@RequiredArgsConstructor
public class CloudController {
    private final MessageBrokerService messageService;
    private final CloudService cloudService;
    private final ConfigurationService configurationService;

    @GetMapping("/connection/status")
    public CloudConnectionStatus isConnected() {
        return messageService.getStatus();
    }

    @PutMapping("/connection/connect")
    public boolean connect() throws AccessDeniedException {
        return messageService.connect(
                Objects.requireNonNull(cloudService.getAuthenticatedUser()).getGateway()
        );
    }
    @PutMapping("/connection/disconnect")
    public boolean disconnect() throws AccessDeniedException {
        messageService.disconnect();
        return true;
    }

    @GetMapping("/auth")
    public AuthenticatedCloudUser getAuthUser() throws AccessDeniedException {
        return cloudService.getAuthenticatedUser();
    }

    @PutMapping("/login")
    public AuthenticatedCloudUser auth(@RequestBody CloudAuthInfo cloudInfo) throws ConfigurationException, AccessDeniedException {
        configurationService.updateCloudAuthInfo(cloudInfo);
        try {
            return cloudService.auth();
        } catch (Exception exception) {
            configurationService.updateCloudAuthInfo(null);
            throw exception;
        }
    }

    @DeleteMapping("/logout")
    public void logout() throws ConfigurationException {
        configurationService.updateCloudAuthInfo(null);
        cloudService.logout();
    }

    @GetMapping("/info")
    public CloudAuthInfo getCloudInfo() {
        return configurationService.getCloudAuthInfo();
    }
}
