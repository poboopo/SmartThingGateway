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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.event.CloudInfoLoadedEvent;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.CloudInfo;

@Component
@Slf4j
public class ConfigurationService {
    public static final String TOKEN_PROPERTY = "token";
    public static final String CLOUD_URL_PROPERTY = "cloud.url";
    public static final String BROKER_IP_PROPERTY = "broker.ip";

    private static final Path CONFIG_FILE_DEFAULT_PATH =
        Paths.get(System.getProperty("user.home"), ".smartthing/gateway.config");

    private final Properties properties = new Properties();
    private final String configFilePath;
    private final ApplicationEventPublisher applicationEventPublisher;
    private CloudInfo cloudInfo = new CloudInfo();

    @Autowired
    public ConfigurationService(Environment environment, ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;

        String configFile = environment.getProperty("CONFIG_FILE");
        if (StringUtils.isNotBlank(configFile)) {
            configFilePath = configFile;
        } else {
            configFilePath = CONFIG_FILE_DEFAULT_PATH.toString();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadAfterStartUp() throws ConfigurationException {
        loadPropertiesFromFile();
    }

    public CloudInfo getCloudInfo() {
        return cloudInfo;
    }

    public void updateCloudInfo(@NonNull CloudInfo info) throws ConfigurationException {
        Objects.requireNonNull(info);
        log.info("Cloud info was updated: {}", info);
        if (StringUtils.equals(info.getToken(), "empty") || StringUtils.equals(info.getToken(), "present")) {
            info.setToken(cloudInfo.getToken());
        }
        this.cloudInfo = info;
        savePropertiesToFile();
        sendCloudInfoEvent();
    }

    private void loadCloudInfo() {
        this.cloudInfo = new CloudInfo(
            properties.getProperty(TOKEN_PROPERTY),
            properties.getProperty(CLOUD_URL_PROPERTY),
            properties.getProperty(BROKER_IP_PROPERTY)
        );
        log.info("Loaded cloud info: {}", this.cloudInfo);
        sendCloudInfoEvent();
    }

    private void writeCloudInfoToProperties() {
        properties.setProperty(TOKEN_PROPERTY, cloudInfo.getToken());
        properties.setProperty(CLOUD_URL_PROPERTY, cloudInfo.getCloudUrl());
        properties.setProperty(BROKER_IP_PROPERTY, cloudInfo.getBrokerIp());
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

            loadCloudInfo();
        } catch (IOException exception) {
            log.error("Failed to load configuration from properties file", exception);
            throw new ConfigurationException(exception.getMessage());
        }
    }

    private void sendCloudInfoEvent() {
        try {
            applicationEventPublisher.publishEvent(new CloudInfoLoadedEvent(this, this.cloudInfo));
        } catch (Exception exception) {
            log.error("Event publish failed: {}", exception.getMessage(), exception);
        }
    }
}
