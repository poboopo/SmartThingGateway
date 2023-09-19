package ru.pobopo.smart.thing.gateway.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.jobs.CommandsConsumer;
import ru.pobopo.smart.thing.gateway.jobs.DeviceLogsJob;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;

@Component
@Slf4j
public class BackgroundJobsService {
    private final ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public BackgroundJobsService(DeviceSearchJob searchService, DeviceLogsJob logsService, CommandsConsumer commandsConsumer) {
        this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

        threadPoolExecutor.submit(searchService);
//        threadPoolExecutor.submit(logsService);
        threadPoolExecutor.submit(commandsConsumer);
    }
}
