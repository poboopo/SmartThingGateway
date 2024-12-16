package ru.pobopo.smartthing.model.stomp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.pobopo.smartthing.model.GatewayInfo;

@EqualsAndHashCode(callSuper = true)
@Data
public class GatewayNotification extends DeviceNotification {
    private GatewayInfo gateway;

    public GatewayNotification() {
    }

    public GatewayNotification(DeviceNotification deviceNotification, GatewayInfo gateway) {
        super(deviceNotification.getId(), deviceNotification.getDevice(), deviceNotification.getNotification(), deviceNotification.getDateTime());
        this.gateway = gateway;
    }
}
