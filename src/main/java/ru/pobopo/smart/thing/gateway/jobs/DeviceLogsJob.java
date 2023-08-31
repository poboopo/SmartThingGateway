package ru.pobopo.smart.thing.gateway.jobs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.model.DeviceLoggerMessage;

@Component
@Slf4j
public class DeviceLogsJob implements Runnable {
    public static String DEVICES_LOGS_TOPIC = "/devices/logs";

    private final static String GROUP = "224.1.1.1";
    private final static int PORT = 7779;

    private final Logger multicastLogs = LoggerFactory.getLogger("multicast-logs");
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DeviceLogsJob(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void run() {
        log.info("Device logs job started");
        try {
            receiveLogs();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveLogs() throws IOException {
        MulticastSocket s = new MulticastSocket(PORT);
        InetAddress group = InetAddress.getByName(GROUP);
        byte[] buf = new byte[4096];

        try {
            s.joinGroup(group);
            for (; ; ) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                s.receive(packet);

                String message = new String(
                    packet.getData(),
                    packet.getOffset(),
                    packet.getLength(),
                    StandardCharsets.UTF_8
                );
                DeviceLoggerMessage deviceLoggerMessage = DeviceLoggerMessage.fromMulticastMessage(message);

                multicastLogs.info(deviceLoggerMessage.toString());
                messagingTemplate.convertAndSend(
                    DEVICES_LOGS_TOPIC,
                    deviceLoggerMessage
                );
            }
        } finally {
            s.leaveGroup(group);
            s.close();
        }
    }
}
