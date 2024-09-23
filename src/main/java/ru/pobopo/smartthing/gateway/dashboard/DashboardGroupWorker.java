package ru.pobopo.smartthing.gateway.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import ru.pobopo.smartthing.gateway.device.api.RestDeviceApi;
import ru.pobopo.smartthing.model.gateway.ObservableType;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardGroup;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardObservable;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardObservableValueUpdate;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardObservableValue;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class DashboardGroupWorker extends Thread {
    @Getter
    private final DashboardGroup group;
    private final ObjectMapper objectMapper;
    private final RestDeviceApi deviceApi;
    private final Consumer<List<DashboardObservableValueUpdate>> updatesConsumer;

    @Getter
    private final Map<DashboardObservable, Deque<DashboardObservableValue>> values = new ConcurrentHashMap<>();

    public DashboardGroupWorker(
            DashboardGroup group,
            RestDeviceApi deviceApi,
            ObjectMapper objectMapper,
            Consumer<List<DashboardObservableValueUpdate>> updatesConsumer
    ) {
        super("Dashboard-Group-Worker-" + group.getDevice().getName());
        this.group = group;
        this.objectMapper = objectMapper;
        this.deviceApi = deviceApi;
        this.updatesConsumer = updatesConsumer;
    }

    @Override
    public void run() {
        Objects.requireNonNull(group);

        try {
            // todo load old values
            while (!Thread.interrupted()) {
                update();
                Thread.sleep(group.getConfig().getUpdateDelay());
            }
        } catch (InterruptedException exception) {
            log.warn("Worker for group {} interrupted", group);
        }
    }

    public synchronized void update() {
        if (group.getObservables().isEmpty()) {
            return;
        }

        try {
            Map<String, Object> sensors = fetchValues(ObservableType.SENSOR);
            Map<String, Object> states = fetchValues(ObservableType.STATE);

            if (sensors.isEmpty() && states.isEmpty()) {
                return;
            }

            List<DashboardObservableValueUpdate> updates = new ArrayList<>();
            for (DashboardObservable observable: group.getObservables()) {
                Object value;
                switch (observable.getType()) {
                    case STATE -> value = states.get(observable.getName());
                    case SENSOR -> value = sensors.get(observable.getName());
                    default -> throw new IllegalArgumentException("Type " + observable.getType() + " not supported!");
                }

                if (value == null) {
                    continue;
                }

                values.putIfAbsent(observable, new ArrayDeque<>(50));
                Deque<DashboardObservableValue> observableValues = values.get(observable);

                DashboardObservableValue observableValue = new DashboardObservableValue(value, LocalDateTime.now());
                observableValues.addFirst(observableValue);
                if (observableValues.size() >= 50) {
                    observableValues.removeLast();
                }
                updates.add(new DashboardObservableValueUpdate(observable, observableValue));
            }
            
            updatesConsumer.accept(updates);
        } catch (JsonProcessingException exception) {
            log.error("Failed to process values", exception);
        }
    }

    @NonNull
    private Map<String, Object> fetchValues(ObservableType type) throws JsonProcessingException {
        log.debug("Trying to update {} values", type);
        List<DashboardObservable> observables = group.getObservables()
                .stream()
                .filter((obs) -> obs.getType().equals(type))
                .toList();

        if (observables.isEmpty()) {
            log.debug("No {}S, skipping update", type);
            return Map.of();
        }
        ResponseEntity<String> response;
        switch (type) {
            case SENSOR -> response = deviceApi.getSensors(group.getDevice());
            case STATE -> response = deviceApi.getStates(group.getDevice());
            default -> {
                throw new IllegalArgumentException("Type " + type + " not supported!");
            }
        }
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to fetch sensors values");
            return Map.of();
        }
        return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
    }
}
