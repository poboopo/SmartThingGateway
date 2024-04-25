package ru.pobopo.smart.thing.gateway.stomp.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse {
    private String requestId;
    private Object response;
    private String error;
    private String stack;
    private boolean success = true;
}
