package ru.pobopo.smartthing.gateway.service.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.cache.CacheItem;
import ru.pobopo.smartthing.gateway.service.device.api.DeviceApi;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.exception.DeviceApiException;
import ru.pobopo.smartthing.gateway.model.cloud.CloudIdentity;
import ru.pobopo.smartthing.gateway.model.device.DeviceApiMethod;
import ru.pobopo.smartthing.gateway.service.cloud.CloudApiService;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceApiService {
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    private final List<DeviceApi> apis;
    private final ObjectMapper objectMapper;
    private final CloudApiService cloudService;
    private final DeviceService deviceService;

    @Value("${device.api.cache.enabled:true}")
    private boolean cacheEnabled;
    @Value("${device.api.cache.ttl:1500}")
    private int cacheTtl;

    private final Map<DeviceRequest, CacheItem<InternalHttpResponse>> cache = new ConcurrentHashMap<>();

    public InternalHttpResponse execute(DeviceRequest request) {
        Objects.requireNonNull(request, "Incoming request can't be null!");
        log.info("Executing device request: {}", request);
        InternalHttpResponse fromCache = getFromCache(request);
        if (fromCache != null) {
            log.info("Got request {} result from cache: {}", request, fromCache);
            return fromCache;
        }

        InternalHttpResponse result = sendRequest(request);
        if (cacheEnabled) {
            log.info("Saving request {} result {} in cache", request, result);
            cache.put(request, new CacheItem<>(result, LocalDateTime.now()));
        }
        return result;
    }

    public InternalHttpResponse execute(String target, String command, String params) throws BadRequestException {
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

        if (IP_PATTERN.matcher(deviceInfo).find()) {
            requestBuilder.device(DeviceInfo.builder().ip(deviceInfo).build());
        } else {
            requestBuilder.device(DeviceInfo.builder().name(deviceInfo).build());
        }

        if (StringUtils.isNotBlank(params)) {
            Map<String, Object> paramsMap = new HashMap<>();
            String[] paramsArray = params.split(";");
            for (String param: paramsArray) {
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

    private InternalHttpResponse sendRequest(DeviceRequest request) {
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
                        && method.getReturnType().equals(InternalHttpResponse.class))
                .map(DeviceApiMethod::fromMethod)
                .toList();
    }

    private InternalHttpResponse sendLocalRequest(DeviceRequest request) {
        Optional<DeviceInfo> deviceInfo = deviceService.findDevice(request.getDevice().getName(), request.getDevice().getIp());
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
                                method.getReturnType().equals(InternalHttpResponse.class)
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
            for (Parameter parameter: targetMethod.getParameters()) {
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
            return (InternalHttpResponse) targetMethod.invoke(api, args.toArray());
        } catch (Exception e) {
            log.error("Failed to call device api", e);
            throw new DeviceApiException(e.getMessage());
        }
    }

    @SneakyThrows
    private InternalHttpResponse sendRemoteRequest(DeviceRequest request) {
        //todo handle gateway not found exception
        // add internal exception codes?
        ResponseEntity<String> response = cloudService.sendDeviceRequest(request);
        Objects.requireNonNull(response);
        if (HttpStatus.FORBIDDEN.equals(response.getStatusCode())) {
            throw new BadRequestException("Gateway with id=" + request.getGatewayId() + " not found!");
        }
        return InternalHttpResponse.builder()
                .data(response.getBody())
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .build();
    }

    private InternalHttpResponse getFromCache(DeviceRequest request) {
        if (!cacheEnabled) {
            return null;
        }

        CacheItem<InternalHttpResponse> cacheItem = cache.get(request);
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
