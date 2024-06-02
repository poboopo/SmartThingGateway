package ru.pobopo.smartthing.gateway.device.api;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.pobopo.smartthing.gateway.device.api.model.Observable;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.InternalHttpResponse;

@Component
public class DeviceApiVerSeven extends DeviceApiVerSix {
    public final static String HOOK_TEST = HOOKS + "/test";

    @Autowired
    public DeviceApiVerSeven(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public boolean accept(DeviceInfo deviceInfo) {
        if (StringUtils.isBlank(deviceInfo.getIp())) {
            return false;
        }
        return StringUtils.equals(deviceInfo.getVersion(), "0.7");
    }

    public InternalHttpResponse testHook(DeviceInfo info, Observable observable, String id, String value) {
        return this.sendRequest(
                info,
                String.format(
                        "%s?type=%s&name=%s&id=%s&value=%s",
                        HOOK_TEST,
                        observable.getType(),
                        observable.getName(),
                        id,
                        value == null ? "" : value
                )
        );
    }
}
