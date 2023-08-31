package ru.pobopo.smart.thing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DeviceInfo {
    private String ip;
    private String name;

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
}
