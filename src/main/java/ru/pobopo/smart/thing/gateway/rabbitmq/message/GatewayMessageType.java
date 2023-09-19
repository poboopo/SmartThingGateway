package ru.pobopo.smart.thing.gateway.rabbitmq.message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

public enum GatewayMessageType {
    DEVICE_REQUEST("device_request"),
    GATEWAY_COMMAND("gateway_command");

    private String type;
    GatewayMessageType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
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
