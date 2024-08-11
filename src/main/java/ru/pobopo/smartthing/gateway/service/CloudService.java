package ru.pobopo.smartthing.gateway.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.exception.StorageException;
import ru.pobopo.smartthing.gateway.model.CloudIdentity;
import ru.pobopo.smartthing.gateway.model.CloudConfig;
import ru.pobopo.smartthing.model.stomp.*;

// todo need refactor
@Component
@Slf4j
public class CloudService {
    public static final String AUTH_TOKEN_HEADER = "SmartThing-Token-Gateway";

    private final CloudDataRepository cloudDataRepository;
    private final RestTemplate restTemplate;

    @Getter
    private CloudIdentity cloudIdentity;
    @Getter
    private CloudConfig cloudConfig;

    @Autowired
    public CloudService(CloudDataRepository cloudDataRepository, RestTemplate restTemplate) {
        this.cloudDataRepository = cloudDataRepository;
        this.restTemplate = restTemplate;

        try {
            cloudConfig = cloudDataRepository.loadCloudConfig();
            cloudIdentity = cloudDataRepository.loadCloudIdentity();
        } catch (StorageException exception) {
            log.warn("Failed to load cloud config or identity: {}", exception.getMessage());
        }
    }

    public CloudIdentity login(CloudConfig cloudConfig) throws StorageException {
        this.cloudConfig = cloudConfig;
        try {
            login();
            cloudDataRepository.saveCloudConfig(cloudConfig);
            return cloudIdentity;
        } catch (Exception exception) {
            this.cloudConfig = null;
            throw exception;
        }
    }

    public synchronized void login() {
        try {
            ResponseEntity<CloudIdentity> response = basicRequest(HttpMethod.GET, "/auth", null, CloudIdentity.class);
            if (response == null) {
                log.error("Failed to fetch authentication from cloud");
                return;
            }
            cloudIdentity = response.getBody();
            cloudDataRepository.saveCloudIdentity(cloudIdentity);
        } catch (StorageException exception) {
            log.error("Failed to save cloud identity: {}", exception.getMessage());
        }
    }

    public void logout() {
        if (cloudIdentity == null) {
            return;
        }
        try {
            basicRequest(
                    HttpMethod.POST,
                    "/auth/gateway/logout/" + cloudIdentity.getGateway().getId(),
                    null, Void.class
            );
        } catch (Exception e) {
            log.error("Failed to logout in cloud: {}", e.getMessage());
        }
        removeCloudCache();
    }

    public void removeCloudCache() {
        cloudIdentity = null;
        cloudConfig = null;
        try {
            cloudDataRepository.saveCloudIdentity(null);
            cloudDataRepository.saveCloudConfig(null);
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

    public ResponseEntity<String> sendDeviceRequest(DeviceRequest request) {
        return basicRequest(
                HttpMethod.POST,
                "/gateway/requests/device",
                request,
                String.class
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
    private <T, P> ResponseEntity<T> basicRequest(HttpMethod method, String path, P payload, Class<T> tClass) {
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
            return restTemplate.exchange(url, method, entity, tClass);
        } catch (ResourceAccessException exception) {
            log.error("Request failed: {}", exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            log.error("Request failed: {}", exception.getMessage());
            throw new RuntimeException();
        }
    }

    private String buildUrl(CloudConfig cloudInfo, String path) {
        return cloudInfo.getCloudUrl() + (!path.isEmpty() && path.charAt(0) != '/' ? '/' : "") + path;
    }
}
