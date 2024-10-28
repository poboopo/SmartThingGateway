package ru.pobopo.smartthing.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smartthing.consumers.DashboardUpdatesConsumer;
import ru.pobopo.smartthing.consumers.DeviceLogsConsumer;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.service.CustomPluginsService;
import ru.pobopo.smartthing.gateway.service.AsyncQueuedConsumersProcessor;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;
import ru.pobopo.smartthing.model.DeviceNotification;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardValuesUpdate;

import java.util.List;

@Configuration
public class QueuedProcessorsConfig {
    @Bean
    @Qualifier("notification-processor")
    public AsyncQueuedConsumersProcessor<DeviceNotificationConsumer, DeviceNotification> notificationsProcessor(
            CustomPluginsService service,
            List<DeviceNotificationConsumer> beans
    ) {
        beans.addAll(service.createBeansFromPlugins(DeviceNotificationConsumer.class));
        AsyncQueuedConsumersProcessor<DeviceNotificationConsumer, DeviceNotification> processor =
                new AsyncQueuedConsumersProcessor<>("notify-proc", beans);
        processor.start();
        return processor;
    }

    @Bean
    @Qualifier("dashboard-processor")
    public AsyncQueuedConsumersProcessor<DashboardUpdatesConsumer, DashboardValuesUpdate> dashboardProcessor(
            CustomPluginsService service,
            List<DashboardUpdatesConsumer> beans
    ) {
        beans.addAll(service.createBeansFromPlugins(DashboardUpdatesConsumer.class));
        AsyncQueuedConsumersProcessor<DashboardUpdatesConsumer, DashboardValuesUpdate> processor =
                new AsyncQueuedConsumersProcessor<>("dashboard-proc", beans);
        processor.start();
        return processor;
    }

    @Bean
    @Qualifier("device-logs-processor")
    public AsyncQueuedConsumersProcessor<DeviceLogsConsumer, DeviceLoggerMessage> logsProcessor(
            CustomPluginsService service,
            List<DeviceLogsConsumer> beans
    ) {
        beans.addAll(service.createBeansFromPlugins(DeviceLogsConsumer.class));
        AsyncQueuedConsumersProcessor<DeviceLogsConsumer, DeviceLoggerMessage> processor =
                new AsyncQueuedConsumersProcessor<>("dev-log-proc", beans);
        processor.start();
        return processor;
    }
}
