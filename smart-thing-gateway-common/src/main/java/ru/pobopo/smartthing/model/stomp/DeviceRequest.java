package ru.pobopo.smartthing.model.stomp;

import lombok.*;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class DeviceRequest {
    private String gatewayId;
    private DeviceInfo device;
    private String command;
    private Map<String, Object> params = new HashMap<>();
}
