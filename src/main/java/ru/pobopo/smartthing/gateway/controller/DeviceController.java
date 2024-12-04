package ru.pobopo.smartthing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.gateway.aspect.AcceptCloudRequest;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.model.device.DeviceSettingsDump;
import ru.pobopo.smartthing.gateway.model.logs.DeviceLogsFilter;
import ru.pobopo.smartthing.gateway.service.device.DevicesSearchService;
import ru.pobopo.smartthing.gateway.service.device.log.DeviceLogsCacheService;
import ru.pobopo.smartthing.gateway.service.device.SavedDevicesService;
import ru.pobopo.smartthing.gateway.service.device.DeviceSettingsService;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.SavedDeviceInfo;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@AcceptCloudRequest
@RequiredArgsConstructor
@Tag(name = "Devices controller", description = "Find and save devices, export and import device settings dumps, get devices logs")
public class DeviceController {
    private final DeviceSettingsService settingsService;
    private final DeviceLogsCacheService deviceLogsCacheService;
    private final DevicesSearchService searchJob;
    private final SavedDevicesService savedDevicesService;

    @GetMapping("/search/enabled")
    public boolean searchEnabled() {
        return searchJob.isSearchEnabled();
    }

    @Operation(summary = "Get recent found devices in local network")
    @GetMapping("/found")
    public Set<DeviceInfo> getDevices() {
        return searchJob.getRecentFoundDevices();
    }

    @GetMapping("/saved")
    public Collection<SavedDeviceInfo> getSavedDevices() {
        return savedDevicesService.getDevices();
    }

    @PostMapping("/saved")
    public SavedDeviceInfo addDevice(@RequestParam String ip) throws BadRequestException {
        return savedDevicesService.addDevice(ip);
    }

    @PutMapping("/saved")
    public SavedDeviceInfo updateDeviceInfo(@RequestParam String ip) throws BadRequestException {
        return savedDevicesService.updateDeviceInfo(ip);
    }

    @DeleteMapping("/saved")
    public void deleteDevice(@RequestParam String ip) throws BadRequestException {
        savedDevicesService.deleteDevice(ip);
    }

    @Operation(
            summary = "Get saved devices settings dumps",
            description = "Loads saved settings from .json files from directory. Directory path loads from env variable " +
                    "device.settings.dir or uses default path $HOME/.smartthing/device_settings/"
    )
    @GetMapping("/settings")
    public Collection<DeviceSettingsDump> getSettings() {
        return settingsService.getSettings();
    }

    @Operation(
            summary = "Save device settings dump",
            description = "Saves settings dump in .json file. Files stored in directory from env variable device.settings.dir or in default " +
                    "directory $HOME/.smartthing/device_settings/"
    )
    @PostMapping("/settings")
    public DeviceSettingsDump createSettings(@RequestBody DeviceSettingsDump settings) {
        return settingsService.createSettings(settings);
    }

    @Operation(summary = "Delete saved settings dump")
    @DeleteMapping("/settings/{id}")
    public void deleteSettings(@PathVariable("id") @Parameter(description = "Settings id") UUID id) {
        settingsService.deleteSettings(id);
    }

    @Operation(summary = "Get last N logs messages from devices")
    @GetMapping("/logs")
    public List<DeviceLoggerMessage> getLogs(
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) Level level
    ) {
        return deviceLogsCacheService.getLogs(DeviceLogsFilter.builder()
                .device(device != null ? device.toLowerCase() : null)
                .tag(tag != null ? tag.toLowerCase() : null)
                .message(message != null ? message.toLowerCase() : null)
                .level(level)
                .build());
    }
}
