package ru.pobopo.smartthing.gateway.service.ota;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareInfo;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareUploadProgress;
import ru.pobopo.smartthing.gateway.repository.FileRepository;
import ru.pobopo.smartthing.gateway.service.device.DeviceService;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtaFirmwareService {
    private final static Map<String, Integer> BOARD_INVITATION_PORT = Map.of(
            "esp32", 3232,
            "esp8266", 8266
    );

    private final OtaFirmwareStorageService storageService;
    private final FileRepository<OtaFirmwareInfo> repository;
    private final DeviceService searchService;
    private final SimpMessagingTemplate messagingTemplate;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<DeviceInfo, Pair<Future<?>, OtaFirmwareUploadTask>> uploadTasks = new ConcurrentHashMap<>();

    public Collection<OtaFirmwareInfo> getAllInfos() {
        return repository.getAll();
    }

    public OtaFirmwareInfo addFirmware(OtaFirmwareInfo info, MultipartFile file) throws IOException {
        if (
                StringUtils.isBlank(info.getType()) ||
                        StringUtils.isBlank(info.getBoard()) ||
                        StringUtils.isBlank(info.getVersion())
        ) {
            throw new ValidationException("Type, board and version params are required!");
        }
        if (findDuplicates(info).isPresent()) {
            throw new ValidationException("Duplicate firmware detected");
        }

        byte[] firmwareData = file.getBytes();
        String filePath = storageService.createFirmwareFile(info, firmwareData);

        try {
            OtaFirmwareInfo newFirmwareInfo = OtaFirmwareInfo.builder()
                    .id(UUID.randomUUID())
                    .board(info.getBoard())
                    .type(info.getType())
                    .version(info.getVersion())
                    .fileChecksum(generateChecksum(firmwareData))
                    .fileName(filePath)
                    .build();

            repository.add(newFirmwareInfo);

            return newFirmwareInfo;
        } catch (Exception e) {
            storageService.deleteFirmwareFile(OtaFirmwareInfo.builder().fileName(filePath).build());
            throw e;
        }
    }

    public OtaFirmwareInfo updateFirmwareInfo(OtaFirmwareInfo otaFirmwareInfo) {
        if (otaFirmwareInfo.getId() == null) {
            throw new ValidationException("Id is missing");
        }
        if (findDuplicates(otaFirmwareInfo).isPresent()) {
            throw new ValidationException("Duplicate firmware info detected");
        }
        Optional<OtaFirmwareInfo> info = repository.findById(otaFirmwareInfo.getId());
        if (info.isEmpty()) {
            throw new ValidationException("Can't find firmware info with given id");
        }

        OtaFirmwareInfo oldInfo = info.get();
        OtaFirmwareInfo.OtaFirmwareInfoBuilder builder = oldInfo.toBuilder();
        if (StringUtils.isNotBlank(otaFirmwareInfo.getType())) {
            builder.type(otaFirmwareInfo.getType());
        }
        if (StringUtils.isNotBlank(otaFirmwareInfo.getBoard())) {
            builder.board(otaFirmwareInfo.getBoard());
        }
        if (StringUtils.isNotBlank(otaFirmwareInfo.getVersion())) {
            builder.version(otaFirmwareInfo.getVersion());
        }
        OtaFirmwareInfo newInfo = builder.build();
        repository.update(newInfo);
        log.info("Firmware info were updated: {}", newInfo);
        return newInfo;
    }

    public void deleteFirmware(UUID id) throws IOException {
        if (id == null) {
            throw new ValidationException("Id is missing");
        }
        Optional<OtaFirmwareInfo> info = repository.findById(id);
        if (info.isEmpty()) {
            throw new ValidationException("Can't find firmware info with given id");
        }
        repository.delete(info.get().getId());
        storageService.deleteFirmwareFile(info.get());
    }

    public Path getFirmwareFile(UUID id) {
        if (id == null) {
            return null;
        }
        Optional<OtaFirmwareInfo> info = repository.findById(id);
        return info.map(storageService::getFirmwareFile).orElse(null);
    }

    public Map<String, UUID> uploadFirmware(UUID id, List<DeviceInfo> targetDevices) throws IOException {
        if (id == null) {
            throw new ValidationException("Id is missing");
        }
        if (targetDevices == null) {
            throw new ValidationException("Device info missing!");
        }

        Optional<OtaFirmwareInfo> info = repository.findById(id);
        if (info.isEmpty()) {
            throw new ValidationException("Can't find firmware info with given id");
        }

        final OtaFirmwareInfo firmwareInfo = info.get();
        Path firmware = storageService.getFirmwareFile(firmwareInfo);
        if (firmware == null) {
            throw new ValidationException("Firmware file is missing!");
        }
        final byte[] data = Files.readAllBytes(firmware);
        if (!StringUtils.equals(firmwareInfo.getFileChecksum(), generateChecksum(data))) {
            throw new IllegalStateException("Firmware file corrupted!");
        }

        Map<String, UUID> result = new ConcurrentHashMap<>();
        targetDevices.parallelStream().forEach((device) -> {
            try {
                result.put(device.getIp(), uploadFirmware(firmwareInfo, data, device));
            } catch (Exception e) {
                log.error("Upload to {} failed", device, e);
                result.put(device.getIp(), null);
            }
        });

        return result;
    }

    public UUID uploadFirmware(UUID id, DeviceInfo deviceInfo) throws IOException {
        if (id == null) {
            throw new ValidationException("Id is missing");
        }

        Optional<OtaFirmwareInfo> info = repository.findById(id);
        if (info.isEmpty()) {
            throw new ValidationException("Can't find firmware info with given id");
        }

        Path firmware = storageService.getFirmwareFile(info.get());
        if (firmware == null) {
            throw new ValidationException("Firmware file is missing!");
        }
        return uploadFirmware(info.get(), Files.readAllBytes(firmware), deviceInfo);
    }

    private UUID uploadFirmware(OtaFirmwareInfo firmwareInfo, byte[] firmwareData, DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            throw new ValidationException("Device info missing!");
        }
        if (!StringUtils.equals(firmwareInfo.getFileChecksum(), generateChecksum(firmwareData))) {
            throw new IllegalStateException("Firmware file corrupted!");
        }

        Optional<DeviceInfo> foundDevice = searchService.findDevice(deviceInfo.getName(), deviceInfo.getIp());
        if (foundDevice.isEmpty()) {
            throw new ValidationException("Can't find device");
        }
        DeviceInfo targetDevice = foundDevice.get();
        if (uploadTasks.containsKey(targetDevice)) {
            Pair<Future<?>, OtaFirmwareUploadTask> oldTask = uploadTasks.get(targetDevice);
            if (oldTask.getLeft().isDone()) {
                uploadTasks.remove(targetDevice);
            } else {
                throw new ValidationException("There is already upload task running for this device");
            }
        }

        if (!StringUtils.equals(firmwareInfo.getBoard(), targetDevice.getBoard())) {
            throw new ValidationException(String.format(
                    "Firmware and target device board type are different! Expected %s, got %s",
                    firmwareInfo.getBoard(),
                    targetDevice.getBoard()
            ));
        }
        Integer invitationPort = BOARD_INVITATION_PORT.get(targetDevice.getBoard());
        if (invitationPort == null) {
            throw new ValidationException("Board " + targetDevice.getBoard() + " not support yet (can't select port)");
        }

        OtaFirmwareUploadTask uploadTask = new OtaFirmwareUploadTask(
                firmwareInfo,
                targetDevice,
                firmwareData,
                invitationPort,
                messagingTemplate
        );
        Future<?> future = executorService.submit(uploadTask);
        log.info("Upload task started (firmware={}, target={})", firmwareInfo, targetDevice);
        uploadTasks.put(targetDevice, Pair.of(future, uploadTask));
        return uploadTask.getId();
    }

    public void abortFirmwareUpload(UUID taskId) {
        Optional<Pair<Future<?>, OtaFirmwareUploadTask>> task = uploadTasks.values().stream()
                .filter(p -> p.getRight().getId().equals(taskId))
                .findFirst();

        if (task.isEmpty()) {
            throw new ValidationException("Upload task not found");
        }

        task.get().getLeft().cancel(true);
    }

    public List<OtaFirmwareUploadProgress> getRunningUploads() {
        List<OtaFirmwareUploadProgress> result = new ArrayList<>();
        for (Map.Entry<DeviceInfo, Pair<Future<?>, OtaFirmwareUploadTask>> entry : uploadTasks.entrySet()) {
            if (entry.getValue().getLeft().isDone()) {
                uploadTasks.remove(entry.getKey());
            } else {
                OtaFirmwareUploadTask task = entry.getValue().getRight();
                result.add(new OtaFirmwareUploadProgress(
                        task.getId(),
                        entry.getKey(),
                        task.getFirmwareInfo(),
                        task.getStatus(),
                        task.getTransferProgress().get()
                ));
            }
        }
        return result;
    }

    public Collection<String> supportedBoards() {
        return BOARD_INVITATION_PORT.keySet();
    }

    private Optional<OtaFirmwareInfo> findDuplicates(OtaFirmwareInfo info) {
        return repository.getAll().stream().filter(firm -> StringUtils.equals(firm.getType(), info.getType())
                && StringUtils.equals(firm.getVersion(), info.getVersion())
                && StringUtils.equals(firm.getBoard(), info.getBoard())
        ).findFirst();
    }

    @SneakyThrows
    private String generateChecksum(byte[] data) {
        byte[] hash = MessageDigest.getInstance("MD5").digest(data);
        return String.format("%32s", new BigInteger(1, hash).toString(16)).replace(' ', '0');
    }

    @Scheduled(fixedDelayString = "${ota.tasks.cleanup.delay:10000}")
    public void cleanupTasks() {
        for (Map.Entry<DeviceInfo, Pair<Future<?>, OtaFirmwareUploadTask>> entry : uploadTasks.entrySet()) {
            if (entry.getValue().getLeft().isDone()) {
                uploadTasks.remove(entry.getKey());
            }
        }
    }
}
