package ru.pobopo.smartthing.gateway.service.device;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.exception.DeviceSettingsException;
import ru.pobopo.smartthing.gateway.model.device.DeviceSettings;
import ru.pobopo.smartthing.gateway.repository.FileRepository;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSettingsService {
    private final FileRepository<DeviceSettings> fileRepository;

    public Collection<DeviceSettings> getSettings() {
        return fileRepository.getAll();
    }

    public DeviceSettings createSettings(DeviceSettings deviceSettings) {
        Objects.requireNonNull(deviceSettings);
        validateSettings(deviceSettings);

        deviceSettings.setId(UUID.randomUUID());
        fileRepository.add(deviceSettings);
        fileRepository.commit();
        return deviceSettings;
    }

    public DeviceSettings updateSettings(DeviceSettings deviceSettings) throws DeviceSettingsException {
        if (deviceSettings.getId() == null) {
            throw new ValidationException("Settings id can't be blank!");
        }
        validateSettings(deviceSettings);

        Optional<DeviceSettings> settingsOptional = fileRepository.find(s -> deviceSettings.getId().equals(s.getId()));
        if (settingsOptional.isEmpty()) {
            throw new ValidationException("Device settings with id=" + deviceSettings.getId() + " not found");
        }
        DeviceSettings oldSettings = settingsOptional.get();
        try {
            fileRepository.delete(oldSettings);
            DeviceSettings.DeviceSettingsBuilder builder = oldSettings.toBuilder();
            if (StringUtils.isNotBlank(deviceSettings.getName())) {
                builder.name(deviceSettings.getName());
            }
            if (StringUtils.isNotBlank(deviceSettings.getValue())) {
                builder.value(deviceSettings.getValue());
            }
            DeviceSettings updatedSettings = builder.build();
            fileRepository.add(updatedSettings);
            fileRepository.commit();
            return updatedSettings;
        } catch (Exception e) {
            fileRepository.rollback();
            throw new DeviceSettingsException("Failed to update settings");
        }
    }

    public void deleteSettings(UUID id) {
        if (id == null) {
            throw new ValidationException("Settings id can't be blank!");
        }

        Optional<DeviceSettings> settingsOptional = fileRepository.find(s -> id.equals(s.getId()));
        if (settingsOptional.isEmpty()) {
            throw new ValidationException("Device settings with id=" + id + " not found");
        }
        fileRepository.delete(settingsOptional.get());
        fileRepository.commit();
    }

    private void validateSettings(DeviceSettings deviceSettings) {
        if (StringUtils.isBlank(deviceSettings.getName())) {
            throw new ValidationException("Settings name can't be blank!");
        }
        if (StringUtils.isBlank(deviceSettings.getValue())) {
            throw new ValidationException("Settings fields can't be blank!");
        }
    }
}
