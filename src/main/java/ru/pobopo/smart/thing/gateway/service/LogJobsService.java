package ru.pobopo.smart.thing.gateway.service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.logs.LogsListener;

@Component
@Slf4j
public class LogJobsService {
    public static String DEVICES_LOGS_TOPIC = "/devices/logs";

    private final List<LogsListener> logsListenerList;
    private final ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public LogJobsService(List<LogsListener> logsListenerList) {
        this.logsListenerList = logsListenerList;
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(logsListenerList.size());
    }

    public void start() {
        logsListenerList.forEach(threadPoolExecutor::execute);
    }
}
