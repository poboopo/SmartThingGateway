package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.pobopo.smart.thing.gateway.device.api.DeviceApi;
import ru.pobopo.smart.thing.gateway.exception.DeviceApiException;
import ru.pobopo.smart.thing.gateway.model.DeviceResponse;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceApiService {
    private final List<DeviceApi> apis;
    private final ObjectMapper objectMapper;

    public DeviceResponse execute(DeviceRequest request) {
        for (DeviceApi api : apis) {
            if (api.accept(request)) {
                return callApi(api, request);
            }
        }
        throw new DeviceApiException("Api not found for this target");
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

    private DeviceResponse callApi(DeviceApi api, DeviceRequest request) {
        Method[] methods = api.getClass().getDeclaredMethods();
        Method targetMethod = Arrays.stream(methods)
                .filter((method) ->
                        method.getName().equals(request.getCommand()) &&
                        method.getReturnType().equals(DeviceResponse.class)
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
            return (DeviceResponse) targetMethod.invoke(api, args.toArray());
        } catch (Exception e) {
            log.error("Failed to call device api", e);
            throw new DeviceApiException(e.getMessage());
        }
    }
}
