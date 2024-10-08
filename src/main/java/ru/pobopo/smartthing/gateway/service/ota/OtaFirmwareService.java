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

import java.io.*;
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
            repository.commit();

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
        Optional<OtaFirmwareInfo> info = repository.find(i -> i.getId().equals(otaFirmwareInfo.getId()));
        if (info.isEmpty()) {
            throw new ValidationException("Can't find firmware info with given id");
        }

        OtaFirmwareInfo oldInfo = info.get();
        try {
            repository.delete(oldInfo);
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
            repository.add(newInfo);
            repository.commit();
            log.info("Firmware info were updated: {}", newInfo);
            return newInfo;
        } catch (Exception e) {
            repository.rollback();
            throw e;
        }
    }

    public void deleteFirmware(UUID id) throws IOException {
        if (id == null) {
            throw new ValidationException("Id is missing");
        }
        Optional<OtaFirmwareInfo> info = repository.find(i -> i.getId().equals(id));
        if (info.isEmpty()) {
            throw new ValidationException("Can't find firmware info with given id");
        }
         try {
             repository.delete(info.get());
             storageService.deleteFirmwareFile(info.get());
             repository.commit();
         } catch (Exception e) {
             repository.rollback();
             throw e;
         }
    }

    public Path getFirmwareFile(UUID id) {
        if (id == null) {
            return null;
        }
        Optional<OtaFirmwareInfo> info = repository.find(i -> i.getId().equals(id));
        return info.map(storageService::getFirmwareFile).orElse(null);
    }

    public UUID uploadFirmware(UUID id, DeviceInfo deviceInfo) throws IOException {
        if (deviceInfo == null) {
            throw new ValidationException("Device info missing!");
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
        Integer invitationPort = BOARD_INVITATION_PORT.get(targetDevice.getBoard());
        if (invitationPort == null) {
            throw new ValidationException("Board " + targetDevice.getBoard() + " not support yet (can't select port)");
        }

        if (id == null) {
            throw new ValidationException("Id is missing");
        }
        Optional<OtaFirmwareInfo> info = repository.find(i -> i.getId().equals(id));
        if (info.isEmpty()) {
            throw new ValidationException("Can't find firmware info with given id");
        }

        OtaFirmwareInfo firmwareInfo = info.get();
        if (!StringUtils.equals(firmwareInfo.getBoard(), targetDevice.getBoard())) {
            throw new ValidationException(String.format(
                    "Firmware and target device board type are different! Expected %s, got %s",
                    firmwareInfo.getBoard(),
                    targetDevice.getBoard()
            ));
        }
        Path firmware = storageService.getFirmwareFile(firmwareInfo);
        if (firmware == null) {
            throw new ValidationException("Firmware file is missing!");
        }
        byte[] data = Files.readAllBytes(firmware);
        if (!StringUtils.equals(firmwareInfo.getFileChecksum(), generateChecksum(data))) {
            throw new IllegalStateException("Firmware file corrupted!");
        }

        OtaFirmwareUploadTask uploadTask = new OtaFirmwareUploadTask(firmwareInfo, targetDevice, data, invitationPort, messagingTemplate);
        Future<?> future = executorService.submit(uploadTask);
        log.info("Upload task started (firmware={}, target={})", firmwareInfo, targetDevice);
        uploadTasks.put(targetDevice, Pair.of(future, uploadTask));
        return uploadTask.getId();
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
