package ru.pobopo.smart.thing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardObservable {
    private String type; //todo enum?
    private String name;
    private String units;
}
