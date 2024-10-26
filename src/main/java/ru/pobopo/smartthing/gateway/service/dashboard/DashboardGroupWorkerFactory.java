package ru.pobopo.smartthing.gateway.service.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.service.device.api.RestDeviceApi;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardGroup;
import ru.pobopo.smartthing.consumers.DashboardUpdatesConsumer;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardGroupWorkerFactory {
    private final ObjectMapper objectMapper;
    private final RestDeviceApi restDeviceApi;
    private final List<DashboardUpdatesConsumer> valuesConsumers;

    public DashboardGroupWorker create(DashboardGroup group) {
        return new DashboardGroupWorker(
                group,
                restDeviceApi,
                objectMapper,
                updates -> valuesConsumers.forEach(consumer -> consumer.consume(group, updates)) //todo async
        );
    }
}
