package ru.pobopo.smart.thing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.pobopo.smart.thing.gateway.event.CloudConnectionEvent;
import ru.pobopo.smart.thing.gateway.model.GatewayStatus;

@Service
@Slf4j
public class GatewayStatusService {
    private final GatewayStatus gatewayStatus = new GatewayStatus();
    
    public GatewayStatus getStatus() {
        return gatewayStatus;
    }

    @EventListener
    public void connectionEvent(CloudConnectionEvent event) {
        switch (event.getEventType()) {
            case CONNECTION_CLOSED -> gatewayStatus.connectionClosed();
            case GATEWAY_INFO_LOADED -> gatewayStatus.setGatewayInfoLoaded(true);
            case CONNECTED_TO_BROKER -> gatewayStatus.setConnectedToBroker(true);
            case SUBSCRIBED_TO_QUEUE -> gatewayStatus.setSubscribedToQueue(true);
        }
        log.info("Event: {}", event.getEventType());
        log.info("Status: {}", gatewayStatus);
    }

}
