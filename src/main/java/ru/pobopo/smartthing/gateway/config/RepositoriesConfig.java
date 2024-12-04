package ru.pobopo.smartthing.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smartthing.gateway.model.device.DeviceSettingsDump;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareInfo;
import ru.pobopo.smartthing.gateway.repository.FileRepository;
import ru.pobopo.smartthing.model.SavedDeviceInfo;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardGroup;
import ru.pobopo.smartthing.model.stomp.DeviceNotification;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Configuration
public class RepositoriesConfig {
    private static final Path SETTINGS_DUMPS_DEFAULT_PATH = Paths.get(DEFAULT_APP_DIR.toString(), "device_settings");
    private static final Path SAVED_DEVICES_DEFAULT = Paths.get(DEFAULT_APP_DIR.toString(), "saved_devices");
    private static final Path DASHBOARD_DEFAULT_PATH = Paths.get(DEFAULT_APP_DIR.toString(), "dashboard_config");
    private static final Path FIRMWARE_DEFAULT_PATH = Paths.get(DEFAULT_APP_DIR.toString(), "firmware_info");
    private static final Path NOTIFICATIONS_DEFAULT_PATH = Paths.get(DEFAULT_APP_DIR.toString(), "notifications");

    @Value("${device.settings.dir:}")
    private String deviceSettingsPath;
    @Value("${device.saved.dir:}")
    private String savedDevicesPath;
    @Value("${dashboard.settings.dir:}")
    private String dashboardConfigPath;
    @Value("${ota.firmware.info.dir:}")
    private String firmwareInfoPath;
    @Value("${notifications.dir:}")
    private String notificationsPath;

    // todo factory?
    @Bean
    public FileRepository<DeviceSettingsDump> deviceSettingsFileRepository(ObjectMapper objectMapper) throws IOException {
        return new FileRepository<>(
                DeviceSettingsDump.class,
                StringUtils.isEmpty(deviceSettingsPath) ? SETTINGS_DUMPS_DEFAULT_PATH : Paths.get(deviceSettingsPath),
                objectMapper
        );
    }

    @Bean
    public FileRepository<SavedDeviceInfo> savedDevicesRepository(ObjectMapper objectMapper) throws IOException {
        return new FileRepository<>(
                SavedDeviceInfo.class,
                StringUtils.isEmpty(savedDevicesPath) ? SAVED_DEVICES_DEFAULT : Paths.get(savedDevicesPath),
                objectMapper
        );
    }

    @Bean
    public FileRepository<DashboardGroup> dashboardRepository(ObjectMapper objectMapper) throws IOException {
        return new FileRepository<>(
                DashboardGroup.class,
                StringUtils.isEmpty(dashboardConfigPath) ? DASHBOARD_DEFAULT_PATH : Paths.get(dashboardConfigPath),
                objectMapper
        );
    }

    @Bean
    public FileRepository<OtaFirmwareInfo> firmwareRepository(ObjectMapper objectMapper) throws IOException {
        return new FileRepository<>(
                OtaFirmwareInfo.class,
                StringUtils.isEmpty(firmwareInfoPath) ? FIRMWARE_DEFAULT_PATH : Paths.get(firmwareInfoPath),
                objectMapper
        );
    }

    @Bean
    public FileRepository<DeviceNotification> notificationRepository(ObjectMapper objectMapper) throws IOException {
        return new FileRepository<>(
                DeviceNotification.class,
                StringUtils.isEmpty(notificationsPath) ? NOTIFICATIONS_DEFAULT_PATH : Paths.get(notificationsPath),
                objectMapper
        );
    }
}
