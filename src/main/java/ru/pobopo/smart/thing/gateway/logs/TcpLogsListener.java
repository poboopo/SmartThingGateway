package ru.pobopo.smart.thing.gateway.logs;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.model.DeviceLogSource;
import ru.pobopo.smart.thing.gateway.model.DeviceLoggerMessage;
import ru.pobopo.smart.thing.gateway.service.DeviceLogsProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpLogsListener implements LogsListener {
    @Value("${device.logs.tcp.port}")
    private String port;

    private final DeviceLogsProcessor logsProcessor;

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

                LogClient client = new LogClient(logsProcessor, socketClient);
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
        private final DeviceLogsProcessor logsProcessor;

        public LogClient(DeviceLogsProcessor logsProcessor, Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.logsProcessor = logsProcessor;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String message;
                while ((message = in.readLine()) != null) {
                    DeviceLoggerMessage deviceLoggerMessage = DeviceLoggerMessage.parse(message);
                    deviceLoggerMessage.setSource(DeviceLogSource.TCP);
                    logsProcessor.addLog(deviceLoggerMessage);
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
