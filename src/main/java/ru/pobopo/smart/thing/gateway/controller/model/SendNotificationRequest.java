package ru.pobopo.smart.thing.gateway.controller.model;

import lombok.Data;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.model.Notification;

@Data
public class SendNotificationRequest {
    private DeviceInfo device;
    private Notification notification;
}
