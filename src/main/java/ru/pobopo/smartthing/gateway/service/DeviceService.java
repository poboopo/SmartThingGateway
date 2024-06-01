package ru.pobopo.smartthing.gateway.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.jobs.DevicesSearchService;
import ru.pobopo.smartthing.gateway.repository.DeviceRepository;
import ru.pobopo.smartthing.model.DeviceInfo;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceService {
    private final DevicesSearchService searchJob;
    private final DeviceRepository deviceRepository;

    public Optional<DeviceInfo> findDevice(String name, String ip) {
        Collection<DeviceInfo> devices = new ArrayList<>();
        devices.addAll(searchJob.getRecentFoundDevices());
        devices.addAll(deviceRepository.getDevices());
        return devices.stream()
                .filter((d) -> StringUtils.equals(d.getIp(), ip) || StringUtils.equals(d.getName(), name))
                .findFirst();
    }
}
