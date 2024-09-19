package ru.pobopo.smartthing.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.dashboard.DashboardGroupWorkerFactory;
import ru.pobopo.smartthing.gateway.exception.DashboardFileException;
import ru.pobopo.smartthing.model.gateway.dashboard.*;
import ru.pobopo.smartthing.gateway.dashboard.DashboardGroupWorker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Slf4j
@Service
public class DashboardService {
    private static final Path DASHBOARD_FILE_DEFAULT_PATH =
            Paths.get(DEFAULT_APP_DIR.toString(), ".smartthing/dashboard.json");

    private final File settingsFile;
    private final ObjectMapper objectMapper;
    private final DashboardGroupWorkerFactory workerFactory;

    @Getter
    private final List<DashboardGroup> groups;
    private final Map<UUID, DashboardGroupWorker> workers = new ConcurrentHashMap<>();

    public DashboardService(
            @Value("${dashboard.settings.file:}") String file,
            ObjectMapper objectMapper,
            DashboardGroupWorkerFactory workerFactory
    ) throws IOException, DashboardFileException {
        if (StringUtils.isNotBlank(file) && !StringUtils.endsWith(file, ".json")) {
            throw new IllegalStateException("Wrong dashboard groups settings file name (.json suffix is missing)");
        }

        Path filePath = StringUtils.isBlank(file) ? DASHBOARD_FILE_DEFAULT_PATH : Paths.get(file);

        log.info("Using dashboard settings file {}", filePath);
        if (Files.isDirectory(filePath)) {
            throw new IllegalStateException(filePath + " can't be directory!");
        }
        if (Files.notExists(filePath)) {
            log.info("No dashboard settings file found, creating...");
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, "[]");
        }

        this.settingsFile = filePath.toFile();
        this.objectMapper = objectMapper;
        this.workerFactory = workerFactory;

        try {
            groups = objectMapper.readValue(settingsFile, new TypeReference<>() {});
            log.info("Loaded groups: {}", groups);
        } catch (MismatchedInputException exception) {
            throw new DashboardFileException("Failed to parse file date. Bad json?");
        }
    }

    public DashboardGroup createGroup(DashboardGroup group) throws IOException, ValidationException {
        Objects.requireNonNull(group);
        if (group.getDevice() == null) {
            throw new ValidationException("Device can't be null!");
        }

        Optional<DashboardGroup> existingGroup = groups.stream()
                .filter((g) -> g.getDevice().equals(group.getDevice()))
                .findFirst();

        if (existingGroup.isPresent()) {
            throw new ValidationException("Group for this device already exists, id=" + existingGroup.get().getId());
        }

        Set<UUID> ids = groups.stream().map(DashboardGroup::getId).collect(Collectors.toSet());
        UUID uuid = UUID.randomUUID();
        while(ids.contains(uuid)) {
            uuid = UUID.randomUUID();
        }
        group.setId(uuid);
        groups.add(group);

        writeGroupsToFile();
        startGroupWorker(group);

        log.info("Created new group {}", group);
        return group;
    }

    public void updateGroup(DashboardGroup group) throws IOException, ValidationException {
        Optional<DashboardGroup> optionalGroup = groups.stream().filter(g -> g.getId().equals(group.getId())).findFirst();
        if (optionalGroup.isEmpty()) {
            throw new ValidationException("Can't find group by id " + group.getId());
        }

        DashboardGroup foundGroup = optionalGroup.get();
        foundGroup.setDevice(group.getDevice());
        foundGroup.setObservables(
                group.getObservables().stream()
                        .filter((o) -> StringUtils.isNotBlank(o.getName()) && o.getType() != null)
                        .toList()
        );
        foundGroup.setConfig(group.getConfig());

        writeGroupsToFile();
        log.info("Group {} was updated", group.getId());

        if (workers.containsKey(group.getId())) {
            log.info("Fetching group values");
            workers.get(group.getId()).update();
        }
    }

    public void deleteGroup(UUID id) throws IOException, ValidationException {
        Objects.requireNonNull(id);

        Optional<DashboardGroup> foundGroup = groups.stream().filter(group -> group.getId().equals(id)).findFirst();
        if (foundGroup.isEmpty()) {
            throw new ValidationException("Can't find group by id " + id);
        }

        DashboardGroupWorker worker = workers.get(id);
        if (worker != null) {
            log.info("Trying to stop group worker");
            worker.interrupt();
            workers.remove(id);
            log.info("Worker stopped and removed");
        }
        groups.remove(foundGroup.get());

        writeGroupsToFile();
        log.info("Group {} was deleted", id);
    }

    public List<DashboardGroupValues> getValues() {
        if (workers.isEmpty()) {
            return List.of();
        }
        return workers.values().stream().map((w) -> new DashboardGroupValues(w.getGroup(), observablesMapToList(w.getValues()))).toList();
    }

    public List<DashboardObservableValues> getGroupValues(UUID id) {
        Objects.requireNonNull(id);

        if (!workers.containsKey(id)) {
            throw new ValidationException("Group with id=" + id + " not found");
        }

        return observablesMapToList(workers.get(id).getValues());
    }

    public void updateValues(UUID id) {
        Objects.requireNonNull(id);

        if (!workers.containsKey(id)) {
            return;
        }
        workers.get(id).update();
    }

    private List<DashboardObservableValues> observablesMapToList(Map<DashboardObservable, Deque<DashboardObservableValue>> values) {
        List<DashboardObservableValues> result = new ArrayList<>();

        for(Map.Entry<DashboardObservable, Deque<DashboardObservableValue>> entry: values.entrySet()) {
            result.add(new DashboardObservableValues(
                    entry.getKey(),
                    entry.getValue())
            );
        }

        return result;
    }

    private void startGroupWorker(DashboardGroup group) {
        if (workers.containsKey(group.getId())) {
            throw new IllegalStateException("Worker for group " + group.getDevice().getName() + " already exists");
        }

        DashboardGroupWorker worker = workerFactory.create(group);
        try {
            log.info("Starting new dashboard worker for group {}", group);
            worker.start();
            workers.put(group.getId(), worker);
        } catch (Exception e) {
            log.error("Failed to start group worker", e);
        }
    }

    @EventListener
    public void startWorkers(ApplicationReadyEvent event) {
        log.info("Trying to start groups workers");
        if (groups.isEmpty()) {
            log.info("Empty groups, nothing to start");
        } else {
            for (DashboardGroup group: groups) {
                startGroupWorker(group);
            }
        }
    }

    private void writeGroupsToFile() throws IOException {
        log.info("Writing groups in file {}", settingsFile);
        objectMapper.writeValue(settingsFile, groups);
    }
}
