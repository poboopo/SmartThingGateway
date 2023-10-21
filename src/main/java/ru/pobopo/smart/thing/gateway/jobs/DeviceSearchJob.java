package ru.pobopo.smart.thing.gateway.jobs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.cache.ConcurrentSetCache;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;

@Component
@Slf4j
public class DeviceSearchJob implements BackgroundJob {
    public static final String DEVICES_SEARCH_TOPIC = "/devices/search";

    private final static String GROUP = "224.1.1.1";
    private final static int PORT = 7778;
    private final ConcurrentSetCache<DeviceInfo> cache = new ConcurrentSetCache<>(2, ChronoUnit.SECONDS);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DeviceSearchJob(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void run() {
        log.info("Device search job started");
        try {
            search();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void search() throws IOException {
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

                DeviceInfo deviceInfo = DeviceInfo.fromMulticastMessage(message);

                if (deviceInfo != null) {
                    log.debug(deviceInfo.toString());
                    messagingTemplate.convertAndSend(
                        DEVICES_SEARCH_TOPIC,
                        deviceInfo
                    );
                    cache.put(deviceInfo);
                } else {
                    log.error("Can't build device info for {}", message);
                }
            }
        } catch (Exception exception) {
            log.error("Search job failed", exception);
            throw new RuntimeException(exception);
        } finally {
            s.leaveGroup(group);
            s.close();
        }
    }

    @NonNull
    public Set<DeviceInfo> getRecentFoundDevices() {
        return cache.getValues();
    }
}
