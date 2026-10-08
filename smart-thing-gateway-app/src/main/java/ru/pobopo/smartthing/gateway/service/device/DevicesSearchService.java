package ru.pobopo.smartthing.gateway.service.device;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.config.properties.DeviceSearchProperties;
import ru.pobopo.smartthing.gateway.service.job.BackgroundJob;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.DEVICES_TOPIC;

@Slf4j
@Component
public class DevicesSearchService implements BackgroundJob {
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceSearchProperties properties;
    private final Cache<String, DeviceInfo> foundDevices;

    private final AtomicBoolean socketConnected = new AtomicBoolean(false);

    public DevicesSearchService(
            SimpMessagingTemplate messagingTemplate,
            DeviceSearchProperties properties
    ) {
        this.messagingTemplate = messagingTemplate;
        this.properties = properties;

        foundDevices = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .maximumSize(100)
                .evictionListener(this::onDeviceLost)
                .build();
    }

    // todo do we need that?
    public boolean isSearchEnabled() {
        return socketConnected.get();
    }

    @NonNull
    public Collection<DeviceInfo> getRecentFoundDevices() {
        return foundDevices.asMap().values();
    }

    @Override
    public void run() {
        log.info("Device search job started");
        try {
            search();
        } catch (IOException exception) {
            log.error("Search job stopped!", exception);
        }
    }

    private void search() throws IOException {
        SocketAddress address = new InetSocketAddress(InetAddress.getByName(properties.getGroup()), properties.getPort());
        MulticastSocket multicastSocket = new MulticastSocket(address);
        byte[] buf = new byte[4096];

        try {
            multicastSocket.joinGroup(address, multicastSocket.getNetworkInterface());
            log.info("Listening for devices beacons at {}:{}", properties.getGroup(), properties.getPort());
            this.socketConnected.lazySet(true);
            while (!Thread.interrupted()) {
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
                    if (foundDevices.getIfPresent(deviceInfo.getIp()) == null) {
                        onDeviceFound(deviceInfo);
                    }
                    foundDevices.put(deviceInfo.getIp(), deviceInfo);
                } else {
                    log.debug("Can't build device info from {}", message);
                }
            }
        } catch (Exception exception) {
            log.error("Search job failed", exception);
            this.socketConnected.lazySet(false);
            throw new RuntimeException(exception);
        } finally {
            multicastSocket.leaveGroup(address, multicastSocket.getNetworkInterface());
            multicastSocket.close();
        }
    }

    private void onDeviceFound(DeviceInfo info) {
        messagingTemplate.convertAndSend(DEVICES_TOPIC + "/found", info);
    }

    private void onDeviceLost(String ip, DeviceInfo info, RemovalCause cause) {
        messagingTemplate.convertAndSend(DEVICES_TOPIC + "/lost", info);
    }
}
