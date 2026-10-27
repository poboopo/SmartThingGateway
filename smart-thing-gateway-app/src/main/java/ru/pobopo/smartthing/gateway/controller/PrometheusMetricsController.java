package ru.pobopo.smartthing.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.gateway.service.device.DeviceRequestService;
import ru.pobopo.smartthing.gateway.service.device.DevicesSearchService;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class PrometheusMetricsController {
    private final DevicesSearchService searchJob;
    private final DeviceRequestService requestService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<String> getMetrics() throws JsonProcessingException {
        List<String> data = new ArrayList<>();

        for (DeviceInfo deviceInfo : searchJob.getRecentFoundDevices()) {
            String device = deviceInfo.getName();
            ResponseEntity<String> sensors = requestService.execute(device, "getSensors", "");
            Map<String, Object> values = objectMapper.readValue(sensors.getBody(), new TypeReference<>() {
            });

            for (Map.Entry<String, Object> e : values.entrySet()) {
                String name = device.replace("-", "_") + "_" + e.getKey();

                if (!(e.getValue() instanceof Number)) {
                    continue;
                }

                data.add(
                        String.format(
                                """
                                        # HELP %1$s Sensor %2$s from %3$s
                                        # TYPE %1$s gauge
                                        %1$s %4$s
                                        """,
                                name, e.getKey(), device, e.getValue()
                        )
                );
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(
                String.join("\n", data),
                headers,
                HttpStatus.OK
        );
    }
}
