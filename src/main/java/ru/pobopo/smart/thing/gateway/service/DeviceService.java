package ru.pobopo.smart.thing.gateway.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.model.DeviceResponse;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.DeviceRequestMessage;

@Component
@Slf4j
public class DeviceService {
    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> getConfigValues(DeviceInfo info) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                String.format("http://%s/config", info.getIp()),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return response.getBody();
    }

    public boolean addConfigValues(DeviceInfo info, Map<String, Object> values) {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                String.format("http://%s/config/save", info.getIp()),
                values,
                Void.class
        );
        return response.getStatusCode() == HttpStatus.OK;
    }

    public DeviceResponse sendRequest(DeviceRequestMessage requestMessage) throws Exception {
        validateRequest(requestMessage);

        String fullPath = buildFullPath(requestMessage);

        log.info(
                "Sending request [{}] {} - {}",
                requestMessage.getMethod(),
                fullPath,
                StringUtils.isBlank(requestMessage.getPayload()) ? "no payload" : requestMessage.getPayload()
        );

        ResponseEntity<String> response = restTemplate.exchange(
                fullPath,
                HttpMethod.valueOf(requestMessage.getMethod()),
                new HttpEntity<>(requestMessage.getPayload()),
                String.class
        );

        return new DeviceResponse(
                response.getStatusCode().value(),
                response.getBody(),
                response.getHeaders()
        );
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
