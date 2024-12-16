package ru.pobopo.smartthing.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smartthing.consumers.DeviceLogsConsumer;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.service.AsyncQueuedConsumersProcessor;
import ru.pobopo.smartthing.gateway.service.plugins.CustomPluginsService;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;
import ru.pobopo.smartthing.model.stomp.DeviceNotification;

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
