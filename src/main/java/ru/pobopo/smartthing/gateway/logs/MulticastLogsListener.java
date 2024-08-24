package ru.pobopo.smartthing.gateway.logs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.jobs.BackgroundJob;
import ru.pobopo.smartthing.gateway.service.DeviceLogsService;
import ru.pobopo.smartthing.model.DeviceLogSource;
import ru.pobopo.smartthing.model.DeviceLoggerMessage;

import java.net.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MulticastLogsListener implements BackgroundJob {

    @Value("${device.logs.multicast.group}")
    private String group;
    @Value("${device.logs.multicast.port}")
    private String port;

    private final DeviceLogsService logsProcessor;
    private final DeviceLoggerMessageParser messageParser;

    @Override
    public void run() {
        log.info("UDP logs started");
        try {
            if (StringUtils.isEmpty(group) || StringUtils.isEmpty(port)) {
                log.error("Multicast logs group or port missing! Leaving");
                return;
            }

            SocketAddress address = new InetSocketAddress(InetAddress.getByName(group), Integer.parseInt(port));
            MulticastSocket multicastSocket = new MulticastSocket(address);
            byte[] buf = new byte[4096];

            try {
                multicastSocket.joinGroup(address, multicastSocket.getNetworkInterface());
                for (; ; ) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(packet);

                    String message = new String(
                            packet.getData(),
                            packet.getOffset(),
                            packet.getLength(),
                            StandardCharsets.UTF_8
                    );
                    logsProcessor.addLog(
                            messageParser.parse(DeviceLogSource.MULTICAST, message, null)
                    );
                }
            } finally {
                multicastSocket.leaveGroup(address, multicastSocket.getNetworkInterface());
                multicastSocket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
