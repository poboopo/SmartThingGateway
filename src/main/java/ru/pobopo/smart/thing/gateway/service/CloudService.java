package ru.pobopo.smart.thing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.event.AuthorizedEvent;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.model.AuthorizedCloudUser;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.model.GatewayCloudConfig;

@Component
@Slf4j
public class CloudService {
    private static final String TOKEN_HEADER = "SmartThing-Token-Gateway";

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
        authorizedCloudUser = basicGetRequest("/auth", AuthorizedCloudUser.class);
        if (authorizedCloudUser != null) {
            log.info("Successfully authorized! {}", authorizedCloudUser);
            applicationEventPublisher.publishEvent(new AuthorizedEvent(this, authorizedCloudUser));
        }
        return authorizedCloudUser;
    }

    public GatewayCloudConfig getGatewayConfig() throws AccessDeniedException {
        return basicGetRequest("/gateway/management/config", GatewayCloudConfig.class);
    }

    @Nullable
    private <T> T basicGetRequest(String path, Class<T> tClass) throws AccessDeniedException {
        if (path == null) {
            path = "";
        }

        CloudAuthInfo cloudInfo = configurationService.getCloudAuthInfo();
        if (cloudInfo == null || StringUtils.isBlank(cloudInfo.getToken())) {
            return null;
        }
        if (StringUtils.isBlank(cloudInfo.getCloudIp())) {
            log.error("Cloud ip is blank!");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, cloudInfo.getToken());
        HttpEntity<String> entity = new HttpEntity<>(
                "body",
                headers
        );

        ResponseEntity<T> response = restTemplate.exchange(
                String.format(
                        "http://%s:%s/%s",
                        cloudInfo.getCloudIp(),
                        cloudInfo.getCloudPort(),
                        path.length() > 0 && path.charAt(0) == '/' ? path.substring(1) : path
                ),
                HttpMethod.GET,
                entity,
                tClass
        );

        if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            throw new AccessDeniedException("Failed to authorize in cloud service");
        }
        return response.getBody();
    }
}
