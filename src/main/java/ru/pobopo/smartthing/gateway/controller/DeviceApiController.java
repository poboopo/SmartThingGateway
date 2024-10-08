package ru.pobopo.smartthing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.gateway.annotation.AcceptCloudRequest;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.model.device.DeviceApiMethod;
import ru.pobopo.smartthing.gateway.service.device.DeviceApiService;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/device/api")
@RequiredArgsConstructor
@Tag(name = "Devices controller", description = "Call device api")
public class DeviceApiController {
    private final DeviceApiService deviceApiService;

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
        InternalHttpResponse result = deviceApiService.execute(request);
        return result.toResponseEntity();
    }
    @GetMapping("/{target}")
    public ResponseEntity<String> callApiByTarget(
            @PathVariable String target,
            @RequestParam String command,
            @RequestParam(required = false) String params
    ) throws BadRequestException {
        InternalHttpResponse result = deviceApiService.execute(target, command, params);
        return result.toResponseEntity();
    }

    @AcceptCloudRequest
    @GetMapping("/methods")
    public List<DeviceApiMethod> getApiMethods(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ip
    ) throws BadRequestException {
        if (StringUtils.isBlank(name) && StringUtils.isBlank(ip)) {
            throw new BadRequestException("Name and ip can't be blank!");
        }
        return deviceApiService.getApiMethods(DeviceInfo.builder().name(name).ip(ip).build());
    }

}
