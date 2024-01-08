package ru.pobopo.smart.thing.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.MessageBrokerService;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@CrossOrigin
@RestController
@RequestMapping("/connection")
public class CloudConnectionController {
    private final MessageBrokerService messageService;
    private final CloudService cloudService;

    @Autowired
    public CloudConnectionController(MessageBrokerService messageService, CloudService cloudService) {
        this.messageService = messageService;
        this.cloudService = cloudService;
    }

    @GetMapping("/status")
    public boolean isConnected() {
        return messageService.isConnected();
    }

    @PutMapping("/connect")
    public boolean connect() throws AccessDeniedException, IOException, TimeoutException {
        return messageService.connect(Objects.requireNonNull(cloudService.getAuthorizedCloudUser()).getGateway());
    }
}
