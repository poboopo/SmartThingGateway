package ru.pobopo.smart.thing.gateway.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.event.CloudInfoUpdated;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;

@Component
@Slf4j
public class ConfigurationService {
    public static final String TOKEN_PROPERTY = "token";
    public static final String CCLOUD_IP_PROPERTY = "cloud.ip";
    public static final String CLOUD_PORT_PROPERTY = "cloud.port";

    private static final Path CONFIG_FILE_DEFAULT_PATH =
        Paths.get(System.getProperty("user.home"), ".smartthing/gateway.config");

    private final Properties properties = new Properties();
    private final String configFilePath;
    private final ApplicationEventPublisher applicationEventPublisher;
    private CloudAuthInfo cloudInfo;

    @Autowired
    public ConfigurationService(Environment environment, ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;

        String configFile = environment.getProperty("config.file.path");
        if (StringUtils.isNotBlank(configFile)) {
            configFilePath = configFile;
        } else {
            configFilePath = CONFIG_FILE_DEFAULT_PATH.toString();
        }
    }

    public CloudAuthInfo getCloudAuthInfo() {
        return cloudInfo;
    }

    public void updateCloudAuthInfo(CloudAuthInfo info) throws ConfigurationException {
        this.cloudInfo = info;
        log.info("Cloud info was updated: {}", info);

        savePropertiesToFile();
        sendCloudInfoEvent();
    }

    public void loadConfiguration() throws ConfigurationException {
        loadPropertiesFromFile();

        String token = properties.getProperty(TOKEN_PROPERTY);
        if (StringUtils.isBlank(token)) {
            log.info("Token property empty! No cloud info loaded");
            return;
        }

        this.cloudInfo = new CloudAuthInfo(
            token,
            properties.getProperty(CCLOUD_IP_PROPERTY),
            Integer.parseInt(properties.getProperty(CLOUD_PORT_PROPERTY, "8080"))
        );

        log.info("Loaded cloud info: {}", this.cloudInfo);
        sendCloudInfoEvent();
    }

    private void writeCloudInfoToProperties() {
        if (cloudInfo == null) {
            properties.setProperty(TOKEN_PROPERTY, "");
            properties.setProperty(CCLOUD_IP_PROPERTY, "");
            properties.setProperty(CLOUD_PORT_PROPERTY, "");
        } else {
            properties.setProperty(TOKEN_PROPERTY, cloudInfo.getToken() == null ? "" : cloudInfo.getToken());
            properties.setProperty(CCLOUD_IP_PROPERTY, cloudInfo.getCloudIp() == null ? "" : cloudInfo.getCloudIp());
            properties.setProperty(CLOUD_PORT_PROPERTY, String.valueOf(cloudInfo.getCloudPort()));
        }
    }

    private void savePropertiesToFile() throws ConfigurationException {
        try {
            writeCloudInfoToProperties();

            properties.store(new FileOutputStream(configFilePath), null);
            log.info("Properties file was updated");
        } catch (IOException exception) {
            log.error("Failed to save properties file", exception);
            throw new ConfigurationException(exception.getMessage());
        }
    }

    private void loadPropertiesFromFile() throws ConfigurationException {
        File configFile = new File(configFilePath);
        try {
            if (configFile.getParentFile().mkdirs()) {
                log.info("Created directories: {}", configFile.getParentFile().getAbsolutePath());
            }

            if (configFile.createNewFile()) {
                log.info("Config file were created: {}", configFilePath);
            } else {
                log.info("Loading config from {}", configFilePath);
            }
            properties.load(new FileInputStream(configFile));
        } catch (IOException exception) {
            log.error("Failed to load configuration from properties file", exception);
            throw new ConfigurationException(exception.getMessage());
        }
    }

    private void sendCloudInfoEvent() {
        try {
            applicationEventPublisher.publishEvent(new CloudInfoUpdated(this));
        } catch (Exception exception) {
            log.error("Event publish failed: {}", exception.getMessage(), exception);
        }
    }
}
