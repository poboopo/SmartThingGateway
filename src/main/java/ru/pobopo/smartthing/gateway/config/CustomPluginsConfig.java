package ru.pobopo.smartthing.gateway.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.model.CustomPlugin;
import ru.pobopo.smartthing.gateway.service.CustomPluginsHelper;
import ru.pobopo.smartthing.consumers.DashboardUpdatesConsumer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Slf4j
@Configuration
public class CustomPluginsConfig {
    private static final Path PLUGINS_DIR_DEFAULT = Path.of(DEFAULT_APP_DIR.toString(), "plugins");

    @Bean
    @Qualifier("notification-consumers")
    public List<DeviceNotificationConsumer> consumersFromPlugin(
            List<DeviceNotificationConsumer> beans,
            List<CustomPlugin> plugins
    ) {
        beans.addAll(CustomPluginsHelper.createBeansFromPlugins(plugins, DeviceNotificationConsumer.class));
        return beans;
    }

    @Bean
    @Qualifier("dashboard-consumer")
    public List<DashboardUpdatesConsumer> dashboardUpdatesConsumers(
            List<DashboardUpdatesConsumer> beans,
            List<CustomPlugin> plugins
    ) {
        beans.addAll(CustomPluginsHelper.createBeansFromPlugins(plugins, DashboardUpdatesConsumer.class));
        return beans;
    }

    @Bean
    @SneakyThrows
    public List<CustomPlugin> loadPlugins(@Value("${plugins.dir:}") String pluginsDir) {
        Path dirPath = StringUtils.isEmpty(pluginsDir) ? PLUGINS_DIR_DEFAULT : Path.of(pluginsDir);

        log.info("Loading plugins from {}", pluginsDir);
        List<CustomPlugin> plugins = Files.list(dirPath)
                .map(CustomPluginsHelper::loadPlugin)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        log.info("Loaded plugins count: {}", plugins.size());
        return plugins;
    }
}
