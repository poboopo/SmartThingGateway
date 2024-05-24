package ru.pobopo.smart.thing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardGroupValues {
    private final DashboardGroup group;
    private final DashboardValues values;
}
