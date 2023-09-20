package ru.pobopo.smart.thing.gateway.rabbitmq.message;

import java.util.Map;
import lombok.Data;

@Data
public class DeviceRequestMessage extends BaseMessage {
    private String target;
    private String path;
    private String method;
    private String payload;
    private Map<String, String> headers;
}
