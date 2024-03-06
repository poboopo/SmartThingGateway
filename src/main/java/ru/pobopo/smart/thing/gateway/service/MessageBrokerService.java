package ru.pobopo.smart.thing.gateway.service;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.pobopo.smart.thing.gateway.controller.model.SendNotificationRequest;
import ru.pobopo.smart.thing.gateway.event.*;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smart.thing.gateway.model.CloudConnectionStatusMessage;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.stomp.CustomStompSessionHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageBrokerService {
    public static final String CONNECTION_STATUS_TOPIC = "/connection/status";

    private final WebSocketStompClient stompClient;
    private final ConfigurationService configurationService;
    private final CustomStompSessionHandler sessionHandler;
    private final CloudService cloudService;
    private final SimpMessagingTemplate messagingTemplate;

    private CloudConnectionStatus cloudConnectionStatus = CloudConnectionStatus.NOT_CONNECTED;
    private StompSession stompSession;

    public CloudConnectionStatus getStatus() {
        return cloudConnectionStatus;
    }

    synchronized public void setStatus(CloudConnectionStatus status) {
        this.cloudConnectionStatus = status;
        this.messagingTemplate.convertAndSend(CONNECTION_STATUS_TOPIC, new CloudConnectionStatusMessage(status));
        //todo send logout event to cloud
    }

    public boolean sendNotification(SendNotificationRequest notificationRequest) {
        if (notificationRequest == null) {
            return false;
        }

        if (!isConnected()) {
            log.warn("Not connected to the cloud!");
            return false;
        }

        try {
            cloudService.notification(notificationRequest);
            return true;
        } catch (Exception exception) {
            log.error("Failed to send notification in cloud: {}", exception.getMessage(), exception);
        }

        return false;
    }

    @EventListener
    public void connect(CloudLoginEvent event) {
        connect(event.getAuthorizedCloudUser().getGateway());
    }

    @EventListener
    public void logout(CloudLogoutEvent event) {
        log.info("Logout event! Disconnecting from the cloud.");
        disconnect();
    }

    public boolean connect(GatewayInfo gatewayInfo) {
        disconnect();
        try {
            if (gatewayInfo == null) {
                throw new ConfigurationException("Gateway info is missing");
            }

            CloudAuthInfo authInfo = configurationService.getCloudAuthInfo();
            if (authInfo == null) {
                throw new ConfigurationException("Cloud info is missing");
            }

            String url = String.format(
                    "ws://%s:%d/stomp",
                    authInfo.getCloudIp(),
                    authInfo.getCloudPort()
            );

            sessionHandler.setGatewayInfo(gatewayInfo);
            sessionHandler.setStatusConsumer(this::setStatus);

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add(CloudService.AUTH_TOKEN_HEADER, authInfo.getToken());
            setStatus(CloudConnectionStatus.CONNECTING);
            stompSession = stompClient.connectAsync(url, headers, sessionHandler).get();
            return true;
        } catch (Exception e) {
            log.error("Failed to connect", e);
            setStatus(CloudConnectionStatus.FAILED_TO_CONNECT);
        }
        return false;
    }

    @PreDestroy
    public boolean disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("Stomp disconnected");
            setStatus(CloudConnectionStatus.DISCONNECTED);
            return true;
        }
        return false;
    }

    private boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
}
