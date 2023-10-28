package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.event.AuthorizedEvent;
import ru.pobopo.smart.thing.gateway.exception.AccessDeniedException;
import ru.pobopo.smart.thing.gateway.model.AuthorizedCloudUser;
import ru.pobopo.smart.thing.gateway.model.CloudInfo;

@Component
@Slf4j
public class CloudService {
    private static final String TOKEN_HEADER = "SmartThing-Token-Gateway";
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private AuthorizedCloudUser authorizedCloudUser;

    @Autowired
    public CloudService(ConfigurationService configurationService, ApplicationEventPublisher applicationEventPublisher) {
        this.configurationService = configurationService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public AuthorizedCloudUser getAuthorizedCloudUser() throws AccessDeniedException {
        if (authorizedCloudUser == null) {
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

    @Nullable
    private <T> T basicGetRequest(String path, Class<T> tClass) throws AccessDeniedException {
        if (path == null) {
            path = "";
        }

        CloudInfo cloudInfo = configurationService.getCloudInfo();
        if (cloudInfo == null) {
            return null;
        }
        if (StringUtils.isBlank(cloudInfo.getCloudIp())) {
            log.error("Cloud ip is blank!");
            return null;
        }

        final HttpGet httpGet = new HttpGet(
                String.format(
                        "http://%s:%s/%s",
                        cloudInfo.getCloudIp(),
                        cloudInfo.getCloudPort(),
                        path.length() > 0 && path.charAt(0) == '/' ? path.substring(1) : path
                )
        );
        httpGet.setHeader(TOKEN_HEADER, cloudInfo.getToken());

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(getConfigRequest());
        builder.setConnectionManager(getConnectionManager());

        log.info("Sending GET request to {}", httpGet.getPath());
        try (CloseableHttpClient httpClient = builder.build(); CloseableHttpResponse response = httpClient.execute(httpGet)) {
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (response.getCode() >= 200 && response.getCode() <= 300) {
                log.info("Got response: {}", responseBody);
                return objectMapper.readValue(responseBody, tClass);
            }
            if (response.getCode() == 403) {
                throw new AccessDeniedException("Failed to authorize in cloud service");
            }
            log.error("Request to {} failed: [{}] {}", path, response.getCode(), responseBody);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private RequestConfig getConfigRequest() {
        return RequestConfig.custom()
            .setConnectionRequestTimeout(5, TimeUnit.SECONDS)
            .build();
    }

    private BasicHttpClientConnectionManager getConnectionManager() {
        ConnectionConfig config = ConnectionConfig.custom()
            .setConnectTimeout(5, TimeUnit.SECONDS)
            .setSocketTimeout(10, TimeUnit.SECONDS)
            .build();

        BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
        cm.setConnectionConfig(config);

        return cm;
    }
}
