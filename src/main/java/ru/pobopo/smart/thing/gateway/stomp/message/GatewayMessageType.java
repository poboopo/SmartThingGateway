package ru.pobopo.smart.thing.gateway.stomp.message;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

@Getter
public enum GatewayMessageType {
    DEVICE_REQUEST("device_request"),
    GATEWAY_COMMAND("gateway_command");

    private String type;
    GatewayMessageType(String type) {
        this.type = type;
    }

    @Nullable
    public static GatewayMessageType fromValue(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }

        for (GatewayMessageType gatewayMessageType : values()) {
            if (StringUtils.equals(gatewayMessageType.getType(), type)) {
                return gatewayMessageType;
            }
        }

        return null;
    }
}
