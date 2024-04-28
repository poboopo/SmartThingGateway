package ru.pobopo.smart.thing.gateway.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.exception.StorageException;
import ru.pobopo.smart.thing.gateway.model.CloudIdentity;
import ru.pobopo.smart.thing.gateway.model.CloudConfig;
import ru.pobopo.smartthing.model.stomp.GatewayEventType;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

@Component
@Slf4j
public class CloudService {
    public static final String AUTH_TOKEN_HEADER = "SmartThing-Token-Gateway";

    private final StorageService storageService;
    private final RestTemplate restTemplate;

    private CloudIdentity cloudIdentity;
    @Getter
    private CloudConfig cloudConfig;

    @Autowired
    public CloudService(StorageService storageService, RestTemplate restTemplate) {
        this.storageService = storageService;
        this.restTemplate = restTemplate;

        try {
            cloudConfig = storageService.loadCloudConfig();
            cloudIdentity = storageService.loadCloudIdentity();
        } catch (StorageException exception) {
            log.warn("Failed to load cloud config or identity: {}", exception.getMessage());
        }
    }

    public CloudIdentity getCloudIdentity() {
        if (cloudIdentity == null) {
            log.info("Cloud identity is null, trying to auth");
            login();
        }
        return cloudIdentity;
    }

    public CloudIdentity login(CloudConfig cloudConfig) throws StorageException {
        this.cloudConfig = cloudConfig;
        try {
            login();
            storageService.saveCloudConfig(cloudConfig);
            return cloudIdentity;
        } catch (Exception exception) {
            this.cloudConfig = null;
            throw exception;
        }
    }

    public void login() {
        cloudIdentity = basicRequest(HttpMethod.GET, "/auth", null, CloudIdentity.class);
        try {
            storageService.saveCloudIdentity(cloudIdentity);
        } catch (StorageException exception) {
            log.error("Failed to save cloud identity: {}", exception.getMessage());
        }
    }

    public void logout() {
        try {
            basicRequest(HttpMethod.POST,"/auth/gateway/logout", null, Void.class);
        } catch (Exception e) {
            log.error("Failed to logout in cloud: {}", e.getMessage());
        }
        removeCloudCache();
    }

    public void removeCloudCache() {
        cloudIdentity = null;
        cloudConfig = null;
        try {
            storageService.saveCloudIdentity(null);
            storageService.saveCloudConfig(null);
        } catch (StorageException exception) {
            log.error("Failed to clear cloud configs: {}", exception.getMessage());
        }
    }

    public void sendResponse(ResponseMessage response) {
        basicRequest(
                HttpMethod.POST,
                "/gateway/requests/response",
                response,
                Void.class
        );
    }

    public void notification(GatewayNotification notification) {
        basicRequest(
                HttpMethod.POST,
                "/gateway/requests/notification",
                notification,
                Void.class
        );
    }

    public void event(GatewayEventType event) {
        basicRequest(
                HttpMethod.POST,
                "/gateway/requests/event?event=" + event.name(),
                null,
                Void.class
        );
    }

    @Nullable
    private <T, P> T basicRequest(HttpMethod method, String path, P payload, Class<T> tClass) {
        if (path == null) {
            path = "";
        }

        try {
            if (cloudConfig == null || StringUtils.isBlank(cloudConfig.getToken())) {
                log.error("No cloud token in cloud config found");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(AUTH_TOKEN_HEADER, cloudConfig.getToken());
            HttpEntity<P> entity = new HttpEntity<>(payload, headers);

            String url = buildUrl(cloudConfig, path);
            log.info("Sending request: url={}, method={}, entity={}", url, method.name(), entity);
            ResponseEntity<T> response = restTemplate.exchange(url, method, entity, tClass);
            log.info("Success!");
            return response.getBody();
        } catch (ResourceAccessException exception) {
            log.error("Request failed: {}", exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            log.error("Request failed: {}", exception.getMessage());
            throw new RuntimeException();
        }
    }

    private String buildUrl(CloudConfig cloudInfo, String path) {
        return String.format(
                "http://%s:%s/%s",
                cloudInfo.getCloudIp(),
                cloudInfo.getCloudPort(),
                !path.isEmpty() && path.charAt(0) == '/' ? path.substring(1) : path
        );
    }
}
