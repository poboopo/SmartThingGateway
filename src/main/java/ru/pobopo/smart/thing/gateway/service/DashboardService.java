package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.pobopo.smart.thing.gateway.exception.DashboardFileException;
import ru.pobopo.smart.thing.gateway.model.DashboardGroup;
import ru.pobopo.smart.thing.gateway.model.DashboardObservable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static ru.pobopo.smart.thing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Slf4j
@Service
public class DashboardService {
    private static final Path DASHBOARD_FILE_DEFAULT_PATH =
            Paths.get(DEFAULT_APP_DIR.toString(), ".smartthing/dashboard/settings.json");

    private final ObjectMapper objectMapper;
    private final File settingsFile;
    @Getter
    private final List<DashboardGroup> groups;

    public DashboardService(@Value("${dashboard.settings.file:}") String file, ObjectMapper objectMapper) throws IOException, DashboardFileException {
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

        log.info("Created new group {}", group);
        return group;
    }

    public void updateGroup(DashboardGroup group) throws IOException, ValidationException {
        int index = findGroupIndexById(groups, group.getId());
        if (index == -1) {
            throw new ValidationException("Can't find group by id " + group.getId());
        }

        groups.get(index).setDevice(group.getDevice());
        groups.get(index).setObservables(
                group.getObservables().stream()
                        .filter((o) -> StringUtils.isNotBlank(o.getName()) && StringUtils.isNotBlank(o.getType()))
                        .toList()
        );
        groups.get(index).setConfig(group.getConfig());

        writeGroupsToFile();
        log.info("Group {} was updated", group.getId());
    }

    public void deleteGroup(UUID id) throws IOException, ValidationException {
        Objects.requireNonNull(id);

        int index = findGroupIndexById(groups, id);
        if (index == -1) {
            throw new ValidationException("Can't find group by id " + id);
        }
        groups.remove(index);

        writeGroupsToFile();
        log.info("Group {} was deleted", id);
    }

    private void writeGroupsToFile() throws IOException {
        log.info("Writing groups in file groups");
        objectMapper.writeValue(settingsFile, groups);
    }

    private int findGroupIndexById(List<DashboardGroup> groups, UUID id) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void updateValues(DashboardGroup group) {
        
    }
}
