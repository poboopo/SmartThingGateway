package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.model.GatewayQueueInfo;

@Component
@Slf4j
public class CloudService {
    private static final String TOKEN_HEADER = "SmartThing-Token-Gateway";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String token;
    private final String url;

    @Autowired
    public CloudService(Environment environment) {
        this.token = environment.getProperty("TOKEN");
        this.url = environment.getProperty("CLOUD_URL");
    }

    public GatewayInfo getGatewayInfo() {
        return basicGetRequest("/gateway/info", GatewayInfo.class);
    }

    public GatewayQueueInfo getQueueInfo() {
        return basicGetRequest("/gateway/queue", GatewayQueueInfo.class);
    }

    @Nullable
    private <T> T basicGetRequest(String path, Class<T> tClass) {
        final HttpGet httpGet = new HttpGet(this.url + path);
        httpGet.setHeader(TOKEN_HEADER, token);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(getConfigRequest());
        builder.setConnectionManager(getConnectionManager());

        try (CloseableHttpClient httpClient = builder.build(); CloseableHttpResponse response = httpClient.execute(httpGet)) {
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (response.getCode() == 200) {
                return objectMapper.readValue(responseBody, tClass);
            } else {
                log.error("Request to {} failed: [{}] {}", path, response.getCode(), responseBody);
            }
        } catch (Exception exception) {
            log.error("Request to {} failed", path, exception);
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
