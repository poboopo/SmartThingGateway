package ru.pobopo.smart.thing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardGroup {
    private UUID id;
    private DeviceInfo device;
    private List<DashboardObservable> observables;
    // todo update interval
}
