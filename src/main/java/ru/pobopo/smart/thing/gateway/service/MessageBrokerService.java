package ru.pobopo.smart.thing.gateway.service;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.ExecutionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.pobopo.smart.thing.gateway.controller.model.SendNotificationRequest;
import ru.pobopo.smart.thing.gateway.event.*;
import ru.pobopo.smart.thing.gateway.exception.ConfigurationException;
import ru.pobopo.smart.thing.gateway.model.CloudAuthInfo;
import ru.pobopo.smart.thing.gateway.model.GatewayInfo;
import ru.pobopo.smart.thing.gateway.model.Notification;
import ru.pobopo.smart.thing.gateway.stomp.CustomStompSessionHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageBrokerService {
    private final WebSocketStompClient stompClient;
    private final ConfigurationService configurationService;
    private final CustomStompSessionHandler sessionHandler;
    private final CloudService cloudService;

    private StompSession stompSession;

    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
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
    public void connect(AuthorizedEvent event) {
        connect(event.getAuthorizedCloudUser().getGateway());
    }

    public boolean connect(GatewayInfo gatewayInfo) {
        cleanup();
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

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add(CloudService.AUTH_TOKEN_HEADER, authInfo.getToken());
            stompSession = stompClient.connectAsync(url, headers, sessionHandler).get();
            return true;
        } catch (ConfigurationException exception) {
            log.error("Failed to configure rabbitmq connection: {}", exception.getMessage());
        } catch (Exception e) {
            log.error("Failed to connect", e);
        }
        return false;
    }

    @PreDestroy
    public void cleanup() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("Stomp connection disconnected");
        }
    }
}
