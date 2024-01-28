package ru.pobopo.smart.thing.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.exception.DeviceSettingsException;
import ru.pobopo.smart.thing.gateway.model.DeviceSettings;

import java.io.BufferedReader;
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

@Slf4j
@Service
public class DeviceSettingsService {
    private static final Path SETTINGS_DIR_DEFAULT_PATH =
            Paths.get(System.getProperty("user.home"), ".smartthing/device/settings/");

    private final Path directoryPath;

    public DeviceSettingsService(Environment env) throws IOException {
        String path = env.getProperty("device.settings.dir", String.class);
        this.directoryPath = StringUtils.isEmpty(path) ? SETTINGS_DIR_DEFAULT_PATH : Paths.get(path);

        log.info("Using device settings directory={}", directoryPath);
        Files.createDirectories(directoryPath);
    }

    public List<DeviceSettings> getSettings() throws DeviceSettingsException {
        List<Path> files = collectSettingsFiles();
        return files.stream().map(this::fromFilePath).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void saveSettings(DeviceSettings deviceSettings) throws BadRequestException, DeviceSettingsException {
        Objects.requireNonNull(deviceSettings);
        if (StringUtils.isBlank(deviceSettings.getName())) {
            throw new BadRequestException("Settings name can't be blank!");
        }
        if (StringUtils.isBlank(deviceSettings.getSettings())) {
            throw new BadRequestException("Settings fields can't be blank!");
        }
        Path path = Paths.get(directoryPath.toString(), deviceSettings.getName() + ".json");
        try {
            Files.writeString(path, deviceSettings.getSettings());
            log.info("Saved device settings ({}): {}", path, deviceSettings.getSettings());
        } catch (IOException exception) {
            throw new DeviceSettingsException("Failed to save device settings " + deviceSettings.getName(), exception);
        }
    }

    public void deleteSettings(String name) throws BadRequestException, DeviceSettingsException {
        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("Settings name can't be blank!");
        }
        Path path = Paths.get(directoryPath.toString(), name + ".json");
        File file = new File(path.toString());
        if (!file.exists() || file.isDirectory()) {
            throw new BadRequestException("Unknown settings name: " + name);
        }
        if (!file.delete()) {
            throw new DeviceSettingsException("Failed to delete settings " + name);
        }
        log.info("Settings {} deleted", path);
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
            settings.setSettings(String.join("\n", lines));
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
