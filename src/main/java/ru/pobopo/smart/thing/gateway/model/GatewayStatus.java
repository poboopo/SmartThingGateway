package ru.pobopo.smart.thing.gateway.model;

import lombok.Data;

@Data
public class GatewayStatus {
    private boolean gatewayInfoLoaded = false;
    private boolean connectedToBroker =  false;
    private boolean subscribedToQueue = false;

    public void connectionClosed() {
        gatewayInfoLoaded = false;
        connectedToBroker = false;
        subscribedToQueue = false;
    }
}
