package ru.pobopo.smartthing.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareInfo;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareUploadProgress;
import ru.pobopo.smartthing.gateway.service.ota.OtaFirmwareService;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ota/firmware")
@RequiredArgsConstructor
public class OtaUpdatesController {
    private final OtaFirmwareService otaFirmwareService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Collection<OtaFirmwareInfo> getAll() {
        return otaFirmwareService.getAllInfos();
    }

    @PostMapping
    public OtaFirmwareInfo uploadFirmware(
            @RequestParam("info") String info,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return otaFirmwareService.addFirmware(objectMapper.readValue(info, OtaFirmwareInfo.class), file);
    }

    @PutMapping
    public OtaFirmwareInfo updateFirmwareInfo(
            @RequestBody OtaFirmwareInfo otaFirmwareInfo
    ) {
        return otaFirmwareService.updateFirmwareInfo(otaFirmwareInfo);
    }

    @DeleteMapping
    public void deleteFirmware(
            @RequestParam("id") UUID id
    ) throws IOException {
        otaFirmwareService.deleteFirmware(id);
    }

    @GetMapping("/upload")
    public List<OtaFirmwareUploadProgress> getRunningUploads() {
        return otaFirmwareService.getRunningUploads();
    }

    @PostMapping("/upload")
    public UUID uploadFirmware(
            @RequestParam("id") UUID id,
            @RequestBody DeviceInfo deviceInfo
    ) throws IOException {
        return otaFirmwareService.uploadFirmware(id, deviceInfo);
    }

    @PostMapping("/upload/batch")
    public Map<String, UUID> uploadFirmwareBatch(
            @RequestParam("id") UUID id,
            @RequestBody List<DeviceInfo> targetDevices
    ) throws IOException {
        return otaFirmwareService.uploadFirmware(id, targetDevices);
    }

    @DeleteMapping("/upload")
    public void abortUpload(
            @RequestParam("id") UUID id
    ) {
        otaFirmwareService.abortFirmwareUpload(id);
    }

    @GetMapping("/boards")
    public Collection<String> getSupportedBoards() {
        return otaFirmwareService.supportedBoards();
    }

    @GetMapping(
            value = "/download"
    )
    public ResponseEntity<Resource> downloadFirmware(
            @RequestParam("id") UUID id
    ) throws IOException {
        Path file = otaFirmwareService.getFirmwareFile(id);
        if (file == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("content-type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        headers.add("content-disposition", String.format("attachment; filename=\"%s\"", file.getFileName()));

        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(file));

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
