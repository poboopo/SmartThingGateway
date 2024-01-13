package ru.pobopo.smart.thing.gateway.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.model.Notification;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    private DeviceInfo device;
    private Notification notification;

    public SendNotificationRequest(Notification notification) {
        this.notification = notification;
    }
}
