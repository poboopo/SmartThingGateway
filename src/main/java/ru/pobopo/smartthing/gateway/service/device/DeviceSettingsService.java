package ru.pobopo.smartthing.gateway.service.device;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.model.device.DeviceSettingsDump;
import ru.pobopo.smartthing.gateway.repository.FileRepository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSettingsService {
    private final FileRepository<DeviceSettingsDump> fileRepository;

    public Collection<DeviceSettingsDump> getSettings() {
        return fileRepository.getAll();
    }

    public DeviceSettingsDump createSettings(DeviceSettingsDump deviceSettingsDump) {
        Objects.requireNonNull(deviceSettingsDump);
        validateSettings(deviceSettingsDump);

        deviceSettingsDump.setId(UUID.randomUUID());
        deviceSettingsDump.setCreationDateTime(LocalDateTime.now());
        fileRepository.add(deviceSettingsDump);
        return deviceSettingsDump;
    }

    public void deleteSettings(UUID id) {
        if (id == null) {
            throw new ValidationException("Settings id can't be blank!");
        }

        Optional<DeviceSettingsDump> settingsOptional = fileRepository.findById(id);
        if (settingsOptional.isEmpty()) {
            throw new ValidationException("Device settings with id=" + id + " not found");
        }
        fileRepository.delete(id);
    }

    private void validateSettings(DeviceSettingsDump deviceSettingsDump) {
        if (deviceSettingsDump.getDevice() == null) {
            throw new ValidationException("Device can't be blank!");
        }
        if (StringUtils.isBlank(deviceSettingsDump.getDump())) {
            throw new ValidationException("Dump can't be blank!");
        }
    }
}
