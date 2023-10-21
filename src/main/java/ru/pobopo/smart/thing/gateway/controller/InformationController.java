package ru.pobopo.smart.thing.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smart.thing.gateway.model.GatewayStatus;
import ru.pobopo.smart.thing.gateway.service.GatewayStatusService;

@CrossOrigin
@RestController
@RequestMapping("/info")
public class InformationController {
    private final GatewayStatusService statusService;

    @Autowired
    public InformationController(GatewayStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/status")
    public GatewayStatus getStatus() {
        return statusService.getStatus();
    }
}
