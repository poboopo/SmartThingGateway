package ru.pobopo.smart.thing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cloud connection controller", description = "Login/logout, connect/disconnect, get information")
public class CloudController {
    private final MessageBrokerService messageService;
    private final CloudService cloudService;

    @Operation(
            summary = "Login in cloud",
            description = "Closes active cloud connections, authenticates in cloud, saves new cloud configuration and identity"
    )
    @PostMapping("/login")
    public CloudIdentity login(@RequestBody CloudConfig cloudConfig) throws StorageException {
        return cloudService.login(cloudConfig);
    }

    @Operation(
            summary = "Logout from cloud",
            description = "Logout from cloud, closes active connection and clear cloud configuration"
    )
    @DeleteMapping("/logout")
    public void logout() {
        cloudService.logout();
        messageService.logout();
    }

    @Operation(summary = "Get cloud connection status")
    @GetMapping("/connection/status")
    public CloudConnectionStatus isConnected() {
        return messageService.getStatus();
    }

    @Operation(summary = "Connect to configured cloud")
    @PutMapping("/connection/connect")
    public void connect() {
        messageService.connect();
    }

    @Operation(summary = "Disconnect from cloud")
    @PutMapping("/connection/disconnect")
    public void disconnect() {
        messageService.disconnect();
    }

    @Operation(summary = "Get current cloud connection configuration")
    @GetMapping("/config")
    public CloudConfig getCloudConfig() {
        return cloudService.getCloudConfig();
    }

    @Operation(summary = "Get cached cloud identity")
    @GetMapping("/identity")
    public CloudIdentity getAuthUser() {
        return cloudService.getCloudIdentity();
    }
}
