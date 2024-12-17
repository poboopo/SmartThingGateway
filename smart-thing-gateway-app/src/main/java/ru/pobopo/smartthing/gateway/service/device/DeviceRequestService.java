package ru.pobopo.smartthing.gateway.service.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.cache.CacheItem;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.exception.DeviceApiException;
import ru.pobopo.smartthing.gateway.model.cloud.CloudIdentity;
import ru.pobopo.smartthing.gateway.model.device.DeviceApiMethod;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.gateway.service.device.api.DeviceApi;
import ru.pobopo.smartthing.model.device.DeviceInfo;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRequestService {
    private final List<DeviceApi> apis;
    private final ObjectMapper objectMapper;
    private final CloudApiService cloudService;
    private final DeviceService deviceService;

    @Value("${device.api.cache.enabled:true}")
    private boolean cacheEnabled;
    @Value("${device.api.cache.ttl:1500}")
    private int cacheTtl;

    private final Map<DeviceRequest, CacheItem<ResponseEntity<String>>> cache = new ConcurrentHashMap<>();

    public ResponseEntity<String> execute(DeviceRequest request) {
        Objects.requireNonNull(request, "Incoming request can't be null!");
        log.info("Executing device request: {}", request);
        ResponseEntity<String> fromCache = getFromCache(request);
        if (fromCache != null) {
            log.info("Got request {} result from cache: {}", request, fromCache);
            return fromCache;
        }

        ResponseEntity<String> result = sendRequest(request);
        if (cacheEnabled) {
            log.info("Saving request {} result {} in cache", request, result);
            cache.put(request, new CacheItem<>(result, LocalDateTime.now()));
        }
        return result;
    }

    public ResponseEntity<String> execute(String target, String command, String params) throws BadRequestException {
        if (StringUtils.isBlank(target)) {
            throw new BadRequestException("Target can't be blank");
        }
        log.info("Parsing request: target={}, command={}, params={}", target, command, params);
        String[] spl = target.split("@");
        DeviceRequest.DeviceRequestBuilder requestBuilder = DeviceRequest.builder().command(command);

        String deviceInfo;
        if (spl.length == 2) {
            requestBuilder.gatewayId(spl[0]);
            deviceInfo = spl[1];
        } else {
            deviceInfo = spl[0];
        }

        requestBuilder.device(deviceInfo);

        if (StringUtils.isNotBlank(params)) {
            Map<String, Object> paramsMap = new HashMap<>();
            String[] paramsArray = params.split(";");
            for (String param : paramsArray) {
                if (StringUtils.isBlank(param)) {
                    continue;
                }
                String[] buff = param.split(":");
                paramsMap.put(buff[0], buff.length == 2 ? buff[1] : null);
            }
            requestBuilder.params(paramsMap);
        }

        return execute(requestBuilder.build());
    }

    private ResponseEntity<String> sendRequest(DeviceRequest request) {
        if (StringUtils.isBlank(request.getGatewayId()) || isSameGateway(request.getGatewayId())) {
            log.info("Executing local request");
            return sendLocalRequest(request);
        }
        log.info("Sending request to gateway id={}", request.getGatewayId());
        return sendRemoteRequest(request);
    }

    public List<DeviceApiMethod> getApiMethods(DeviceInfo deviceInfo) {
        Optional<DeviceInfo> foundDevice = deviceService.findDevice(deviceInfo.getName(), deviceInfo.getIp());
        if (foundDevice.isEmpty()) {
            throw new DeviceApiException("Unknown device!");
        }

        Optional<DeviceApi> optionalApi = apis.stream().filter((api) -> api.accept(foundDevice.get())).findFirst();
        if (optionalApi.isEmpty()) {
            throw new DeviceApiException("Api not found for this target");
        }

        Method[] methods = optionalApi.get().getClass().getMethods();
        return Arrays.stream(methods)
                .filter((method) -> Modifier.isPublic(method.getModifiers())
                        && method.getReturnType().equals(ResponseEntity.class))
                .map(DeviceApiMethod::fromMethod)
                .toList();
    }

    private ResponseEntity<String> sendLocalRequest(DeviceRequest request) {
        Optional<DeviceInfo> deviceInfo = deviceService.findDevice(request.getDevice());
        if (deviceInfo.isEmpty()) {
            throw new DeviceApiException("Unknown device!");
        }

        Optional<DeviceApi> optionalApi = apis.stream().filter((api) -> api.accept(deviceInfo.get())).findFirst();
        if (optionalApi.isEmpty()) {
            throw new DeviceApiException("Api not found for this target");
        }

        DeviceApi api = optionalApi.get();
        Method[] methods = api.getClass().getMethods();
        Method targetMethod = Arrays.stream(methods)
                .filter((method) ->
                        method.getName().equals(request.getCommand()) &&
                                method.getReturnType().equals(ResponseEntity.class)
                )
                .findFirst()
                .orElseThrow(() -> new DeviceApiException(String.format(
                        "There is no such method %s in class %s",
                        request.getCommand(),
                        api.getClass().getName()
                )));
        try {
            log.info(
                    "Calling api: method {} in {} (params={})",
                    targetMethod.getName(),
                    api.getClass().getName(),
                    request.getParams()
            );

            List<Object> args = new ArrayList<>();
            for (Parameter parameter : targetMethod.getParameters()) {
                if (parameter.getType().equals(DeviceInfo.class)) {
                    args.add(deviceInfo.get());
                } else if (parameter.getType().equals(DeviceRequest.class)) {
                    args.add(request);
                } else {
                    Object value = request.getParams().getOrDefault(parameter.getName(), null);
                    if (value == null) {
                        args.add(null);
                        continue;
                    }
                    args.add(objectMapper.convertValue(value, parameter.getType()));
                }
            }

            ResponseEntity<String> result = (ResponseEntity<String>) targetMethod.invoke(api, args.toArray());
            List<String> contentType = result.getHeaders().get("content-type");
            HttpHeaders requiredHeaders = new HttpHeaders();
            requiredHeaders.addAll("content-type", contentType == null ? List.of("text/plain") : contentType);

            return new ResponseEntity<>(result.getBody(), requiredHeaders, result.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to call device api", e);
            throw new DeviceApiException(e.getMessage());
        }
    }

    @SneakyThrows
    private ResponseEntity<String> sendRemoteRequest(DeviceRequest request) {
        //todo handle gateway not found exception
        // add internal exception codes?
        ResponseEntity<String> response = cloudService.sendDeviceRequest(request);
        Objects.requireNonNull(response);
        if (HttpStatus.FORBIDDEN.equals(response.getStatusCode())) {
            throw new BadRequestException("Gateway with id=" + request.getGatewayId() + " not found!");
        }
        return response;
    }

    private ResponseEntity<String> getFromCache(DeviceRequest request) {
        if (!cacheEnabled) {
            return null;
        }

        CacheItem<ResponseEntity<String>> cacheItem = cache.get(request);
        if (cacheItem == null || cacheItem.getAddedTime().until(LocalDateTime.now(), ChronoUnit.MILLIS) > cacheTtl) {
            cache.remove(request);
            return null;
        }
        return cacheItem.getItem();
    }

    private boolean isSameGateway(String gatewayId) {
        CloudIdentity cloudIdentity = cloudService.getCloudIdentity();
        if (cloudIdentity == null || cloudIdentity.getGateway() == null) {
            return true;
        }
        return StringUtils.equals(cloudIdentity.getGateway().getId(), gatewayId);
    }
}
