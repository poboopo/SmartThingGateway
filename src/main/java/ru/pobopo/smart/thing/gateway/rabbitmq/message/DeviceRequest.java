package ru.pobopo.smart.thing.gateway.rabbitmq.message;

import java.util.Map;
import lombok.Data;

@Data
public class DeviceRequest extends BaseMessage {
    private String ip;
    private String method;
    private String payload;
    private Map<String, String> headers;
}
