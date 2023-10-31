package ru.pobopo.smart.thing.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.AuthorizedCloudUser;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;

@CrossOrigin
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final ConfigurationService configurationService;
    private final CloudService cloudService;

    @Autowired
    public AuthController(ConfigurationService configurationService, CloudService cloudService) {
        this.configurationService = configurationService;
        this.cloudService = cloudService;
    }

    @GetMapping
    public AuthorizedCloudUser getAuthorization() throws AccessDeniedException {
        return cloudService.getAuthorizedCloudUser();
    }

    @PutMapping
    public AuthorizedCloudUser auth(@RequestBody CloudAuthInfo cloudInfo) throws ConfigurationException, AccessDeniedException {
        configurationService.updateCloudAuthInfo(cloudInfo);
        return cloudService.authorize();
    }

    @GetMapping("/configuration")
    public CloudAuthInfo getCloudInfo() {
        return configurationService.getCloudAuthInfo();
    }
}
