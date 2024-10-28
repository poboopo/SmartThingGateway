package ru.pobopo.smartthing.gateway.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardGroup;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardObservableValueUpdate;
import ru.pobopo.smartthing.consumers.DashboardUpdatesConsumer;
import ru.pobopo.smartthing.model.gateway.dashboard.DashboardValuesUpdate;

import java.util.List;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DASHBOARD_TOPIC_PREFIX;

@Component
@RequiredArgsConstructor
public class WsValuesConsumer implements DashboardUpdatesConsumer {
    private final SimpMessagingTemplate template;

    @Override
    public void accept(DashboardValuesUpdate update) {
        template.convertAndSend(
                DASHBOARD_TOPIC_PREFIX + "/" + update.getGroup().getId(),
                update.getValues()
        );
    }
}
