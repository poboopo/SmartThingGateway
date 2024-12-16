package ru.pobopo.smartthing.gateway.model.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.event.Level;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceLogsFilter {
    private String device;
    private String tag;
    private String message;
    private Level level;

    public boolean isEmpty() {
        return level == null && StringUtils.firstNonBlank(device, tag, message) == null;
    }
}
