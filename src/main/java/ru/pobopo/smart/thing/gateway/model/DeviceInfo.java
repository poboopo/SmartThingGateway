package ru.pobopo.smart.thing.gateway.model;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceInfo {
    private String ip;
    private String type;
    private String name;
    private String version;

    public DeviceInfo(String ip,  String name) {
        this.ip = ip;
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
        if (splited.length != 4) {
            return null;
        }

        DeviceInfo.DeviceInfoBuilder builder = builder();
        builder.ip(splited[0])
                .type(splited[1])
                .name(splited[2])
                .version(splited[3]);
        return builder.build();
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
