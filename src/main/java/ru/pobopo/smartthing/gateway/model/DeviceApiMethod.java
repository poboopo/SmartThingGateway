package ru.pobopo.smartthing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.model.DeviceInfo;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceApiMethod {
    private String name;
    private List<DeviceApiMethodParam> params;

    public static DeviceApiMethod fromMethod(Method method) {
        return builder()
                .name(method.getName())
                .params(Arrays.stream(method.getParameters())
                        .filter((parameter -> !parameter.getType().equals(DeviceInfo.class)))
                        .map((param) -> new DeviceApiMethodParam(param.getName(), param.getType().getSimpleName()))
                        .toList()
                ).build();
    }
}
