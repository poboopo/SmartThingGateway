package ru.pobopo.smartthing.model.stomp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class GatewayRequestMessage extends BaseMessage {
    private String url;
    private String method;
    private Object data;

    public GatewayRequestMessage() {
        super(MessageType.GATEWAY_REQUEST);
    }

    public GatewayRequestMessage(String path, String method, Object data) {
        super(MessageType.GATEWAY_REQUEST);
    }
}
