package ru.pobopo.smartthing.gateway.logs;

public interface LogsListener extends Runnable {
    void listen() throws Exception;
}
