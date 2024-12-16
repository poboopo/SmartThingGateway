package ru.pobopo.smartthing.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum NotificationType {
    WARNING("warning"),
    ERROR("error"),
    INFO("info"),
    SUCCESS("success");

    private final String name;

    @JsonValue
    public String getName() {
        return name;
    }
}
