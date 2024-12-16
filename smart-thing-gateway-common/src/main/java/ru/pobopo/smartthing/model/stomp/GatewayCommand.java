package ru.pobopo.smartthing.model.stomp;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum GatewayCommand {
    LOGOUT("logout"),
    PING("ping");

    private final String name;

    GatewayCommand(String name) {
        this.name = name;
    }

    public GatewayCommand fromValue(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }

        for (GatewayCommand commandType : values()) {
            if (StringUtils.equals(commandType.getName(), type)) {
                return commandType;
            }
        }

        return null;
    }
}
