package ru.pobopo.smartthing.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smartthing.gateway.model.DeviceSettings;
import ru.pobopo.smartthing.gateway.repository.FileRepository;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardGroup;

import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Configuration
public class RepositoriesConfig {
    private static final Path SETTINGS_FILE_DEFAULT = Paths.get(DEFAULT_APP_DIR.toString(), "device_settings.json");
    private static final Path SAVED_DEVICES_DEFAULT = Paths.get(DEFAULT_APP_DIR.toString(), "saved_devices.json");
    private static final Path DASHBOARD_DEFAULT_PATH = Paths.get(DEFAULT_APP_DIR.toString(), "dashboard_config.json");

    @Value("${device.settings.dir:}")
    private String deviceSettingsPath;
    @Value("${device.saved.file}")
    private String savedDevicesPath;
    @Value("${dashboard.settings.file:}")
    private String dashboardConfigPath;

    @Bean
    public FileRepository<DeviceSettings> deviceSettingsFileRepository(ObjectMapper objectMapper) {
        return new FileRepository<>(
                DeviceSettings.class,
                StringUtils.isEmpty(deviceSettingsPath) ? SETTINGS_FILE_DEFAULT : Paths.get(deviceSettingsPath),
                objectMapper
        );
    }

    @Bean
    public FileRepository<DeviceInfo> savedDevicesRepository(ObjectMapper objectMapper) {
        return new FileRepository<>(
                DeviceInfo.class,
                StringUtils.isEmpty(savedDevicesPath) ? SAVED_DEVICES_DEFAULT : Paths.get(savedDevicesPath),
                objectMapper
        );
    }

    @Bean
    public FileRepository<DashboardGroup> dashboardRepository(ObjectMapper objectMapper) {
        return new FileRepository<>(
                DashboardGroup.class,
                StringUtils.isEmpty(dashboardConfigPath) ? DASHBOARD_DEFAULT_PATH : Paths.get(dashboardConfigPath),
                objectMapper
        );
    }
}
