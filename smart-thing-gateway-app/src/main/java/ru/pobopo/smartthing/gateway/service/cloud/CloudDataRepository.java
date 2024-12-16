package ru.pobopo.smartthing.gateway.service.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.exception.StorageException;
import ru.pobopo.smartthing.gateway.model.cloud.CloudConfig;
import ru.pobopo.smartthing.gateway.model.cloud.CloudIdentity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Component
@Slf4j
public class CloudDataRepository {
    private static final String CLOUD_CONFIG_FILE = "cloud_config.json";
    private static final String CLOUD_IDENTITY_FILE = "cloud_identity.json";

    private final String baseDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public CloudDataRepository(Environment environment, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        String configFile = environment.getProperty("cloud.config.dir");
        if (StringUtils.isNotBlank(configFile)) {
            baseDir = configFile;
        } else {
            baseDir = DEFAULT_APP_DIR.toString();
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
