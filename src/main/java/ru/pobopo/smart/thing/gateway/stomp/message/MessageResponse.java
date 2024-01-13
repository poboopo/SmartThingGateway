package ru.pobopo.smart.thing.gateway.stomp.message;

import lombok.Data;

@Data
public class MessageResponse {
    private String requestId;
    private Object response;
    private String error;
    private boolean success = true;
}
