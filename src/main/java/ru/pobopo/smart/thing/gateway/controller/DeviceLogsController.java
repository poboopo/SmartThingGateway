package ru.pobopo.smart.thing.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smart.thing.gateway.model.DeviceLoggerMessage;
import ru.pobopo.smart.thing.gateway.service.DeviceLogsProcessor;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/device/logs")
@RequiredArgsConstructor
public class DeviceLogsController {
    private final DeviceLogsProcessor deviceLogsProcessor;

    @GetMapping
    public List<DeviceLoggerMessage> getLogs() {
        return deviceLogsProcessor.getLogs();
    }
}
