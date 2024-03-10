package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.StorageException;
import ru.pobopo.smart.thing.gateway.model.CloudIdentity;
import ru.pobopo.smart.thing.gateway.model.CloudConfig;
import ru.pobopo.smart.thing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.MessageBrokerService;

@RestController
@RequestMapping("/cloud")
@RequiredArgsConstructor
public class CloudController {
    private final MessageBrokerService messageService;
    private final CloudService cloudService;

    @GetMapping("/connection/status")
    public CloudConnectionStatus isConnected() {
        return messageService.getStatus();
    }

    @PutMapping("/connection/connect")
    public void connect() {
        messageService.connect();
    }
    @PutMapping("/connection/disconnect")
    public void disconnect() {
        messageService.disconnect();
    }

    @GetMapping("/identity")
    public CloudIdentity getAuthUser() {
        return cloudService.getCloudIdentity();
    }

    @PutMapping("/login")
    public CloudIdentity login(@RequestBody CloudConfig cloudConfig) throws StorageException {
        return cloudService.login(cloudConfig);
    }

    @DeleteMapping("/logout")
    public void logout() {
        cloudService.logout();
        messageService.logout();
    }

    @GetMapping("/config")
    public CloudConfig getCloudConfig() {
        return cloudService.getCloudConfig();
    }
}
