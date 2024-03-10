package ru.pobopo.smart.thing.gateway.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.exception.StorageException;
import ru.pobopo.smart.thing.gateway.model.CloudConfig;
import ru.pobopo.smart.thing.gateway.model.CloudIdentity;

@Component
@Slf4j
public class StorageService {
    private static final String CLOUD_CONFIG_FILE = "cloud_config.json";
    private static final String CLOUD_IDENTITY_FILE = "cloud_identity.json";

    private static final Path CONFIG_FILE_DEFAULT_PATH =
        Paths.get(System.getProperty("user.home"), ".smartthing");

    private final String baseDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public StorageService(Environment environment, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        String configFile = environment.getProperty("storage.dir.path");
        if (StringUtils.isNotBlank(configFile)) {
            baseDir = configFile;
        } else {
            baseDir = CONFIG_FILE_DEFAULT_PATH.toString();
        }
        log.info("Using storage directory: {}", baseDir);
    }

    public CloudConfig loadCloudConfig() throws StorageException {
        return readFromFile(CLOUD_CONFIG_FILE, CloudConfig.class);
    }

    public void saveCloudConfig(CloudConfig info) throws StorageException {
        writeToFile(CLOUD_CONFIG_FILE, info);
    }

    public CloudIdentity loadCloudIdentity() throws StorageException {
        return readFromFile(CLOUD_IDENTITY_FILE, CloudIdentity.class);
    }

    public void saveCloudIdentity(CloudIdentity cloudIdentity) throws StorageException {
        writeToFile(CLOUD_IDENTITY_FILE, cloudIdentity);
    }

    private void writeToFile(String fileName, Object value) throws StorageException {
        try {
            Path path = Paths.get(baseDir, fileName);
            String data = value == null ? "" : objectMapper.writeValueAsString(value);
            log.info("Writing {} to {}", data, fileName);
            Files.writeString(path, data);
        } catch (IOException e) {
            throw new StorageException("Failed to write file " + fileName, e);
        }
    }

    @Nullable
    private <T> T readFromFile(String fileName, Class<T> tClass) throws StorageException {
        try {
            File file = new File(Paths.get(baseDir, fileName).toString());
            if (!file.exists() || file.isDirectory()) {
                log.warn("There is no file {} in {}", fileName, baseDir);
                return null;
            }
            log.info("Reading {} from {}", tClass.getName(), fileName);
            return objectMapper.readValue(file, tClass);
        } catch (IOException e) {
            throw new StorageException("Failed to read file " + fileName, e);
        }
    }
}
