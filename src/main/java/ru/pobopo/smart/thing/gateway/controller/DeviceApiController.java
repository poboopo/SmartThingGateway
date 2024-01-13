package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smart.thing.gateway.model.DeviceRequest;
import ru.pobopo.smart.thing.gateway.service.DeviceApiService;

@RestController
@RequestMapping("/device/api")
@RequiredArgsConstructor
public class DeviceApiController {
    private final DeviceApiService deviceApiService;

    @PostMapping
    public Object callApi(@RequestBody DeviceRequest request) {
        return deviceApiService.execute(request);
    }
}
