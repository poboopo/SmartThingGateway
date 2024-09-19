package ru.pobopo.smartthing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.gateway.annotation.AcceptCloudRequest;
import ru.pobopo.smartthing.gateway.controller.model.UpdateDeviceSettings;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.exception.DeviceSettingsException;
import ru.pobopo.smartthing.gateway.service.DevicesSearchService;
import ru.pobopo.smartthing.gateway.model.*;
import ru.pobopo.smartthing.gateway.service.DeviceLogsService;
import ru.pobopo.smartthing.gateway.repository.DeviceRepository;
import ru.pobopo.smartthing.gateway.service.DeviceSettingsService;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/devices")
@AcceptCloudRequest
@RequiredArgsConstructor
@Tag(name = "Devices controller", description = "Find and save devices, export and import device settings, get devices logs")
public class DeviceController {
    private final DeviceSettingsService settingsService;
    private final DeviceLogsService deviceLogsService;
    private final DevicesSearchService searchJob;
    private final DeviceRepository deviceRepository;

    @Operation(summary = "Get recent found devices in local network")
    @GetMapping("/found")
    public Set<DeviceInfo> getDevices() {
        return searchJob.getRecentFoundDevices();
    }

    @GetMapping("/saved")
    public Collection<DeviceInfo> getSavedDevices() {
        return deviceRepository.getDevices();
    }

    @PostMapping("/saved")
    public DeviceInfo addDevice(@RequestParam String ip) throws BadRequestException {
        return deviceRepository.addDevice(ip);
    }

    @PutMapping("/saved")
    public DeviceInfo updateDeviceInfo(@RequestParam String ip) throws BadRequestException {
        return deviceRepository.updateDeviceInfo(ip);
    }

    @DeleteMapping("/saved")
    public void deleteDevice(@RequestParam String ip) throws BadRequestException {
        deviceRepository.deleteDevice(ip);
    }

    @Operation(
            summary = "Save device settings",
            description = "Saves settings in .json file directory from env variable device.settings.dir or in default " +
                    "directory $HOME/.smartthing/device/settings/"
    )
    @PostMapping("/settings")
    public void createSettings(@RequestBody DeviceSettings settings) {
        settingsService.createSettings(settings);
    }

    @Operation(
            summary = "Get saved devices settings",
            description = "Loads saved settings from .json files from directory. Directory path loads from env variable " +
                    "device.settings.dir or uses default path $HOME/.smartthing/device/settings/"
    )
    @GetMapping("/settings")
    public Collection<DeviceSettings> getSettings() {
        return settingsService.getSettings();
    }

    @Operation(summary = "Update existing settings")
    @PutMapping("/settings")
    public void updateSettings(@RequestBody UpdateDeviceSettings settings) throws DeviceSettingsException {
        settingsService.updateSettings(settings);
    }

    @Operation(summary = "Delete saved settings")
    @DeleteMapping("/settings/{id}")
    public void deleteSettings(@PathVariable("id") @Parameter(description = "Settings id") UUID id) {
        settingsService.deleteSettings(id);
    }

    @Operation(summary = "Get last 100 devices logs messages")
    @GetMapping("/logs")
    public List<DeviceLoggerMessage> getLogs() {
        return deviceLogsService.getLogs();
    }
}
