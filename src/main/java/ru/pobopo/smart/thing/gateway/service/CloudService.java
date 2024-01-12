package ru.pobopo.smart.thing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.controller.model.SendNotificationRequest;
import ru.pobopo.smart.thing.gateway.event.AuthorizedEvent;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.model.AuthorizedCloudUser;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.model.Notification;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;

@Component
@Slf4j
public class CloudService {
    public static final String AUTH_TOKEN_HEADER = "SmartThing-Token-Gateway";

    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RestTemplate restTemplate;

    private AuthorizedCloudUser authorizedCloudUser;

    @Autowired
    public CloudService(ConfigurationService configurationService, ApplicationEventPublisher applicationEventPublisher, RestTemplate restTemplate) {
        this.configurationService = configurationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.restTemplate = restTemplate;
    }

    public void clearAuthorization() {
        this.authorizedCloudUser = null;
    }

    public AuthorizedCloudUser getAuthorizedCloudUser() throws AccessDeniedException {
        if (authorizedCloudUser == null) {
            log.info("AuthorizedCloudUser is null, trying to auth");
            authorize();
        }
        return authorizedCloudUser;
    }

    public AuthorizedCloudUser authorize() throws AccessDeniedException {
        authorizedCloudUser = basicRequest(HttpMethod.GET, "/auth", null, AuthorizedCloudUser.class);
        if (authorizedCloudUser != null) {
            log.info("Successfully authorized! {}", authorizedCloudUser);
            applicationEventPublisher.publishEvent(new AuthorizedEvent(this, authorizedCloudUser));
        }
        return authorizedCloudUser;
    }

    public void sendResponse(MessageResponse response) throws AccessDeniedException {
        basicRequest(
                HttpMethod.POST,
                "/gateway/requests/response",
                response,
                Void.class
        );
    }

    public void notification(SendNotificationRequest notification) throws AccessDeniedException {
        basicRequest(
                HttpMethod.POST,
                "/gateway/requests/notification",
                notification,
                Void.class
        );
    }

    @Nullable
    private <T, P> T basicRequest(HttpMethod method, String path, P payload, Class<T> tClass) throws AccessDeniedException {
        if (path == null) {
            path = "";
        }

        CloudAuthInfo cloudInfo = configurationService.getCloudAuthInfo();
        if (cloudInfo == null || StringUtils.isBlank(cloudInfo.getToken())) {
            log.error("No cloud token");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTH_TOKEN_HEADER, cloudInfo.getToken());
        HttpEntity<P> entity = new HttpEntity<>(
                payload,
                headers
        );

        ResponseEntity<T> response = restTemplate.exchange(
                buildUrl(cloudInfo, path),
                method,
                entity,
                tClass
        );

        if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            throw new AccessDeniedException("Failed to authorize in cloud service");
        }
        return response.getBody();
    }

    private String buildUrl(CloudAuthInfo cloudInfo, String path) {
        return String.format(
                "http://%s:%s/%s",
                cloudInfo.getCloudIp(),
                cloudInfo.getCloudPort(),
                !path.isEmpty() && path.charAt(0) == '/' ? path.substring(1) : path
        );
    }
}
