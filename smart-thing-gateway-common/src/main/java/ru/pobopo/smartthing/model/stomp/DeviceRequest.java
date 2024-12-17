package ru.pobopo.smartthing.model.stomp;

import lombok.*;

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
    private String device;
    private String command;
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
}
