package ru.pobopo.smartthing.gateway.service.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceService {
    private final DevicesSearchService searchJob;
    private final SavedDevicesService savedDevicesService;

    public Optional<DeviceInfo> findDevice(String name, String ip) {
        Optional<DeviceInfo> deviceInfo = findDevice(ip);
        if (deviceInfo.isEmpty()) {
            deviceInfo = findDevice(name);
        }
        return deviceInfo;
    }

    public Optional<DeviceInfo> findDevice(String device) {
        return Stream.concat(
                        searchJob.getRecentFoundDevices().stream(),
                        savedDevicesService.getDevices().stream()
                ).filter((d) -> StringUtils.equals(d.getIp(), device) || StringUtils.equals(d.getName(), device))
                .findFirst();
    }
}
