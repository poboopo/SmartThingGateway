package ru.pobopo.smart.thing.gateway.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.model.Notification;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Send notification request from device")
public class SendNotificationRequest {
    @Schema(description = "Sender device")
    private DeviceInfo device;
    @Schema(description = "Notification information")
    private Notification notification;

    public SendNotificationRequest(Notification notification) {
        this.notification = notification;
    }
}
