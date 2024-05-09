package ru.pobopo.smart.thing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ru.pobopo.smart.thing.gateway.controller.model.UpdateDeviceSettings;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.exception.DeviceSettingsException;
import ru.pobopo.smart.thing.gateway.model.DeviceSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.pobopo.smart.thing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Slf4j
@Service
public class DeviceSettingsService {
    private static final Path SETTINGS_DIR_DEFAULT_PATH =
            Paths.get(DEFAULT_APP_DIR.toString(), ".smartthing/device/settings/");

    private final Path directoryPath;

    public DeviceSettingsService(@Value("${device.settings.dir:}") String path) throws IOException {
        this.directoryPath = StringUtils.isEmpty(path) ? SETTINGS_DIR_DEFAULT_PATH : Paths.get(path);

        log.info("Using device settings directory={}", directoryPath);
        Files.createDirectories(directoryPath);
    }

    public List<DeviceSettings> getSettings() throws DeviceSettingsException {
        List<Path> files = collectSettingsFiles();
        return files.stream().map(this::fromFilePath).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void updateSettings(UpdateDeviceSettings deviceSettings) throws DeviceSettingsException, BadRequestException {
        if (StringUtils.isBlank(deviceSettings.getName())) {
            throw new BadRequestException("Settings name can't be blank!");
        }
        Path settingsPath = buildSettingsFilePath(deviceSettings.getName());
        if (StringUtils.isNotBlank(deviceSettings.getOldName()) && !StringUtils.equals(deviceSettings.getOldName(), deviceSettings.getName())) {
            if (isSettingsExists(settingsPath)) {
                throw new DeviceSettingsException("Settings with name " + deviceSettings.getName() + " already exists");
            }
            Path oldPath = buildSettingsFilePath(deviceSettings.getOldName());
            if (!isSettingsExists(oldPath)) {
                throw new DeviceSettingsException("There is no settings with name " + deviceSettings.getOldName());
            }
            try {
                Files.copy(oldPath, settingsPath);
                Files.delete(oldPath);
                log.info("Renamed settings file from {} to {}", oldPath, settingsPath);
            } catch (IOException e) {
                throw new DeviceSettingsException("Failed to rename");
            }
        } else if (!isSettingsExists(settingsPath)) {
            throw new DeviceSettingsException("There is no settings with name " + deviceSettings.getName());
        }
        try {
            Files.writeString(settingsPath, deviceSettings.getValue());
            log.info("Update device settings ({}): {}", settingsPath, deviceSettings.getValue());
        } catch (IOException exception) {
            throw new DeviceSettingsException("Failed to save device settings " + deviceSettings.getName(), exception);
        }
    }

    public void createSettings(DeviceSettings deviceSettings) throws BadRequestException, DeviceSettingsException {
        Objects.requireNonNull(deviceSettings);
        if (StringUtils.isBlank(deviceSettings.getName())) {
            throw new BadRequestException("Settings name can't be blank!");
        }
        if (StringUtils.isBlank(deviceSettings.getValue())) {
            throw new BadRequestException("Settings fields can't be blank!");
        }
        Path path = buildSettingsFilePath(deviceSettings.getName());
        if (isSettingsExists(path)) {
            throw new DeviceSettingsException("Settings with name " + deviceSettings.getName() + " already exists!");
        }
        try {
            Files.writeString(path, deviceSettings.getValue());
            log.info("Created device settings ({}): {}", path, deviceSettings.getValue());
        } catch (IOException exception) {
            throw new DeviceSettingsException("Failed to save device settings " + deviceSettings.getName(), exception);
        }
    }

    public void deleteSettings(String name) throws BadRequestException, DeviceSettingsException {
        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("Settings name can't be blank!");
        }
        Path path = buildSettingsFilePath(name);
        File file = new File(path.toString());
        if (!file.exists() || file.isDirectory()) {
            throw new BadRequestException("Unknown settings name: " + name);
        }
        if (!file.delete()) {
            throw new DeviceSettingsException("Failed to delete settings " + name);
        }
        log.info("Settings {} deleted", path);
    }

    private boolean isSettingsExists(Path path) {
        File f = new File(path.toString());
        return f.exists() && !f.isDirectory();
    }

    private Path buildSettingsFilePath(String name) {
        return Paths.get(directoryPath.toString(), name + ".json");
    }

    private DeviceSettings fromFilePath(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            if (lines.isEmpty()) {
                log.error("File {} is empty!", path);
                return null;
            }
            String fileName = path.getFileName().toString();
            DeviceSettings settings = new DeviceSettings();
            settings.setName(fileName.replace(".json", ""));
            settings.setValue(String.join("\n", lines));
            return settings;
        } catch (Exception exception) {
            log.error("Failed to load settings from {}, reason: {}", path, exception.getMessage());
        }
        return null;
    }

    private List<Path> collectSettingsFiles() throws DeviceSettingsException {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(directoryPath)) {
            for (Path child : ds) {
                if (Files.isDirectory(child) || !StringUtils.endsWith(child.getFileName().toString(), ".json")) {
                    // check inner dirs?
                    continue;
                }
                result.add(child);
            }
        } catch (IOException exception) {
            throw new DeviceSettingsException("Can't read settings directory " + directoryPath, exception);
        }
        return result;
    }
}
