package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.controller.model.UpdateDeviceSettings;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.exception.DeviceSettingsException;
import ru.pobopo.smart.thing.gateway.model.DeviceSettings;
import ru.pobopo.smart.thing.gateway.service.DeviceSettingsService;

import java.util.List;

@RestController
@RequestMapping("/device/settings")
@RequiredArgsConstructor
public class DeviceSettingsController {
    private final DeviceSettingsService settingsService;

    @GetMapping
    public List<DeviceSettings> getSettings() throws DeviceSettingsException {
        return settingsService.getSettings();
    }

    @PostMapping
    public void createSettings(@RequestBody DeviceSettings settings) throws BadRequestException, DeviceSettingsException {
        settingsService.createSettings(settings);
    }

    @PutMapping
    public void updateSettings(@RequestBody UpdateDeviceSettings settings) throws BadRequestException, DeviceSettingsException {
        settingsService.updateSettings(settings);
    }

    @DeleteMapping("/{name}")
    public void deleteSettings(@PathVariable("name") String name) throws DeviceSettingsException, BadRequestException {
        settingsService.deleteSettings(name);
    }
}
