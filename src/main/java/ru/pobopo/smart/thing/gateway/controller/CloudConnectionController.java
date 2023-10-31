package ru.pobopo.smart.thing.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.RabbitConnectionService;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@CrossOrigin
@RestController
@RequestMapping("/connection")
public class CloudConnectionController {
    private final RabbitConnectionService connectionService;
    private final CloudService cloudService;

    @Autowired
    public CloudConnectionController(RabbitConnectionService connectionService, CloudService cloudService) {
        this.connectionService = connectionService;
        this.cloudService = cloudService;
    }

    @GetMapping("/connected")
    public boolean isConnected() {
        return connectionService.isConnected();
    }

    @PutMapping("/connect")
    public boolean connect() throws AccessDeniedException, IOException, TimeoutException {
        return connectionService.connect(Objects.requireNonNull(cloudService.getAuthorizedCloudUser()).getGateway());
    }
}
