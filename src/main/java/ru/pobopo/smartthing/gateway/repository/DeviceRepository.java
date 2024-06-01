package ru.pobopo.smartthing.gateway.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.device.api.DefaultDeviceApi;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.exception.DashboardFileException;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Slf4j
@Service
public class DeviceRepository {
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Path repoFile;

    private final Set<DeviceInfo> devices = ConcurrentHashMap.newKeySet();

    @Autowired
    public DeviceRepository(@Value("${device.saved.file}") String repoFilePath,
                            RestTemplate restTemplate, ObjectMapper objectMapper) throws IOException {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        if (StringUtils.isBlank(repoFilePath)) {
            repoFile = Paths.get(DEFAULT_APP_DIR.toString(), "/saved_devices.json");
        } else {
            repoFile = Paths.get(repoFilePath);
        }
        log.info("Using saved device file: {}", repoFile);

        if (!Files.exists(repoFile)) {
            log.info("No saved devices file found, creating...");
            Files.createDirectories(repoFile.getParent());
            Files.writeString(repoFile, "[]");
        } else {
            try {
                this.devices.addAll(objectMapper.readValue(repoFile.toFile(), new TypeReference<>() {}));
                log.info("Loaded devices: {}", devices);
            } catch (MismatchedInputException exception) {
                throw new RuntimeException("Failed to load saved devices", exception);
            }
        }
    }

    public Collection<DeviceInfo> getDevices() {
        return Collections.unmodifiableCollection(devices);
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
        devices.add(newDeviceInfo);
        log.info("Added new device {}", newDeviceInfo);
        saveToFile();
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
        devices.remove(device.get());
        log.info("Device {} deleted", device.get());
        saveToFile();
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
        devices.remove(device.get());
        devices.add(newInfo);
        log.info("Updated device info {}", newInfo);
        saveToFile();
        return newInfo;
    }

    public Optional<DeviceInfo> getDevice(String ip) {
        if (StringUtils.isBlank(ip)) {
            return Optional.empty();
        }
        return devices.stream().filter((deviceInfo -> StringUtils.equals(deviceInfo.getIp(), ip))).findFirst();
    }

    // 2 requests when device name updated
    // todo load from DeviceApi?
    private DeviceInfo loadDeviceInfo(String ip) {
        log.info("Trying to load device info (ip={})", ip);
        try {
            DeviceInfo info = restTemplate.getForObject(
                    String.format("http://%s/%s", ip, DefaultDeviceApi.SYSTEM_INFO),
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

    @SneakyThrows
    private void saveToFile() {
        log.info("Writing saved devices to file");
        objectMapper.writeValue(repoFile.toFile(), devices);
    }

    private boolean isValidIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        Matcher matcher = IP_PATTERN.matcher(ip);
        return matcher.find();
    }
}
