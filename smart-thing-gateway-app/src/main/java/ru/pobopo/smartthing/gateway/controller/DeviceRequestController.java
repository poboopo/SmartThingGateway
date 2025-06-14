package ru.pobopo.smartthing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.gateway.aspect.AcceptCloudRequest;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.model.device.DeviceApiMethod;
import ru.pobopo.smartthing.gateway.service.device.DeviceRequestService;
import ru.pobopo.smartthing.model.device.DeviceInfo;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/device/request")
@RequiredArgsConstructor
@Tag(name = "Devices controller", description = "Call device api")
public class DeviceRequestController {
    private final DeviceRequestService deviceRequestService;

    @Operation(
            summary = "Call device api method",
            description = "Api for target device will be selected from possible implementations." +
                    "If there is no api implementation for target device DeviceApiException thrown.",
            responses = @ApiResponse(
                    description = "Returns device api call response"
            )
    )
    @PostMapping
    public ResponseEntity<String> callApi(@RequestBody DeviceRequest request) {
        return deviceRequestService.execute(request);
    }

    @Operation(
            summary = "Call device api method",
            description = "Another method to call device api, but more compact and can be used as webhook",
            parameters = {
                    @Parameter(
                            name = "target",
                            description = "Target device, it can be from other gateway. Can be used device name or ip." +
                                    "Example values: window - target device 'window' in current gateway network." +
                                    "home@window - target device 'window' in gateway's network named 'home'." +
                                    "Requires cloud connection"
                    ),
                    @Parameter(
                            name = "command",
                            description = "Command to call. All device's command list in /api/device/request/commands"
                    ),
                    @Parameter(
                            name = "params",
                            description = "Method params. Key and value should be divided by ':'." +
                                    "Key-value pairs should be divided by ';'." +
                                    "For example, string 'ssid:home;password:123' will be resolved to " +
                                    "map {'ssid': 'home', 'password': '123'}"
                    )
            }
    )
    @GetMapping
    public ResponseEntity<String> callApiByTarget(
            @RequestParam String target,
            @RequestParam String command,
            @RequestParam(required = false) String params
    ) throws BadRequestException {
        return deviceRequestService.execute(target, command, params);
    }

    @AcceptCloudRequest
    @GetMapping("/commands")
    public List<DeviceApiMethod> getApiMethods(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ip
    ) throws BadRequestException {
        if (StringUtils.isBlank(name) && StringUtils.isBlank(ip)) {
            throw new BadRequestException("Name and ip can't be blank!");
        }
        return deviceRequestService.getApiMethods(DeviceInfo.builder().name(name).ip(ip).build());
    }

}
