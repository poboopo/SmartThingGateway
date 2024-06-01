package ru.pobopo.smartthing.gateway.service;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.logs.LogsListener;
import ru.pobopo.smartthing.gateway.model.DeviceLoggerMessage;

@Component
@Slf4j
public class LogJobsService {
    private final List<LogsListener> logsListenerList;
    private final ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public LogJobsService(List<LogsListener> logsListenerList) {
        this.logsListenerList = logsListenerList;
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(logsListenerList.size());
    }

    @PostConstruct
    public void start() {
        log.info("Starting devices logs listeners");
        logsListenerList.forEach(threadPoolExecutor::execute);
    }
}
