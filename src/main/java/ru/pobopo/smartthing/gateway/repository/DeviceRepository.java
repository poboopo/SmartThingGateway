package ru.pobopo.smartthing.gateway.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.device.api.RestDeviceApi;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRepository {
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    private final RestTemplate restTemplate;
    private final FileRepository<DeviceInfo> fileRepository;

    public Collection<DeviceInfo> getDevices() {
        return fileRepository.getAll();
    }

    public DeviceInfo addDevice(String ip) throws BadRequestException {
        log.info("Trying to add new device by ip={}", ip);
        if (!isValidIp(ip)) {
            throw new BadRequestException("Not valid ip");
        }
        Optional<DeviceInfo> existing = getDevice(ip);
        if (existing.isPresent()) {
            log.info("Device with ip {} already exists", ip);
            return existing.get();
        }
        DeviceInfo newDeviceInfo = loadDeviceInfo(ip);
        if (newDeviceInfo == null) {
            throw new BadRequestException("Can't find device with ip=" + ip);
        }
        fileRepository.add(newDeviceInfo);
        fileRepository.commit();
        log.info("Added new device {}", newDeviceInfo);
        return newDeviceInfo;
    }

    public void deleteDevice(String ip) throws BadRequestException {
        log.info("Trying to delete device with ip={}", ip);
        if (!isValidIp(ip)) {
            throw new BadRequestException("Not valid ip");
        }
        Optional<DeviceInfo> device = getDevice(ip);
        if (device.isEmpty()) {
            throw new BadRequestException("There is no saved device with ip=" + ip);
        }
        fileRepository.remove(device.get());
        fileRepository.commit();
        log.info("Device {} deleted", device.get());
    }

    public DeviceInfo updateDeviceInfo(String ip) throws BadRequestException {
        log.info("Trying to update device with ip={}", ip);
        if (!isValidIp(ip)) {
            throw new BadRequestException("Not valid ip");
        }
        Optional<DeviceInfo> device = getDevice(ip);
        if (device.isEmpty()) {
            throw new BadRequestException("There is no saved device with ip=" + ip);
        }
        DeviceInfo newInfo = loadDeviceInfo(ip);
        if (newInfo == null) {
            throw new BadRequestException("Can't find active device with ip=" + ip);
        }
        if (newInfo.equals(device.get())) {
            log.info("New device info are equals to old one");
            return newInfo;
        }
        fileRepository.remove(device.get());
        fileRepository.add(newInfo);
        fileRepository.commit();
        log.info("Updated device info {}", newInfo);
        return newInfo;
    }

    public Optional<DeviceInfo> getDevice(String ip) {
        if (StringUtils.isBlank(ip)) {
            return Optional.empty();
        }
        return fileRepository.find(deviceInfo -> StringUtils.equals(deviceInfo.getIp(), ip));
    }

    private DeviceInfo loadDeviceInfo(String ip) {
        try {
            DeviceInfo info = restTemplate.getForObject(
                    String.format("http://%s/%s", ip, RestDeviceApi.SYSTEM_INFO),
                    DeviceInfo.class
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
