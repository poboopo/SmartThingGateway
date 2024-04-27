package ru.pobopo.smart.thing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ru.pobopo.smart.thing.gateway.service.DeviceLogsService;
import ru.pobopo.smart.thing.gateway.service.DeviceSettingsService;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@Tag(name = "Devices controller", description = "Find devices, call device api and export/import device settings")
public class DeviceController {
    private final DeviceSettingsService settingsService;
    private final DeviceLogsService deviceLogsService;
    private final DeviceApiService deviceApiService;
    private final DevicesSearchJob searchJob;

    @Operation(summary = "Get recent found devices in local network")
    @GetMapping("/found")
    public Set<DeviceInfo> getDevices() {
        return searchJob.getRecentFoundDevices();
    }

    // TODO more info about how ApiSelector works?
    @Operation(
            summary = "Call device api method",
            description = "Api for target device will be selected from possible implementations." +
                    "If there is no api implementation for target device DeviceApiException thrown.",
            responses = @ApiResponse(
                    description = "Returns device api call response"
            )
    )
    @PostMapping("/api")
    public ResponseEntity<String> callApi(@RequestBody DeviceRequest request) {
        DeviceResponse result = deviceApiService.execute(request);
        return new ResponseEntity<>(
                result.getBody(),
                result.getCode()
        );
    }

    @Operation(
            summary = "Save device settings",
            description = "Saves settings in .json file directory from env variable device.settings.dir or in default " +
                    "directory $HOME/.smartthing/device/settings/"
    )
    @PostMapping("/settings")
    public void createSettings(@RequestBody DeviceSettings settings) throws BadRequestException, DeviceSettingsException {
        settingsService.createSettings(settings);
    }

    @Operation(
            summary = "Get saved devices settings",
            description = "Loads saved settings from .json files from directory. Directory path loads from env variable " +
                    "device.settings.dir or uses default path $HOME/.smartthing/device/settings/"
    )
    @GetMapping("/settings")
    public List<DeviceSettings> getSettings() throws DeviceSettingsException {
        return settingsService.getSettings();
    }

    @Operation(summary = "Update existing settings")
    @PutMapping("/settings")
    public void updateSettings(@RequestBody UpdateDeviceSettings settings) throws BadRequestException, DeviceSettingsException {
        settingsService.updateSettings(settings);
    }

    @Operation(summary = "Delete saved settings")
    @DeleteMapping("/settings/{name}")
    public void deleteSettings(
            @PathVariable("name") @Parameter(description = "Settings name") String name
    ) throws DeviceSettingsException, BadRequestException {
        settingsService.deleteSettings(name);
    }

    @Operation(summary = "Get last 100 devices logs messages")
    @GetMapping("/logs")
    public List<DeviceLoggerMessage> getLogs() {
        return deviceLogsService.getLogs();
    }
}
