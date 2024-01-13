package ru.pobopo.smart.thing.gateway.stomp.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smart.thing.gateway.model.DeviceRequest;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRequestMessage extends BaseMessage {
    private DeviceRequest request;
}
