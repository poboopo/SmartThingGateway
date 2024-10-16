package ru.pobopo.smartthing.gateway.service.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.service.device.api.RestDeviceApi;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.repository.FileRepository;
import ru.pobopo.smartthing.model.SavedDeviceInfo;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedDevicesService {
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    private final RestTemplate restTemplate;
    private final FileRepository<SavedDeviceInfo> fileRepository;

    public Collection<SavedDeviceInfo> getDevices() {
        return fileRepository.getAll();
    }

    public SavedDeviceInfo addDevice(String ip) throws BadRequestException {
        log.info("Trying to add new device by ip={}", ip);
        if (!isValidIp(ip)) {
            throw new BadRequestException("Not valid ip");
        }
        Optional<SavedDeviceInfo> existing = getDevice(ip);
        if (existing.isPresent()) {
            log.warn("Device with ip {} already exists", ip);
            return existing.get();
        }
        SavedDeviceInfo newDeviceInfo = loadDeviceInfo(ip);
        if (newDeviceInfo == null) {
            throw new BadRequestException("Can't find device with ip=" + ip);
        }
        newDeviceInfo.setId(UUID.randomUUID());
        fileRepository.add(newDeviceInfo);
        log.info("Added new device {}", newDeviceInfo);
        return newDeviceInfo;
    }

    public void deleteDevice(String ip) throws BadRequestException {
        log.info("Trying to delete device with ip={}", ip);
        if (!isValidIp(ip)) {
            throw new BadRequestException("Not valid ip");
        }
        Optional<SavedDeviceInfo> device = getDevice(ip);
        if (device.isEmpty()) {
            throw new BadRequestException("There is no saved device with ip=" + ip);
        }
        fileRepository.delete(device.get().getId());
        log.info("Device {} deleted", device.get());
    }

    public SavedDeviceInfo updateDeviceInfo(String ip) throws BadRequestException {
        log.info("Trying to update device with ip={}", ip);
        if (!isValidIp(ip)) {
            throw new BadRequestException("Not valid ip");
        }
        Optional<SavedDeviceInfo> device = getDevice(ip);
        if (device.isEmpty()) {
            throw new BadRequestException("There is no saved device with ip=" + ip);
        }
        SavedDeviceInfo newInfo = loadDeviceInfo(ip);
        if (newInfo == null) {
            throw new BadRequestException("Can't find active device with ip=" + ip);
        }
        if (newInfo.equals(device.get())) {
            log.info("New device info are equals to old one");
            return newInfo;
        }
        fileRepository.update(device.get());
        log.info("Updated device info {}", newInfo);
        return newInfo;
    }

    public Optional<SavedDeviceInfo> getDevice(String ip) {
        if (StringUtils.isBlank(ip)) {
            return Optional.empty();
        }
        return fileRepository.getAll().stream().filter(deviceInfo -> StringUtils.equals(deviceInfo.getIp(), ip)).findFirst();
    }

    private SavedDeviceInfo loadDeviceInfo(String ip) {
        try {
            SavedDeviceInfo info = restTemplate.getForObject(
                    String.format("http://%s/%s", ip, RestDeviceApi.SYSTEM_INFO),
                    SavedDeviceInfo.class
            );
            if (info == null) {
                return null;
            }
            info.setIp(ip);
            return info;
        } catch (RestClientException exception) {
            log.error("Failed to get device info: {}", exception.getMessage(), exception);
            return null;
        }
    }

    private boolean isValidIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        Matcher matcher = IP_PATTERN.matcher(ip);
        return matcher.find();
    }
}
