package ru.pobopo.smartthing.model.stomp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BaseMessage {
    private UUID id;
    private MessageType type;
    private boolean needResponse = true;

    public BaseMessage(MessageType type) {
        this.type = type;
    }
}
