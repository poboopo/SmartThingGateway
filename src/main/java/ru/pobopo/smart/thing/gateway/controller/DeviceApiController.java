package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.model.DeviceRequest;
import ru.pobopo.smart.thing.gateway.model.DeviceResponse;
import ru.pobopo.smart.thing.gateway.service.DeviceApiService;

@RestController
@RequestMapping("/device/api")
@RequiredArgsConstructor
public class DeviceApiController {
    private final DeviceApiService deviceApiService;

    @PostMapping
    public ResponseEntity<String> callApi(@RequestBody DeviceRequest request) {
        DeviceResponse result = (DeviceResponse) deviceApiService.execute(request);
        return new ResponseEntity<>(
                result.getBody(),
                result.getCode()
        );
    }
}
