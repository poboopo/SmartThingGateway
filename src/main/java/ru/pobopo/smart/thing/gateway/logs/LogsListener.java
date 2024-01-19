package ru.pobopo.smart.thing.gateway.logs;

public interface LogsListener extends Runnable {
    void listen() throws Exception;
}
