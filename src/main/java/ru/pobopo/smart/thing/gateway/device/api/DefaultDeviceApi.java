package ru.pobopo.smart.thing.gateway.device.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.device.api.model.Observable;
import ru.pobopo.smart.thing.gateway.jobs.DevicesSearchJob;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.model.DeviceRequest;
import ru.pobopo.smart.thing.gateway.model.DeviceResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDeviceApi extends DeviceApi {
    public final static Collection<String> SUPPORTED_VERSIONS = List.of("0.5");
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

    public final static String METRICS = "/metrics";
    public final static String SETTINGS = "/settings";
    public final static String RESTART = "/restart";

    private final DevicesSearchJob searchJob;
    private final RestTemplate restTemplate;

    @Override
    public boolean accept(DeviceRequest request) {
        // todo select by version and then find in recent?
        return searchJob.getRecentFoundDevices()
                .stream().anyMatch((d) ->
                        d.getIp().equals(request.getDevice().getIp()) &&
                                SUPPORTED_VERSIONS.contains(d.getVersion())
                );
    }

    @Override
    public DeviceResponse getInfo(DeviceInfo info) {
        return sendRequest(info, SYSTEM_INFO);
    }

    public DeviceResponse saveName(DeviceInfo info, String name) {
        return sendRequest(
                info,
                SYSTEM_INFO,
                HttpMethod.PUT,
                Map.of("name", name)
        );
    }

    @Override
    public DeviceResponse getActions(DeviceInfo info) {
        return sendRequest(info, GET_ACTIONS);
    }

    @Override
    public DeviceResponse callAction(DeviceInfo info, String action) {
        return sendRequest(
                info,
                CALL_ACTION + "?action=" + action,
                HttpMethod.PUT,
                null
        );
    }

    @Override
    public DeviceResponse getSensors(DeviceInfo info) {
        return sendRequest(
                info,
                SENSORS
        );
    }

    @Override
    public DeviceResponse getStates(DeviceInfo info) {
        return sendRequest(
                info,
                STATES
        );
    }

    public DeviceResponse getConfigInfo(DeviceInfo info) {
        return sendRequest(info, GET_CONFIG);
    }

    public DeviceResponse getConfigValues(DeviceInfo info) {
        return sendRequest(info, CONFIG_VALUES);
    }

    public DeviceResponse saveConfigValues(DeviceInfo info, Map<String, Object> values) {
        return sendRequest(
                info,
                SAVE_CONFIG_VALUES,
                HttpMethod.POST,
                values
        );
    }

    public DeviceResponse deleteConfigValue(DeviceInfo info, String name) {
        return sendRequest(
                info,
                CONFIG_VALUES,
                HttpMethod.DELETE,
                Map.of("name", name)
        );
    }

    public DeviceResponse deleteAllConfigValues(DeviceInfo info) {
        return sendRequest(
                info,
                DELETE_ALL_CONFIG_VALUES,
                HttpMethod.DELETE,
                null
        );
    }

    public DeviceResponse getAllHooks(DeviceInfo info) {
        return sendRequest(
                info,
                HOOKS
        );
    }

    public DeviceResponse getHooks(DeviceInfo info, Observable observable) {
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

    public DeviceResponse getHookById(DeviceInfo info, Observable observable, String id) {
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

    public DeviceResponse getHooksTemplates(DeviceInfo info) {
        return sendRequest(
                info,
                HOOKS_TEMPLATES
        );
    }

    public DeviceResponse createHook(DeviceInfo info, Observable observable, Map<String, Object> hook) {
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

    public DeviceResponse updateHook(DeviceInfo info, Observable observable, Map<String, Object> hook) {
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

    public DeviceResponse deleteHook(DeviceInfo info, Observable observable, String id) {
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

    public DeviceResponse getMetrics(DeviceInfo info) {
        return sendRequest(
                info,
                METRICS
        );
    }

    public DeviceResponse exportSettings(DeviceInfo info) {
        return sendRequest(
                info,
                SETTINGS,
                HttpMethod.GET,
                null
        );
    }

    public DeviceResponse importSettings(DeviceInfo info, Map<String, Object> settings) {
        return sendRequest(
                info,
                SETTINGS,
                HttpMethod.POST,
                settings
        );
    }

    public DeviceResponse restart(DeviceInfo info) {
        return sendRequest(
                info,
                RESTART,
                HttpMethod.PUT,
                null
        );
    }

    private DeviceResponse sendRequest(DeviceInfo info, String path) {
        return sendRequest(info, path, HttpMethod.GET, null);
    }

    private DeviceResponse sendRequest(DeviceInfo info, String path, HttpMethod method, Object payload) {
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
            DeviceResponse deviceResponse = new DeviceResponse(
                    response.getStatusCode(),
                    response.getBody(),
                    response.getHeaders()
            );
            log.info("Request finished: {}", deviceResponse);
            return deviceResponse;
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("Request failed: {}", exception.getMessage());
            return new DeviceResponse(
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString(),
                    exception.getResponseHeaders()
            );
        } catch (Exception exception) {
            log.error("Failed to send request {}", exception.getMessage(), exception);
            return new DeviceResponse(HttpStatusCode.valueOf(503), null, null);
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
