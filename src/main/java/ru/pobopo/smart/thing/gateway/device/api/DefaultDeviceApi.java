package ru.pobopo.smart.thing.gateway.device.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.device.api.model.Observable;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.InternalHttpResponse;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDeviceApi extends DeviceApi {
    public final static Collection<String> SUPPORTED_VERSIONS = List.of("0.5", "0.6");
    public final static String HEALTH = "/health";
    public final static String SYSTEM_INFO = "/info/system";
    public final static String GET_ACTIONS = "/info/actions";
    public final static String GET_CONFIG = "/info/config";
    public final static String CALL_ACTION = "/actions";
    public final static String CONFIG_VALUES = "/config";
    public final static String SAVE_CONFIG_VALUES = CONFIG_VALUES + "/save";
    public final static String DELETE_ALL_CONFIG_VALUES = CONFIG_VALUES + "/delete/all";
    public final static String SENSORS = "/sensors";
    public final static String STATES = "/states";

    public final static String HOOKS = "/hooks";
    public final static String HOOKS_BY_OBSERVABLE = HOOKS + "/by/observable";
    public final static String HOOKS_BY_ID = HOOKS + "/by/id";
    public final static String HOOKS_TEMPLATES = HOOKS + "/templates";

    public final static String FEATURES = "/features";
    public final static String METRICS = "/metrics";
    public final static String SETTINGS = "/settings";
    public final static String RESTART = "/restart";

    private final RestTemplate restTemplate;

    @Override
    public boolean accept(DeviceInfo deviceInfo) {
        if (StringUtils.isBlank(deviceInfo.getIp())) {
            return false;
        }
        return SUPPORTED_VERSIONS.contains(deviceInfo.getVersion());
    }

    @Override
    public InternalHttpResponse health(DeviceInfo info) {
        return sendRequest(info, HEALTH);
    }

    @Override
    public InternalHttpResponse getInfo(DeviceInfo info) {
        return sendRequest(info, SYSTEM_INFO);
    }

    public InternalHttpResponse saveName(DeviceInfo info, String name) {
        return sendRequest(
                info,
                SYSTEM_INFO,
                HttpMethod.PUT,
                Map.of("name", name)
        );
    }

    @Override
    public InternalHttpResponse getActions(DeviceInfo info) {
        return sendRequest(info, GET_ACTIONS);
    }

    @Override
    public InternalHttpResponse callAction(DeviceInfo info, String action) {
        return sendRequest(
                info,
                CALL_ACTION + "?action=" + action,
                HttpMethod.PUT,
                null
        );
    }

    @Override
    public InternalHttpResponse getSensors(DeviceInfo info) {
        return sendRequest(
                info,
                SENSORS
        );
    }

    @Override
    public InternalHttpResponse getStates(DeviceInfo info) {
        return sendRequest(
                info,
                STATES
        );
    }

    public InternalHttpResponse getConfigInfo(DeviceInfo info) {
        return sendRequest(info, GET_CONFIG);
    }

    public InternalHttpResponse getConfigValues(DeviceInfo info) {
        return sendRequest(info, CONFIG_VALUES);
    }

    public InternalHttpResponse saveConfigValues(DeviceInfo info, Map<String, Object> values) {
        return sendRequest(
                info,
                SAVE_CONFIG_VALUES,
                HttpMethod.POST,
                values
        );
    }

    public InternalHttpResponse deleteConfigValue(DeviceInfo info, String name) {
        return sendRequest(
                info,
                CONFIG_VALUES,
                HttpMethod.DELETE,
                Map.of("name", name)
        );
    }

    public InternalHttpResponse deleteAllConfigValues(DeviceInfo info) {
        return sendRequest(
                info,
                DELETE_ALL_CONFIG_VALUES,
                HttpMethod.DELETE,
                null
        );
    }

    public InternalHttpResponse getAllHooks(DeviceInfo info) {
        return sendRequest(
                info,
                HOOKS
        );
    }

    public InternalHttpResponse getHooks(DeviceInfo info, Observable observable) {
        return sendRequest(
                info,
                String.format(
                        "%s?type=%s&name=%s",
                        HOOKS_BY_OBSERVABLE,
                        observable.getType(),
                        observable.getName()
                )
        );
    }

    public InternalHttpResponse getHookById(DeviceInfo info, Observable observable, String id) {
        return sendRequest(
                info,
                String.format(
                        "%s?type=%s&name=%s&id=%s",
                        HOOKS_BY_ID,
                        observable.getType(),
                        observable.getName(),
                        id
                )
        );
    }

    public InternalHttpResponse getHooksTemplates(DeviceInfo info, String type) {
        return sendRequest(
                info,
                HOOKS_TEMPLATES + "?type=" + type
        );
    }

    public InternalHttpResponse createHook(DeviceInfo info, Observable observable, Map<String, Object> hook) {
        return sendRequest(
                info,
                HOOKS,
                HttpMethod.POST,
                Map.of(
                        "observable", observable,
                        "hook", hook
                )
        );
    }

    public InternalHttpResponse updateHook(DeviceInfo info, Observable observable, Map<String, Object> hook) {
        return sendRequest(
                info,
                HOOKS,
                HttpMethod.PUT,
                Map.of(
                        "observable", observable,
                        "hook", hook
                )
        );
    }

    public InternalHttpResponse deleteHook(DeviceInfo info, Observable observable, String id) {
        return sendRequest(
                info,
                String.format(
                        "%s?type=%s&name=%s&id=%s",
                        HOOKS_BY_ID,
                        observable.getType(),
                        observable.getName(),
                        id
                ),
                HttpMethod.DELETE,
                null
        );
    }

    public InternalHttpResponse getFeatures(DeviceInfo info) {
        if (info.getVersion().equals("0.5")) {
            return InternalHttpResponse.builder()
                    .status(200)
                    .data("{\"web\":true,\"actions\":true,\"sensors\":true,\"states\":true,\"hooks\":true,\"logger\":true}")
                    .build();
        }
        return sendRequest(info, FEATURES, HttpMethod.GET, null);
    }

    public InternalHttpResponse getMetrics(DeviceInfo info) {
        return sendRequest(
                info,
                METRICS
        );
    }

    public InternalHttpResponse exportSettings(DeviceInfo info) {
        return sendRequest(
                info,
                SETTINGS,
                HttpMethod.GET,
                null
        );
    }

    public InternalHttpResponse importSettings(DeviceInfo info, Map<String, Object> settings) {
        return sendRequest(
                info,
                SETTINGS,
                HttpMethod.POST,
                settings
        );
    }

    public InternalHttpResponse restart(DeviceInfo info) {
        return sendRequest(
                info,
                RESTART,
                HttpMethod.PUT,
                null
        );
    }

    private InternalHttpResponse sendRequest(DeviceInfo info, String path) {
        return sendRequest(info, path, HttpMethod.GET, null);
    }

    private InternalHttpResponse sendRequest(DeviceInfo info, String path, HttpMethod method, Object payload) {
        String url = buildUrl(info, path);
        log.info(
                "Sending request [{}] {} - {}",
                method.name(),
                url,
                payload == null ? "no payload" : payload
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    new HttpEntity<>(payload == null ? "" : payload),
                    String.class
            );
            InternalHttpResponse deviceResponse = InternalHttpResponse.builder()
                    .data(response.getBody())
                    .status(response.getStatusCode().value())
                    .headers(response.getHeaders().toSingleValueMap())
                    .build();
            log.info("Request finished: {}", deviceResponse);
            return deviceResponse;
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("Request failed: {}", exception.getMessage());
            return InternalHttpResponse.builder()
                    .data(exception.getMessage())
                    .status(exception.getStatusCode().value())
                    .build();
        } catch (Exception exception) {
            log.error("Failed to send request {}", exception.getMessage(), exception);
            return new InternalHttpResponse(503, null, null);
        }
    }

    private String buildUrl(DeviceInfo info, String path) {
        return String.format(
                "http://%s%s",
                info.getIp(),
                path
        );
    }
}
