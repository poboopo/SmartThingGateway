package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.pobopo.smart.thing.gateway.cache.CacheItem;
import ru.pobopo.smart.thing.gateway.device.api.DeviceApi;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.exception.DeviceApiException;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceApiService {
    private final List<DeviceApi> apis;
    private final ObjectMapper objectMapper;
    private final CloudService cloudService;

    @Value("${device.api.cache.enabled:true}")
    private boolean cacheEnabled;
    @Value("${device.api.cache.ttl:1500}")
    private int cacheTtl;

    private final Map<DeviceRequest, CacheItem<InternalHttpResponse>> cache = new ConcurrentHashMap<>();

    public InternalHttpResponse execute(DeviceRequest request) {
        InternalHttpResponse fromCache = getFromCache(request);
        if (fromCache != null) {
            log.debug("Got request {} result from cache: {}", request, fromCache);
            return fromCache;
        }

        InternalHttpResponse result = sendRequest(request);
        if (cacheEnabled) {
            log.debug("Saving request {} result {} in cache", request, result);
            cache.put(request, new CacheItem<>(result, LocalDateTime.now()));
        }
        return result;
    }

    private InternalHttpResponse sendRequest(DeviceRequest request) {
        if (StringUtils.isNotBlank(request.getGatewayId())) {
            return sendRemoteRequest(request);
        }
        return sendLocalRequest(request);
    }

    private InternalHttpResponse sendLocalRequest(DeviceRequest request) {
        Optional<DeviceApi> optionalApi = apis.stream().filter((api) -> api.accept(request)).findFirst();
        if (optionalApi.isEmpty()) {
            throw new DeviceApiException("Api not found for this target");
        }
        DeviceApi api = optionalApi.get();
        Method[] methods = api.getClass().getDeclaredMethods();
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
                    args.add(request.getDevice());
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
        if (response.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
            throw new BadRequestException("Gateway with id=" + request.getGatewayId() + " not found!");
        }
        return InternalHttpResponse.builder()
                .data(response.getBody())
                .status(response.getStatusCode().value())
                .headers(response.getHeaders().toSingleValueMap())
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

    private String toCaps(String camel) {
        StringBuilder res = new StringBuilder();
        for (char value: camel.toCharArray()) {
            if (Character.isUpperCase(value)) {
                res.append("_");
            }
            res.append(Character.toUpperCase(value));
        }
        return res.toString();
    }
}
