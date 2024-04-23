package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.pobopo.smart.thing.gateway.exception.DashboardFileException;
import ru.pobopo.smart.thing.gateway.model.DashboardGroup;
import ru.pobopo.smart.thing.gateway.model.DashboardObservable;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardService {
    private static final Path DASHBOARD_FILE_DEFAULT_PATH =
            Paths.get(System.getProperty("user.home"), ".smartthing/dashboard/settings.json");

    private final ObjectMapper objectMapper;
    private final File settingsFile;

    public DashboardService(@Value("${dashboard.settings:}") String dirPath, ObjectMapper objectMapper) throws IOException {
        if (StringUtils.isNotBlank(dirPath) && !StringUtils.endsWith(dirPath, ".json")) {
            throw new IllegalStateException("Wrong dashboard groups settings file name (.json suffix is missing)");
        }

        Path filePath = StringUtils.isBlank(dirPath) ? DASHBOARD_FILE_DEFAULT_PATH : Paths.get(dirPath);

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
    }


    public List<DashboardGroup> getGroups() throws IOException, DashboardFileException {
        try {
            return objectMapper.readValue(settingsFile, new TypeReference<>() {});
        } catch (MismatchedInputException exception) {
            throw new DashboardFileException("Failed to parse file date. Bad json?");
        }
    }

    public DashboardGroup createGroup(DashboardGroup group) throws IOException, ValidationException, DashboardFileException {
        Objects.requireNonNull(group);
        if (group.getDevice() == null) {
            throw new ValidationException("Device can't be null!");
        }

        List<DashboardGroup> groups = getGroups();
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

        writeGroupsToFile(groups);

        log.info("Created new group {}", group);
        return group;
    }

    // todo update device info?

    public void updateGroupObservables(UUID id, List<DashboardObservable> observables) throws IOException, ValidationException, DashboardFileException {
        List<DashboardGroup> groups = getGroups();
        int index = findGroupIndexById(groups, id);
        if (index == -1) {
            throw new ValidationException("Can't find group by id " + id);
        }

        if (CollectionUtils.isEmpty(observables)) {
            groups.get(index).setObservables(List.of());
        } else {
            groups.get(index).setObservables(
                    observables.stream()
                            .filter((o) -> StringUtils.isNotBlank(o.getName()) && StringUtils.isNotBlank(o.getType()))
                            .toList()
            );
        }

        writeGroupsToFile(groups);
        log.info("Group {} was updated", id);
    }

    public void deleteGroup(UUID id) throws IOException, ValidationException, DashboardFileException {
        Objects.requireNonNull(id);

        List<DashboardGroup> groups = getGroups();
        int index = findGroupIndexById(groups, id);
        if (index == -1) {
            throw new ValidationException("Can't find group by id " + id);
        }
        groups.remove(index);

        writeGroupsToFile(groups);
        log.info("Group {} was deleted", id);
    }

    private void writeGroupsToFile(List<DashboardGroup> groups) throws IOException {
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
}
