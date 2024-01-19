package ru.pobopo.smart.thing.gateway.logs;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.model.DeviceLoggerMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ru.pobopo.smart.thing.gateway.service.LogJobsService.DEVICES_LOGS_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpLogsListener implements LogsListener {
    @Value("${device.logs.tcp.port}")
    private String port;

    private final SimpMessagingTemplate messagingTemplate;

    private ServerSocket serverSocket;
    private final HashMap<String, LogClient> clients = new HashMap<>();

    @Override
    public void listen() throws Exception {
        if (StringUtils.isEmpty(port)) {
            log.error("Tcp logs port missing! Leaving");
            return;
        }
        serverSocket = new ServerSocket(Integer.parseInt(port));
        log.info("TCP logs started, waiting for connections...");

        while(true) {
            try {
                Socket socketClient = serverSocket.accept();
                String ip = socketClient.getInetAddress().getHostAddress();
                if (clients.containsKey(ip)) {
                    log.warn("Already have connection with {}, closing", ip);
                    clients.remove(ip).stopClient();
                }

                LogClient client = new LogClient(messagingTemplate, socketClient);
                client.setDaemon(true);
                client.start();
                clients.put(ip, client);
                log.info(
                        "Got new log client: {}, total clients count: {}",
                        ip,
                        clients.size()
                );
            } catch (Exception exception) {
                log.error("Tcp logger error: {}", exception.getMessage());
            }
        }
    }

    @Override
    public void run() {
        try {
            listen();
        } catch (Exception e) {
            log.error("Tcp logs listen exception: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() throws IOException {
        clients.values().forEach(client -> {
            try {
                client.stopClient();
            } catch (IOException e) {
                log.error("Failed to stop log client: {}", e.getMessage());
            }
        });
        serverSocket.close();
    }

    private static class LogClient extends Thread {
        private final Socket clientSocket;
        private final SimpMessagingTemplate messagingTemplate;

        public LogClient(SimpMessagingTemplate messagingTemplate, Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.messagingTemplate = messagingTemplate;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String message;
                while ((message = in.readLine()) != null) {
                    DeviceLoggerMessage deviceLoggerMessage = DeviceLoggerMessage.parse(message);
                    log.info(deviceLoggerMessage.toString());
                    messagingTemplate.convertAndSend(
                            DEVICES_LOGS_TOPIC,
                            deviceLoggerMessage
                    );
                }

                in.close();
                clientSocket.close();
                log.info("Log client finished: {}", clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                log.error("Tcp client log stop: {}", e.getMessage());
            }
        }

        public void stopClient() throws IOException {
            clientSocket.close();
        }
    }
}
