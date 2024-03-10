package ru.pobopo.smart.thing.gateway.service;

import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.IncompatibleConfigurationException;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.pobopo.smart.thing.gateway.event.*;
import ru.pobopo.smart.thing.gateway.exception.StorageException;
import ru.pobopo.smart.thing.gateway.model.CloudConfig;
import ru.pobopo.smart.thing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smart.thing.gateway.model.CloudConnectionStatusMessage;
import ru.pobopo.smart.thing.gateway.stomp.CustomStompSessionHandler;

import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class MessageBrokerService {
    public static final String CONNECTION_STATUS_TOPIC = "/connection/status";

    private final WebSocketStompClient stompClient;
    private final CustomStompSessionHandler sessionHandler;
    private final SimpMessagingTemplate messagingTemplate;
    private final CloudService cloudService;

    @Value("${server.reconnect.attempts}")
    private int reconnectAttempts;
    @Value("${server.reconnect.pause}")
    private int reconnectPause;

    private CloudConnectionStatus connectionStatus = CloudConnectionStatus.NOT_CONNECTED;
    private StompSession stompSession;
    private Thread reconnectThread;
    private boolean reconnectFailed = false;

    public MessageBrokerService(WebSocketStompClient stompClient, CloudService cloudService, CustomStompSessionHandler sessionHandler, SimpMessagingTemplate messagingTemplate) {
        this.stompClient = stompClient;
        this.sessionHandler = sessionHandler;
        this.messagingTemplate = messagingTemplate;
        this.cloudService = cloudService;

        this.sessionHandler.setStatusConsumer(this::setStatus);
    }

    public CloudConnectionStatus getStatus() {
        return connectionStatus;
    }

    synchronized public void setStatus(CloudConnectionStatus status) {
        log.info("New connection status: {}", status);
        this.connectionStatus = status;
        this.messagingTemplate.convertAndSend(CONNECTION_STATUS_TOPIC, new CloudConnectionStatusMessage(status));

        if (status == CloudConnectionStatus.CONNECTION_LOST) {
             startReconnectThread();
        }
        if (status == CloudConnectionStatus.CONNECTED) {
            stopReconnectThread();
            reconnectFailed = false;
        }
    }

    public void logout() {
        disconnect();
        setStatus(CloudConnectionStatus.NOT_CONNECTED);
    }

    public void connect() {
        stopReconnectThread();
        disconnect();
        try {
            if (connectionStatus == CloudConnectionStatus.CONNECTING || connectionStatus == CloudConnectionStatus.CONNECTED) {
                return;
            }
            setStatus(CloudConnectionStatus.CONNECTING);
            connectWs();
        } catch (Exception e) {
            log.error("Failed to connect", e);
            setStatus(CloudConnectionStatus.FAILED_TO_CONNECT);
        }
    }

    private void connectWs() throws ExecutionException, InterruptedException {
        CloudConfig cloudConfig = cloudService.getCloudConfig();
        if (cloudConfig == null) {
            throw new IncompatibleConfigurationException("Can't find token in cloud config");
        }

        String url = String.format(
                "ws://%s:%d/ws",
                cloudConfig.getCloudIp(),
                cloudConfig.getCloudPort()
        );
        log.info("Connecting to {}", url);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add(CloudService.AUTH_TOKEN_HEADER, cloudConfig.getToken());

        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        stompSession = stompClient.connectAsync(url, headers, sessionHandler).get();
    }

    @PreDestroy
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("Stop session disconnected");
        }
        setStatus(CloudConnectionStatus.DISCONNECTED);
    }

    private void stopReconnectThread() {
        if (reconnectThread != null && reconnectThread.isAlive()) {
            log.info("Stopping reconnect thread");
            reconnectThread.interrupt();
            reconnectThread = null;
        }
    }

    private void startReconnectThread() {
        if (reconnectFailed) {
            return;
        }
        if (reconnectAttempts == 0) {
            log.info("Reconnect disabled");
            return;
        }
        setStatus(CloudConnectionStatus.RECONNECTING);
        if (reconnectThread != null && reconnectThread.isAlive()) {
            log.info("Reconnect thread already running");
            return;
        }
        reconnectThread = new Thread(() -> {
            int attempt = 0;
            while ((stompSession == null || !stompSession.isConnected()) && attempt < reconnectAttempts) {
                try {
                    Thread.sleep(reconnectPause);
                    log.info("Reconnect attempt №{}", attempt);
                    connectWs();
                } catch (InterruptedException exception) {
                    break;
                } catch (Exception exception) {
                    log.warn("Failed to reconnect: {}", exception.getMessage());
                } finally {
                    attempt++;
                }
            }
            log.info("Reconnect thread stopped");
            if (stompSession == null || !stompSession.isConnected()) {
                setStatus(CloudConnectionStatus.CONNECTION_LOST);
                reconnectFailed = true;
            }
        });
        reconnectThread.setDaemon(true);

        log.info("Starting reconnect thread");
        reconnectThread.start();
    }

}
