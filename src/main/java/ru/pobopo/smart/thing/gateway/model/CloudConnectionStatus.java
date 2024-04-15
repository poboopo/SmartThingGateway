package ru.pobopo.smart.thing.gateway.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "WebSocket cloud connection status")
public enum CloudConnectionStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    CONNECTION_LOST,
    DISCONNECTED,
    FAILED_TO_CONNECT,
    RECONNECTING,
}
