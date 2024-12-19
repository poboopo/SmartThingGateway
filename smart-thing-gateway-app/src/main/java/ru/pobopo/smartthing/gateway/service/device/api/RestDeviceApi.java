package ru.pobopo.smartthing.gateway.service.device.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestDeviceApi implements DeviceApi {
    public final static String HEALTH = "/health";
    public final static String WIFI = "/wifi";
    public final static String SYSTEM_INFO = "/info/system";
    public final static String GET_ACTIONS = "/actions/info";
    public final static String CALL_ACTION = "/actions/call";
    public final static String ACTION_SCHEDULE = "/actions/schedule";
    public final static String CONFIG_VALUES = "/config";
    public final static String DELETE_ALL_CONFIG_VALUES = "/config/delete/all";
    public final static String SENSORS = "/sensors";

    public final static String HOOKS = "/hooks";
    public final static String HOOKS_BY_ID = HOOKS + "/by/id";
    public final static String HOOKS_TEMPLATES = HOOKS + "/templates";
    public final static String HOOK_TEST = HOOKS + "/test";

    public final static String FEATURES = "/features";
    public final static String METRICS = "/metrics";
    public final static String SETTINGS = "/settings";

    private final RestTemplate restTemplate;

    @Override
    public boolean accept(DeviceInfo deviceInfo) {
        // in the future, there is going to be BLE support,
        // and it will require multiple apis to communicate with devices
        // for that's the only api, so I always return true
        return true;
    }

    public ResponseEntity<String> health(DeviceInfo info) {
        return sendRequest(info, HEALTH);
    }

    public ResponseEntity<String> getWiFi(DeviceInfo info) {
        return sendRequest(info, WIFI);
    }

    public ResponseEntity<String> setWiFi(DeviceInfo info, String ssid, String password, Integer mode) {
        return sendRequest(
                info,
                WIFI,
                HttpMethod.POST,
                Map.of(
                        "ssid", ssid,
                        "password", password,
                        "mode", mode
                )
        );
    }

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

    public ResponseEntity<String> getActions(DeviceInfo info) {
        return sendRequest(info, GET_ACTIONS);
    }

    public ResponseEntity<String> callAction(DeviceInfo info, String name) {
        return sendRequest(
                info,
                CALL_ACTION + "?name=" + name,
                HttpMethod.GET,
                null
        );
    }

    public ResponseEntity<String> actionSchedule(DeviceInfo info, String name, long callDelay) {
        return sendRequest(
                info,
                ACTION_SCHEDULE,
                HttpMethod.PUT,
                Map.of("name", name, "callDelay", callDelay)
        );
    }

    public ResponseEntity<String> getSensors(DeviceInfo info) {
        return sendRequest(
                info,
                SENSORS
        );
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

    public ResponseEntity<String> getHooks(DeviceInfo info, String sensor) {
        return sendRequest(
                info,
                String.format(
                        "%s?sensor=%s",
                        HOOKS,
                        sensor
                )
        );
    }

    public ResponseEntity<String> getHookById(DeviceInfo info, String sensor, String id) {
        return sendRequest(
                info,
                String.format(
                        "%s?sensor=%s&id=%s",
                        HOOKS_BY_ID,
                        sensor,
                        id
                )
        );
    }

    public ResponseEntity<String> getHooksTemplates(DeviceInfo info, String sensor) {
        return sendRequest(
                info,
                HOOKS_TEMPLATES + "?sensor=" + sensor
        );
    }

    public ResponseEntity<String> createHook(DeviceInfo info, String sensor, Map<String, Object> hook) {
        return sendRequest(
                info,
                HOOKS,
                HttpMethod.POST,
                Map.of(
                        "sensor", sensor,
                        "hook", hook
                )
        );
    }

    public ResponseEntity<String> updateHook(DeviceInfo info, String sensor, Map<String, Object> hook) {
        return sendRequest(
                info,
                HOOKS,
                HttpMethod.PUT,
                Map.of(
                        "sensor", sensor,
                        "hook", hook
                )
        );
    }

    public ResponseEntity<String> deleteHook(DeviceInfo info, String sensor, String id) {
        return sendRequest(
                info,
                String.format(
                        "%s?sensor=%s&id=%s",
                        HOOKS,
                        sensor,
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

    public ResponseEntity<String> importSettings(DeviceInfo info, String settings) {
        if (StringUtils.isBlank(settings) || settings.charAt(0) < '0' || settings.charAt(0) > '9') {
            return new ResponseEntity<>("Bad settings dump", HttpStatus.BAD_REQUEST);
        }

        return sendRequest(
                info,
                SETTINGS,
                HttpMethod.POST,
                settings.trim()
        );
    }

    public ResponseEntity<String> testHook(DeviceInfo info, String sensor, String id, String value) {
        return this.sendRequest(
                info,
                String.format(
                        "%s?sensor=%s&id=%s&value=%s",
                        HOOK_TEST,
                        sensor,
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
            return restTemplate.exchange(
                    url,
                    method,
                    new HttpEntity<>(payload == null ? "" : payload),
                    String.class
            );
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
