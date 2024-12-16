package ru.pobopo.smartthing.model.stomp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString(callSuper = true)
public class GatewayCommandMessage extends BaseMessage {
    private GatewayCommand command;
    private Map<String, Object> parameters;

    public GatewayCommandMessage() {
        super(MessageType.GATEWAY_COMMAND);
    }

    public GatewayCommandMessage(GatewayCommand command, Map<String, Object> parameters) {
        super(MessageType.GATEWAY_COMMAND);
        this.command = command;
        this.parameters = parameters;
    }
}
