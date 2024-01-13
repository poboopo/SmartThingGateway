package ru.pobopo.smart.thing.gateway.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class DeviceInfo {
    private String ip;
    private String type;
    private String name;

    public DeviceInfo(String ip,  String name) {
        this.ip = ip;
        this.name = name;
        this.type = "type_missing";
    }

    public DeviceInfo(String ip, String type, String name) {
        this.ip = ip;
        this.type = type;
        this.name = name;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(ip) || StringUtils.isEmpty(name);
    }

    @Nullable
    public static DeviceInfo fromMulticastMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return null;
        }

        String[] splited = message.split("[$]");
        if (splited.length == 2) { //old version
            return new DeviceInfo(splited[0], splited[1]);
        }
        if (splited.length == 3) {
            return new DeviceInfo(splited[0], splited[1], splited[2]);
        }
        return null;
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
        return StringUtils.equals(comp.getIp(), getIp())
               && StringUtils.equals(comp.getName(), getName())
                && StringUtils.equals(comp.getType(), getType());
    }

    @Override
    public int hashCode() {
        int hashCode = ip.hashCode();
        hashCode = 31 * hashCode + name.hashCode();
        hashCode = 31 * hashCode + type.hashCode();
        return hashCode;
    }
}
