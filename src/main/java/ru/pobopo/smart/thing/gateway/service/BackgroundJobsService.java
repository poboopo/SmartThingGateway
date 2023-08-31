package ru.pobopo.smart.thing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.jobs.DeviceLogsJob;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;

@Component
@Slf4j
public class BackgroundJobsService {
    private final Thread searchThread;
    private final Thread logsThread;

    @Autowired
    public BackgroundJobsService(DeviceSearchJob searchService, DeviceLogsJob logsService) {
        searchThread = new Thread(searchService);
        logsThread = new Thread(logsService);

        searchThread.setDaemon(true);
        logsThread.setDaemon(true);

        searchThread.start();
        logsThread.start();
    }
}
