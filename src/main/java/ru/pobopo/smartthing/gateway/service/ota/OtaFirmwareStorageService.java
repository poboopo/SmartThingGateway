package ru.pobopo.smartthing.gateway.service.ota;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Service
public class OtaFirmwareStorageService {
    private static final Path FIRMWARE_STORAGE_DIR = Paths.get(DEFAULT_APP_DIR.toString(), "firmware");

    private final String rootDir;

    public OtaFirmwareStorageService(
            @Value("${ota.firmware.storage.dir:}") String firmwareDir
    ) throws IOException {
        rootDir = StringUtils.isBlank(firmwareDir) ? FIRMWARE_STORAGE_DIR.toString() : firmwareDir;
        Path rootPath = Path.of(rootDir);
        if (Files.exists(rootPath)) {
            if (!Files.isDirectory(rootPath)) {
                throw new IllegalArgumentException("Wrong path to ota firmwares directory: " + rootPath);
            }
        } else {
            Files.createDirectories(rootPath);
        }
    }

    public String createFirmwareFile(OtaFirmwareInfo info, byte[] data) throws IOException {
        String fileName = String.format(
                "%s-%s-%s-%s.bin",
                info.getBoard(),
                info.getType(),
                info.getVersion(),
                UUID.randomUUID()
        );
        Path filePath = Files.createFile(Path.of(rootDir, fileName));
        Files.write(filePath, data);
        return fileName;
    }

    public void deleteFirmwareFile(OtaFirmwareInfo info) throws IOException {
        if (StringUtils.isEmpty(info.getFileName())) {
            return;
        }
        Path filePath = Path.of(rootDir, info.getFileName());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    public Path getFirmwareFile(OtaFirmwareInfo info) {
        Path filePath = Path.of(rootDir, info.getFileName());
        if (Files.exists(filePath)) {
            return filePath;
        }
        return null;
    }
}
