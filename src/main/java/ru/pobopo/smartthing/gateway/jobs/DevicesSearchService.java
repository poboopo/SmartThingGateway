package ru.pobopo.smartthing.gateway.jobs;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.cache.ConcurrentSetCache;
import ru.pobopo.smartthing.model.DeviceInfo;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DEVICES_TOPIC;

@Component
@Slf4j
@RequiredArgsConstructor
public class DevicesSearchService implements BackgroundJob {
    // TODO MOVE ALL TO ENV
    public static final String DEVICES_SEARCH_TOPIC = DEVICES_TOPIC + "/search";
    private final static String GROUP = "224.1.1.1";
    private final static int PORT = 7778;

    private final ConcurrentSetCache<DeviceInfo> cache = new ConcurrentSetCache<>(5, ChronoUnit.SECONDS);
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void run() {
        log.info("Device search job started");
        try {
            search();
        } catch (IOException exception) {
            log.error("Search job stopped!", exception);
            throw new RuntimeException(exception);
        }
    }

    private void search() throws IOException {
        SocketAddress address = new InetSocketAddress(InetAddress.getByName(GROUP), PORT);
        MulticastSocket multicastSocket = new MulticastSocket(address);
        byte[] buf = new byte[4096];

        try {
            multicastSocket.joinGroup(address, multicastSocket.getNetworkInterface());
            while(!Thread.interrupted()) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);

                String message = new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );

                DeviceInfo deviceInfo = DeviceInfo.fromMulticastMessage(message);

                if (deviceInfo != null) {
                    messagingTemplate.convertAndSend(
                            DEVICES_SEARCH_TOPIC,
                            deviceInfo
                    );
                    cache.put(deviceInfo);
                } else {
                    log.error("Can't build device info from {}", message);
                }
            }
        } catch (Exception exception) {
            log.error("Search job failed", exception);
            throw new RuntimeException(exception);
        } finally {
            multicastSocket.leaveGroup(address, multicastSocket.getNetworkInterface());
            multicastSocket.close();
        }
    }

    @NonNull
    public Set<DeviceInfo> getRecentFoundDevices() {
        return cache.getValues();
    }
}
