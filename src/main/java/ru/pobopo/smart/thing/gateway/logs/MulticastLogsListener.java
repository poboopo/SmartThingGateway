package ru.pobopo.smart.thing.gateway.logs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.model.DeviceLogSource;
import ru.pobopo.smart.thing.gateway.model.DeviceLoggerMessage;
import ru.pobopo.smart.thing.gateway.service.DeviceLogsService;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MulticastLogsListener implements LogsListener {

    @Value("${device.logs.multicast.group}")
    private String group;
    @Value("${device.logs.multicast.port}")
    private String port;

    private final DeviceLogsService logsProcessor;

    @Override
    public void run() {
        log.info("UDP logs started");
        try {
            listen();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void listen() throws Exception {
        if (StringUtils.isEmpty(group) || StringUtils.isEmpty(port)) {
            log.error("Multicast logs group or port missing! Leaving");
            return;
        }

        MulticastSocket s = new MulticastSocket(Integer.parseInt(port));
        InetAddress groupAddr = InetAddress.getByName(group);
        byte[] buf = new byte[4096];

        try {
            s.joinGroup(groupAddr);
            for (; ; ) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                s.receive(packet);

                String message = new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );
                DeviceLoggerMessage deviceLoggerMessage = DeviceLoggerMessage.parse(message);
                deviceLoggerMessage.setSource(DeviceLogSource.MULTICAST);
                logsProcessor.addLog(deviceLoggerMessage);
            }
        } finally {
            s.leaveGroup(groupAddr);
            s.close();
        }
    }
}
