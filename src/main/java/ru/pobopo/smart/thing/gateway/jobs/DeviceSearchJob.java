package ru.pobopo.smart.thing.gateway.jobs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.cache.ConcurrentSetCache;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.service.DeviceService;

@Component
@Slf4j
public class DeviceSearchJob implements BackgroundJob {
    // TODO MOVE ALL TO ENV
    public static final String DEVICES_SEARCH_TOPIC = "/devices/search";
    private final static String GATEWAY_CONFIG_FIELD = "gtw";
    private final static String GROUP = "224.1.1.1";
    private final static int PORT = 7778;

    @Value("${server.ip}")
    private String gatewayIp;
    @Value("${server.port}")
    private String gatewayPort;

    private final ConcurrentSetCache<DeviceInfo> cache = new ConcurrentSetCache<>(5, ChronoUnit.SECONDS);
    private final Set<String> foundIps = ConcurrentHashMap.newKeySet();

    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceService deviceService;

    @Autowired
    public DeviceSearchJob(SimpMessagingTemplate messagingTemplate, DeviceService deviceService) {
        this.messagingTemplate = messagingTemplate;
        this.deviceService = deviceService;
    }

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
                    messagingTemplate.convertAndSend(
                            DEVICES_SEARCH_TOPIC,
                            deviceInfo
                    );
                    setupDevice(deviceInfo);
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

    private void setupDevice(DeviceInfo info) {
        if (foundIps.contains(info.getIp())) {
            return;
        }

        if (getThreadByName(info.getIp()) != null) {
            log.debug("Device with ip {} already have setup job", info.getIp());
            return;
        }

        log.debug("Starting setup job for {}", info);
        new Thread(() -> {
            try {
                Map<String, Object> config = deviceService.getConfigValues(info);
                if (config == null || !StringUtils.isNotBlank((String) config.get(GATEWAY_CONFIG_FIELD))) {
                    log.debug("Setting up device");
                    boolean res = deviceService.addConfigValues(info, Map.of(GATEWAY_CONFIG_FIELD, gatewayIp + ":" + gatewayPort));
                    if (!res) {
                        throw new Exception("Failed to save device config");
                    }
                } else {
                    log.debug("Device already configured");
                }
                foundIps.add(info.getIp());
            } catch (Exception exception) {
                log.error("Device setup failed!", exception);
            }
        }, info.getIp()).start();
    }

    public Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) return t;
        }
        return null;
    }
}
