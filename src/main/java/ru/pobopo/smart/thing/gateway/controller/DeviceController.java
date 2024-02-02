package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.controller.model.UpdateDeviceSettings;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.exception.DeviceSettingsException;
import ru.pobopo.smart.thing.gateway.jobs.DevicesSearchJob;
import ru.pobopo.smart.thing.gateway.model.*;
import ru.pobopo.smart.thing.gateway.service.DeviceApiService;
import ru.pobopo.smart.thing.gateway.service.DeviceLogsProcessor;
import ru.pobopo.smart.thing.gateway.service.DeviceSettingsService;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceSettingsService settingsService;
    private final DeviceLogsProcessor deviceLogsProcessor;
    private final DeviceApiService deviceApiService;
    private final DevicesSearchJob searchJob;

    @GetMapping("/found")
    public Set<DeviceInfo> getDevices() {
        return searchJob.getRecentFoundDevices();
    }

    @PostMapping("/api")
    public ResponseEntity<String> callApi(@RequestBody DeviceRequest request) {
        DeviceResponse result = deviceApiService.execute(request);
        return new ResponseEntity<>(
                result.getBody(),
                result.getCode()
        );
    }

    @GetMapping("/settings")
    public List<DeviceSettings> getSettings() throws DeviceSettingsException {
        return settingsService.getSettings();
    }

    @PostMapping("/settings")
    public void createSettings(@RequestBody DeviceSettings settings) throws BadRequestException, DeviceSettingsException {
        settingsService.createSettings(settings);
    }

    @PutMapping("/settings")
    public void updateSettings(@RequestBody UpdateDeviceSettings settings) throws BadRequestException, DeviceSettingsException {
        settingsService.updateSettings(settings);
    }

    @DeleteMapping("/settings/{name}")
    public void deleteSettings(@PathVariable("name") String name) throws DeviceSettingsException, BadRequestException {
        settingsService.deleteSettings(name);
    }

    @GetMapping("/logs")
    public List<DeviceLoggerMessage> getLogs() {
        return deviceLogsProcessor.getLogs();
    }
}
