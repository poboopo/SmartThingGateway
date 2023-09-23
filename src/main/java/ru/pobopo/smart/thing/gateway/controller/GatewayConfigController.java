package ru.pobopo.smart.thing.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.CloudInfo;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;

@RestController
@RequestMapping("/configuration")
public class GatewayConfigController {
    private final ConfigurationService configurationService;

    @Autowired
    public GatewayConfigController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping("/cloud-info")
    public CloudInfo getCloudInfo() {
        return configurationService.getCloudInfo();
    }

    @PutMapping("/cloud-info/update")
    void updateCloudInfo(@RequestBody CloudInfo cloudInfo) throws ConfigurationException {
        configurationService.updateCloudInfo(cloudInfo);
    }
}
