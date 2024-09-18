package ru.pobopo.smartthing.gateway.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardGroup;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardObservableValueUpdate;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardUpdatesConsumer;

import java.util.List;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DASHBOARD_TOPIC_PREFIX;

@Component
@RequiredArgsConstructor
public class WsValuesConsumer implements DashboardUpdatesConsumer {
    private final SimpMessagingTemplate template;

    @Override
    public void consume(DashboardGroup group, List<DashboardObservableValueUpdate> updates) {
        template.convertAndSend(
                DASHBOARD_TOPIC_PREFIX + "/" + group.getId(),
                updates
        );
    }
}
