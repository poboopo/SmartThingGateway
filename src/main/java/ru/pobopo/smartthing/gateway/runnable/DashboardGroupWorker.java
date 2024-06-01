package ru.pobopo.smartthing.gateway.runnable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.pobopo.smartthing.gateway.model.DashboardGroup;
import ru.pobopo.smartthing.gateway.model.DashboardValues;
import ru.pobopo.smartthing.gateway.model.DashboardObservable;
import ru.pobopo.smartthing.gateway.service.DeviceApiService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;

import static ru.pobopo.smartthing.gateway.service.DashboardService.DASHBOARD_TOPIC_PREFIX;

@Slf4j
public class DashboardGroupWorker extends Thread {
    @Getter
    private final DashboardGroup group;
    private final DeviceApiService apiService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate template;

    @Getter
    private final DashboardValues values = new DashboardValues();

    public DashboardGroupWorker(DashboardGroup group, DeviceApiService apiService, ObjectMapper objectMapper,  SimpMessagingTemplate template) {
        super("Dashboard-Group-Worker-" + group.getId());
        this.group = group;
        this.apiService = apiService;
        this.objectMapper = objectMapper;
        this.template = template;
    }

    public synchronized void update() {
        if (!group.getObservables().isEmpty()) {
            updateSensors();
            updateStates();
            values.setLastUpdate(LocalDateTime.now());
            template.convertAndSend(DASHBOARD_TOPIC_PREFIX + "/" + group.getId(), values);
        }
    }

    @Override
    public void run() {
        Objects.requireNonNull(group);
        log.info("Started dashboard group worker for group {}", group);

        try {
            while (!Thread.interrupted()) {
                update();
                Thread.sleep(group.getConfig().getUpdateDelay());
            }
        } catch (InterruptedException exception) {
            log.warn("Worker for group {} interrupted", group);
        }
    }

    private void updateSensors() {
        updateValues(
                "sensor",
                "getSensors",
                (Object value) -> objectMapper.convertValue(value, SensorBody.class).value(),
                (String name, Integer value) -> Objects.equals(values.getSensors().get(name), value),
                (String name, Integer value) -> values.getSensors().put(name, value)
        );
    }

    private void updateStates() {
        updateValues(
                "state",
                "getStates",
                (Object v) -> (String) v,
                (String name, String value) -> StringUtils.equals(values.getStates().get(name), value),
                (String name, String value) -> values.getStates().put(name, value)
        );
    }

    private <V> void updateValues(String type, String command, Converter<V> converter, Comparator<V> comparator, BiConsumer<String, V> consumer) {
        if (StringUtils.isBlank(type)) {
            throw new IllegalStateException("Type can't be blank!");
        }
        log.info("Trying to update {} values", type);
        List<String> names = group.getObservables()
                .stream()
                .filter((obs) -> obs.getType().equals(type))
                .map(DashboardObservable::getName)
                .toList();

        if (names.isEmpty()) {
            log.info("No {}s, skipping update", type);
            return;
        }
        InternalHttpResponse response = apiService.execute(
                DeviceRequest.builder().device(group.getDevice()).command(command).build()
        );
        try {
            Map<String, Object> result = objectMapper.readValue(response.getData(), new TypeReference<>() {
            });
            for (String name : names) {
                if (!result.containsKey(name)) {
                    log.error("There is no value for observable [{}]{}", type, name);
                    continue;
                }
                V value = converter.convert(result.get(name));
                if (!comparator.compare(name, value)) {
                    consumer.accept(name, value);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to process response: {}", e.getMessage(), e);
        }
    }

    record SensorBody(int value, String type) {}

    @FunctionalInterface
    interface Converter<V> {
        V convert(Object value);
    }

    @FunctionalInterface
    interface Comparator<V> {
        boolean compare(String name, V v2);
    }
}
