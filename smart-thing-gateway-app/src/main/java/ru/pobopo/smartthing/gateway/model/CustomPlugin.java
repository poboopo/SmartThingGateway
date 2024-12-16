package ru.pobopo.smartthing.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CustomPlugin {
    private final String name;
    private final List<Class<?>> classes;

    @Override
    public String toString() {
        return String.format(
                "CustomPlugin{name=%s, classes count=%d}",
                name,
                classes.size()
        );
    }
}
