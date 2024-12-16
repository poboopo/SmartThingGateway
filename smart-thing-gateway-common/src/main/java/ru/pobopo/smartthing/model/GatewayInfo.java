package ru.pobopo.smartthing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GatewayInfo {
    private String id;
    private String name;
    private String description;
}
