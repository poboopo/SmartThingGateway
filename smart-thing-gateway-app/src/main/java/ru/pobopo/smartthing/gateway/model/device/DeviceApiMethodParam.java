package ru.pobopo.smartthing.gateway.model.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceApiMethodParam {
    private String name;
    private String className;
}
