package ru.pobopo.smart.thing.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.controller.model.SendNotificationRequest;
import ru.pobopo.smart.thing.gateway.event.CloudLoginEvent;
import ru.pobopo.smart.thing.gateway.event.CloudLogoutEvent;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.model.AuthenticatedCloudUser;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.stomp.message.MessageResponse;

//TODO rework to feign client?

@Component
@Slf4j
@RequiredArgsConstructor
public class CloudService {
    public static final String AUTH_TOKEN_HEADER = "SmartThing-Token-Gateway";

    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RestTemplate restTemplate;

    private AuthenticatedCloudUser authenticatedCloudUser;

    public void clearAuthorization() {
        this.authenticatedCloudUser = null;
    }

    public AuthenticatedCloudUser getAuthenticatedUser() throws AccessDeniedException {
        if (authenticatedCloudUser == null) {
            log.info("AuthenticatedCloudUser is null, trying to auth");
            auth();
        }
        return authenticatedCloudUser;
    }

    public AuthenticatedCloudUser auth() throws AccessDeniedException {
        authenticatedCloudUser = basicRequest(HttpMethod.GET, "/auth", null, AuthenticatedCloudUser.class);
        if (authenticatedCloudUser != null) {
            log.info("Successfully authorized! {}", authenticatedCloudUser);
            applicationEventPublisher.publishEvent(new CloudLoginEvent(this, authenticatedCloudUser));
        }
        return authenticatedCloudUser;
    }

    public void logout() {
        try {
            basicRequest(HttpMethod.POST,"/auth/gateway/logout", null, Void.class);
        } catch (Exception e) {
            log.error("Failed to logout in cloud: {}", e.getMessage());
        }
        authenticatedCloudUser = null;
        applicationEventPublisher.publishEvent(new CloudLogoutEvent(this));
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

        try {
            String url = buildUrl(cloudInfo, path);
            log.info(
                "Sending request: url={}, method={}, entity={}",
                url,
                method.name(),
                entity
            );
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    tClass
            );
            return response.getBody();
        } catch (ResourceAccessException exception) {
            log.error("Request failed: {}", exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            log.error("Request failed: {}", exception.getMessage());
            throw new  RuntimeException();
        }
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
