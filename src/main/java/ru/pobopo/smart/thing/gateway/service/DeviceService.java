package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.model.DeviceFullInfo;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.model.DeviceResponse;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.DeviceRequestMessage;

@Component
@Slf4j
public class DeviceService {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public DeviceFullInfo getDeviceFullInfo(DeviceInfo info) throws Exception {
        DeviceResponse response = sendRequest(
            new DeviceRequestMessage(
                info.getIp(),
                "/info/system",
                "GET",
                null, null
            )
        );
        if (response.getCode() != 200) {
            log.error("Failed to load device full info. Code {}, response {}", response.getCode(), response.getCode());
            return null;
        }
        return objectMapper.readValue(response.getBody(), DeviceFullInfo.class);
    }

    public DeviceResponse sendRequest(DeviceRequestMessage requestMessage) throws Exception {
        validateRequest(requestMessage);

        String fullPath = buildFullPath(requestMessage);
        final HttpUriRequestBase requestBase = new HttpUriRequestBase(
            requestMessage.getMethod(),
            URI.create(fullPath)
        );

        log.info(
            "Sending request [{}] {} - {}",
            requestMessage.getMethod(),
            fullPath,
            StringUtils.isBlank(requestMessage.getPayload()) ? "no payload" : requestMessage.getPayload()
        );

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(getConfigRequest());
        builder.setConnectionManager(getConnectionManager());

        try (CloseableHttpClient httpClient = builder.build();
            CloseableHttpResponse response = httpClient.execute(requestBase)) {
            return new DeviceResponse(
                response.getCode(),
                EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8),
                response.getHeaders()
            );
        } catch (Exception exception) {
            log.error("Request to {} failed", fullPath, exception);
            throw exception;
        }
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

    private String buildFullPath(DeviceRequestMessage requestMessage) {
        StringBuilder builder = new StringBuilder();
        builder.append("http://").append(requestMessage.getTarget());
        if (!requestMessage.getPath().startsWith("/")) {
            builder.append("/");
        }
        builder.append(requestMessage.getPath());
        return builder.toString();
    }


    private void validateRequest(DeviceRequestMessage request) throws BadRequestException {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(request.getTarget())) {
            errors.add("Target can't be blank!");
        }
        if (StringUtils.isBlank(request.getMethod())) {
            errors.add("Method can't be blank!");
        }
        if (StringUtils.isEmpty(request.getPath())) {
            errors.add("Path can't be blank!");
        }

        if (!errors.isEmpty()) {
            String errorText = "Request validation failed! Errors: ";
            errorText += String.join(", ", errors);
            throw new BadRequestException(errorText);
        }
    }
}
