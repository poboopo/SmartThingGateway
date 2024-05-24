package ru.pobopo.smart.thing.gateway.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "units")
public class DashboardObservable {
    private String type; //todo enum?
    private String name;
    private String units;
}
