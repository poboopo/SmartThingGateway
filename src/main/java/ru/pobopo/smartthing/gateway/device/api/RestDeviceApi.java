package ru.pobopo.smartthing.gateway.device.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.model.gateway.Observable;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestDeviceApi extends DeviceApi {
    private final static List<String> SUPPORTED_VERSIONS = List.of("0.5", "0.6", "0.7");

    public final static String HEALTH = "/health";
    public final static String SYSTEM_INFO = "/info/system";
    public final static String GET_ACTIONS = "/actions/info";
    public final static String CALL_ACTION = "/actions";
    public final static String GET_CONFIG = "/config/info";
    public final static String CONFIG_VALUES = "/config/values";
    public final static String DELETE_ALL_CONFIG_VALUES = "/config/delete/all";
    public final static String SENSORS = "/sensors";
    public final static String SENSORS_TYPES = "/sensors/types";
    public final static String STATES = "/states";

    public final static String HOOKS = "/hooks";
    public final static String HOOKS_BY_OBSERVABLE = HOOKS + "/by/observable";
    public final static String HOOKS_BY_ID = HOOKS + "/by/id";
    public final static String HOOKS_TEMPLATES = HOOKS + "/templates";
    public final static String HOOK_TEST = HOOKS + "/test";

    public final static String FEATURES = "/features";
    public final static String METRICS = "/metrics";
    public final static String SETTINGS = "/settings";

    private final RestTemplate restTemplate;

    @Override
    public boolean accept(DeviceInfo deviceInfo) {
        if (StringUtils.isBlank(deviceInfo.getIp())) {
            return false;
        }
        return SUPPORTED_VERSIONS.contains(deviceInfo.getVersion());
    }

    @Override
    public ResponseEntity<String> health(DeviceInfo info) {
        return sendRequest(info, HEALTH);
    }

    @Override
    public ResponseEntity<String> getInfo(DeviceInfo info) {
        return sendRequest(info, SYSTEM_INFO);
    }

    public ResponseEntity<String> saveName(DeviceInfo info, String name) {
        return sendRequest(
                info,
                SYSTEM_INFO,
                HttpMethod.PUT,
                Map.of("name", name)
        );
    }

    @Override
    public ResponseEntity<String> getActions(DeviceInfo info) {
        return sendRequest(info, GET_ACTIONS);
    }

    @Override
    public ResponseEntity<String> callAction(DeviceInfo info, String action) {
        return sendRequest(
                info,
                CALL_ACTION + "Stringaction=" + action,
                HttpMethod.PUT,
                null
        );
    }

    @Override
    public ResponseEntity<String> getSensors(DeviceInfo info) {
        return sendRequest(
                info,
                SENSORS
        );
    }

    public ResponseEntity<String> getSensorsTypes(DeviceInfo info) {
        return sendRequest(
                info,
                SENSORS_TYPES
        );
    }

    @Override
    public ResponseEntity<String> getStates(DeviceInfo info) {
        return sendRequest(
                info,
                STATES
        );
    }

    public ResponseEntity<String> getConfigInfo(DeviceInfo info) {
        return sendRequest(info, GET_CONFIG);
    }

    public ResponseEntity<String> getConfigValues(DeviceInfo info) {
        return sendRequest(info, CONFIG_VALUES);
    }

    public ResponseEntity<String> saveConfigValues(DeviceInfo info, Map<String, Object> values) {
        return sendRequest(
                info,
                CONFIG_VALUES,
                HttpMethod.POST,
                values
        );
    }

    public ResponseEntity<String> deleteConfigValue(DeviceInfo info, String name) {
        return sendRequest(
                info,
                CONFIG_VALUES,
                HttpMethod.DELETE,
                Map.of("name", name)
        );
    }

    public ResponseEntity<String> deleteAllConfigValues(DeviceInfo info) {
        return sendRequest(
                info,
                DELETE_ALL_CONFIG_VALUES,
                HttpMethod.DELETE,
                null
        );
    }

    public ResponseEntity<String> getAllHooks(DeviceInfo info) {
        return sendRequest(
                info,
                HOOKS
        );
    }

    public ResponseEntity<String> getHooks(DeviceInfo info, Observable observable) {
        return sendRequest(
                info,
                String.format(
                        "%sStringtype=%s&name=%s",
                        HOOKS_BY_OBSERVABLE,
                        observable.getType(),
                        observable.getName()
                )
        );
    }

    public ResponseEntity<String> getHookById(DeviceInfo info, Observable observable, String id) {
        return sendRequest(
                info,
                String.format(
                        "%sStringtype=%s&name=%s&id=%s",
                        HOOKS_BY_ID,
                        observable.getType(),
                        observable.getName(),
                        id
                )
        );
    }

    public ResponseEntity<String> getHooksTemplates(DeviceInfo info, String type) {
        return sendRequest(
                info,
                HOOKS_TEMPLATES + "Stringtype=" + type
        );
    }

    public ResponseEntity<String> createHook(DeviceInfo info, Observable observable, Map<String, Object> hook) {
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

    public ResponseEntity<String> updateHook(DeviceInfo info, Observable observable, Map<String, Object> hook) {
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

    public ResponseEntity<String> deleteHook(DeviceInfo info, Observable observable, String id) {
        return sendRequest(
                info,
                String.format(
                        "%sStringtype=%s&name=%s&id=%s",
                        HOOKS_BY_ID,
                        observable.getType(),
                        observable.getName(),
                        id
                ),
                HttpMethod.DELETE,
                null
        );
    }

    public ResponseEntity<String> getFeatures(DeviceInfo info) {
        return sendRequest(info, FEATURES, HttpMethod.GET, null);
    }

    public ResponseEntity<String> getMetrics(DeviceInfo info) {
        return sendRequest(
                info,
                METRICS
        );
    }

    public ResponseEntity<String> exportSettings(DeviceInfo info) {
        return sendRequest(
                info,
                SETTINGS,
                HttpMethod.GET,
                null
        );
    }

    public ResponseEntity<String> importSettings(DeviceInfo info, Map<String, Object> settings) {
        return sendRequest(
                info,
                SETTINGS,
                HttpMethod.POST,
                settings
        );
    }

    public ResponseEntity<String> testHook(DeviceInfo info, Observable observable, String id, String value) {
        return this.sendRequest(
                info,
                String.format(
                        "%sStringtype=%s&name=%s&id=%s&value=%s",
                        HOOK_TEST,
                        observable.getType(),
                        observable.getName(),
                        id,
                        value == null ? "" : value
                )
        );
    }

    public ResponseEntity<String> restart(DeviceInfo info) {
        return this.sendRequest(info, "/danger/restart", HttpMethod.POST, null);
    }

    public ResponseEntity<String> wipe(DeviceInfo info) {
        return this.sendRequest(info, "/danger/wipe", HttpMethod.POST, null);
    }

    protected ResponseEntity<String> sendRequest(DeviceInfo info, String path) {
        return sendRequest(info, path, HttpMethod.GET, null);
    }

    protected ResponseEntity<String> sendRequest(DeviceInfo info, String path, HttpMethod method, Object payload) {
        String url = buildUrl(info, path);
        log.debug(
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
            log.debug("Request finished: {}", response);
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("Request failed: {}", exception.getMessage());
            return new ResponseEntity<>(exception.getMessage(), exception.getStatusCode());
        } catch (Exception exception) {
            log.error("Failed to send request {}", exception.getMessage(), exception);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
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
