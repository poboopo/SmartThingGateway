package ru.pobopo.smart.thing.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.AuthorizedCloudUser;
import ru.pobopo.smart.thing.gateway.model.CloudInfo;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;

@CrossOrigin
@RestController
@RequestMapping("/info")
public class InformationController {
    private final ConfigurationService configurationService;
    private final CloudService cloudService;

    @Autowired
    public InformationController(ConfigurationService configurationService, CloudService cloudService) {
        this.configurationService = configurationService;
        this.cloudService = cloudService;
    }

    @GetMapping("/cloud-info")
    public CloudInfo getCloudInfo() {
        return configurationService.getCloudInfo();
    }

    @GetMapping("/authorization")
    public AuthorizedCloudUser getAuthorization() throws AccessDeniedException {
        return cloudService.getAuthorizedCloudUser();
    }

    @PutMapping("/authorization")
    public AuthorizedCloudUser auth(@RequestBody CloudInfo cloudInfo) throws ConfigurationException, AccessDeniedException {
        configurationService.updateCloudInfo(cloudInfo);
        return cloudService.authorize();
    }
}
