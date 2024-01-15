package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.MessageBrokerService;

import java.util.Objects;

@RestController
@RequestMapping("/connection")
@RequiredArgsConstructor
public class CloudConnectionController {
    private final MessageBrokerService messageService;
    private final CloudService cloudService;

    @GetMapping("/status")
    public boolean isConnected() {
        return messageService.isConnected();
    }

    @PutMapping("/connect")
    public boolean connect() throws AccessDeniedException {
        return messageService.connect(Objects.requireNonNull(cloudService.getAuthorizedCloudUser()).getGateway());
    }
}
