package ru.pobopo.smartthing.gateway.service;

import jakarta.annotation.PreDestroy;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.IncompatibleConfigurationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.pobopo.smartthing.gateway.model.CloudConfig;
import ru.pobopo.smartthing.gateway.model.CloudConnectionStatus;
import ru.pobopo.smartthing.gateway.model.CloudConnectionStatusMessage;
import ru.pobopo.smartthing.gateway.stomp.CustomStompSessionHandler;
import ru.pobopo.smartthing.model.stomp.GatewayEventType;

import java.util.concurrent.ExecutionException;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.CONNECTION_STATUS_TOPIC;

@Component
@Slf4j
public class CloudMessageBrokerService {
    private final WebSocketStompClient stompClient;
    private final CustomStompSessionHandler sessionHandler;
    private final SimpMessagingTemplate messagingTemplate;
    private final CloudService cloudService;

    @Value("${cloud.reconnect.attempts:-1}")
    private int reconnectAttempts;
    @Value("${cloud.reconnect.pause:10000}")
    private int reconnectPause;

    private volatile CloudConnectionStatus connectionStatus = CloudConnectionStatus.NOT_CONNECTED;
    private StompSession stompSession;
    private Thread reconnectThread;
    private boolean reconnectFailed = false;

    public CloudMessageBrokerService(WebSocketStompClient stompClient, CloudService cloudService, CustomStompSessionHandler sessionHandler, SimpMessagingTemplate messagingTemplate) {
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
        if (status == null || connectionStatus.equals(status)) {
            return;
        }
        log.info("New connection status: {}", status);
        this.connectionStatus = status;
        this.messagingTemplate.convertAndSend(CONNECTION_STATUS_TOPIC, new CloudConnectionStatusMessage(status));

        switch (status) {
            case CONNECTED -> {
                stopReconnectThread();
                reconnectFailed = false;
                cloudService.event(GatewayEventType.CONNECTED);
            }
            case CONNECTION_LOST -> startReconnectThread();
            case DISCONNECTED -> cloudService.event(GatewayEventType.DISCONNECTED);
        }
    }

    public void logout() {
        disconnect();
        setStatus(CloudConnectionStatus.NOT_CONNECTED);
    }

    @SneakyThrows
    public void connect(CloudConnectionStatus fallbackStatus) {
        stopReconnectThread();
        disconnect();
        try {
            if (connectionStatus == CloudConnectionStatus.CONNECTING
                    || connectionStatus == CloudConnectionStatus.CONNECTED
                    || connectionStatus == CloudConnectionStatus.RECONNECTING
            ) {
                return;
            }
            setStatus(CloudConnectionStatus.CONNECTING);
            connectWs();
        } catch (Exception e) {
            setStatus(fallbackStatus != null ? fallbackStatus : CloudConnectionStatus.FAILED_TO_CONNECT);
            throw e;
        }
    }

    private void connectWs() throws ExecutionException, InterruptedException {
        CloudConfig cloudConfig = cloudService.getCloudConfig();
        if (cloudConfig == null || StringUtils.isBlank(cloudConfig.getCloudUrl()) || StringUtils.isBlank(cloudConfig.getToken())) {
            throw new IncompatibleConfigurationException("Can't find cloud ul or auth token in cloud config!");
        }

        String wsUrl = cloudConfig.getCloudUrl().replace("http", "ws") + "/smt-ws";
        log.info("Connecting to {}", wsUrl);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add(CloudService.AUTH_TOKEN_HEADER, cloudConfig.getToken());

        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        stompSession = stompClient.connectAsync(wsUrl, headers, sessionHandler).get();
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
            while (
                    (stompSession == null || !stompSession.isConnected())
                    && (reconnectAttempts == -1 || attempt < reconnectAttempts)
                    && !Thread.interrupted()
            ) {
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
                setStatus(CloudConnectionStatus.FAILED_TO_CONNECT);
                reconnectFailed = true;
            } else {
                log.info("Connection restored!");
            }
        });
        reconnectThread.setDaemon(true);

        log.info("Starting reconnect thread");
        reconnectThread.start();
    }

    @Scheduled(fixedDelayString = "${cloud.status.check.delay}")
    public void checkStatus() {
        if (CloudConnectionStatus.CONNECTED.equals(connectionStatus)) {
            return;
        }
        if (stompSession != null && !stompSession.isConnected()) {
            log.info("Connection lost detected!");
            setStatus(CloudConnectionStatus.CONNECTION_LOST);
        }
    }
}
