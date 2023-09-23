package ru.pobopo.smart.thing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

@Getter
@Setter
@ToString
public class DeviceInfo {
    private String ip;
    private String name;
    private DeviceFullInfo fullInfo;

    public DeviceInfo(String ip, String name) {
        this.ip = ip;
        this.name = name;
    }

    @Nullable
    public static DeviceInfo fromMulticastMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return null;
        }
        String[] splited = message.split("[$]");
        if (splited.length != 2) {
            return null;
        }
        return new DeviceInfo(splited[0], splited[1]);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        DeviceInfo comp = (DeviceInfo) obj;
        return StringUtils.equals(comp.getIp(), getIp()) && StringUtils.equals(comp.getName(), getName());
    }

    @Override
    public int hashCode() {
        int hashCode = ip.hashCode();
        hashCode = 31 * hashCode + name.hashCode();
        return hashCode;
    }
}
