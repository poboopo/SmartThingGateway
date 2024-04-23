package ru.pobopo.smart.thing.gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Device information")
public class DeviceInfo {
    @Schema(description = "Ip in local network")
    private String ip;
    @Schema(description = "Device type", example = "lamp")
    private String type;
    @Schema(description = "Device name")
    private String name;
    @Schema(description = "Firmware version")
    private String version;

    public DeviceInfo(String ip,  String name) {
        this.ip = ip;
        this.name = name;
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
               && StringUtils.equals(comp.getName(), getName());
        // do i need this check?
//                && StringUtils.equals(comp.getType(), getType());
    }

    @Override
    public int hashCode() {
        int hashCode = ip.hashCode();
        hashCode = 31 * hashCode + name.hashCode();
//        hashCode = 31 * hashCode + type.hashCode();
        return hashCode;
    }
}
