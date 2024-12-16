package ru.pobopo.smartthing.model.stomp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.pobopo.smartthing.model.GatewayInfo;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayEvent {
    private GatewayInfo gateway;
    private GatewayEventType event;
}
