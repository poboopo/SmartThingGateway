package ru.pobopo.smart.thing.gateway.service;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;

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
}
