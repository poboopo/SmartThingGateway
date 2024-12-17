package ru.pobopo.smartthing.gateway.service.ota;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareInfo;
import ru.pobopo.smartthing.gateway.model.ota.OtaFirmwareTaskStatus;
import ru.pobopo.smartthing.gateway.model.ota.OtaUploadProgressShort;
import ru.pobopo.smartthing.model.device.DeviceInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.pobopo.smartthing.gateway.config.StompMessagingConfig.OTA_PROGRESS_TOPIC;

@Slf4j
public class OtaFirmwareUploadTask implements Runnable {
    private final static int UPLOAD_COMMAND = 0;
    private final static int STEP_SIZE = 1024;
    private final static int CONFIRMATION_TIMEOUT = 50000;

    @Getter
    private final UUID id = UUID.randomUUID();
    @Getter
    private final OtaFirmwareInfo firmwareInfo;
    private final DeviceInfo device;
    private final byte[] data;
    private final int invitationPort;
    private final SimpMessagingTemplate messagingTemplate;
    private final String topic;

    @Getter
    private volatile OtaFirmwareTaskStatus status;
    @Getter
    private final AtomicInteger transferProgress = new AtomicInteger(0);

    public OtaFirmwareUploadTask(OtaFirmwareInfo firmwareInfo, DeviceInfo device, byte[] data, int invitationPort, SimpMessagingTemplate messagingTemplate) {
        this.firmwareInfo = firmwareInfo;
        this.device = device;
        this.data = data;
        this.invitationPort = invitationPort;
        this.messagingTemplate = messagingTemplate;
        this.topic = String.format("%s/%s", OTA_PROGRESS_TOPIC, id);
    }

    @Override
    public void run() {
        setStatus(OtaFirmwareTaskStatus.STARTED);
        try (ServerSocket socket = new ServerSocket(0)) {
            int port = socket.getLocalPort();
            log.info("Upload port for device {}: {}", device, port);

            checkInterrupted();

            if (!sendInvitationMessage(port)) {
                log.error("Device {} not accepted invitation", device);
                return;
            }

            checkInterrupted();

            sendFirmware(socket);
            setStatus(OtaFirmwareTaskStatus.FINISHED);
        } catch (InterruptedException e) {
            setStatus(OtaFirmwareTaskStatus.ABORTED);
            log.warn("Upload task for {} was aborted", device);
        } catch (Exception e) {
            log.error("Failed to upload firmware to device {}", device, e);
        }
    }

    private void sendFirmware(ServerSocket socket) throws IOException, InterruptedException {
        try (Socket client = socket.accept();) {
            setStatus(OtaFirmwareTaskStatus.FIRMWARE_TRANSFER);
            OutputStream outputStream = client.getOutputStream();
            InputStream inputStream = client.getInputStream();
            byte[] buff = new byte[Integer.BYTES];

            String response = "";
            int total = 0, step = 1, sendLength, respLen;
            while (total < data.length) {
                checkInterrupted();
                sendLength = Math.min(data.length - total, STEP_SIZE);
                outputStream.write(data, total, sendLength);

                respLen = inputStream.read(buff);
                response = new String(buff, 0, respLen);

                total += sendLength;
                if (step % 20 == 0) {
                    setProgress(total * 100 / data.length);
                }
                step++;
            }

            checkInterrupted();

            transferProgress.lazySet(100);
            setStatus(OtaFirmwareTaskStatus.FIRMWARE_TRANSFER_CONFIRMATION);
            log.info("Firmware transmission to {} finished, waiting for confirmation from device", device);

            long start = System.currentTimeMillis();
            while (!StringUtils.equals(response, "OK") && System.currentTimeMillis() - start < CONFIRMATION_TIMEOUT) {
                checkInterrupted();
                respLen = inputStream.read(buff);
                response = new String(buff, 0, respLen);
            }

            checkInterrupted();

            if (!StringUtils.equals(response, "OK")) {
                log.error("Confirmation from {} is missing!", device);
                setStatus(OtaFirmwareTaskStatus.FIRMWARE_TRANSFER_FAILED);
            } else {
                setStatus(OtaFirmwareTaskStatus.FIRMWARE_TRANSFER_FINISHED);
            }
        } catch (Exception e) {
            setStatus(OtaFirmwareTaskStatus.FIRMWARE_TRANSFER_FAILED);
            throw e;
        }
    }

    // looks bad, but it works
    private void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    private boolean sendInvitationMessage(int uploadPort) throws IOException {
        setStatus(OtaFirmwareTaskStatus.INVITATION);
        String invitation =  String.format(
                "%d %d %d %s",
                UPLOAD_COMMAND,
                uploadPort,
                data.length,
                firmwareInfo.getFileChecksum()
        );
        log.info("Sending invitation message [{}] to {}", invitation, device);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);

            InetAddress address = InetAddress.getByName(device.getIp());
            byte[] buff = invitation.getBytes();

            DatagramPacket packet = new DatagramPacket(buff, buff.length, address, invitationPort);
            log.info("Sending invitation message on port={} to {}", invitationPort, device);
            socket.send(packet);

            byte[] buffResponse = new byte[2];
            packet = new DatagramPacket(buffResponse, buffResponse.length);
            socket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength());
            log.info("Device {} response: {}", device, response);
            setStatus(OtaFirmwareTaskStatus.INVITATION_ACCEPTED);
            return response.equals("OK");
        } catch (SocketTimeoutException exception) {
            log.error("Invitation timeout for device {}", device);
            setStatus(OtaFirmwareTaskStatus.INVITATION_FAILED);
            return false;
        }
    }

    private void setStatus(OtaFirmwareTaskStatus status) {
        this.status = status;
        messagingTemplate.convertAndSend(topic, new OtaUploadProgressShort(status, null));
    }

    private void setProgress(int progress) {
        transferProgress.lazySet(progress);
        messagingTemplate.convertAndSend(topic, new OtaUploadProgressShort(status, progress));
    }
}
