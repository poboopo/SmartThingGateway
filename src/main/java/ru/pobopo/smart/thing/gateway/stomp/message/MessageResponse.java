package ru.pobopo.smart.thing.gateway.stomp.message;

import lombok.Data;

@Data
public class MessageResponse {
    private String requestId;
    private String response;
    private boolean success = true;
}
