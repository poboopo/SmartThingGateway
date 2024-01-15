package ru.pobopo.smart.thing.gateway.device.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Observable {
    private String type;
    private String name;
}
