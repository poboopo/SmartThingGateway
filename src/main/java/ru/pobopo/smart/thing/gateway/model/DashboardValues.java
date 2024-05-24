package ru.pobopo.smart.thing.gateway.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class DashboardValues {
    private Map<String, Integer> sensors = new HashMap<>();
    private Map<String, String> states = new HashMap<>();
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdate;
}
