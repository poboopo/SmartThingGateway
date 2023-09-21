package ru.pobopo.smart.thing.gateway.service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smart.thing.gateway.jobs.BackgroundJob;

@Component
@Slf4j
public class BackgroundJobsService {
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public BackgroundJobsService(List<BackgroundJob> jobList) {
        if (jobList == null || jobList.isEmpty()) {
            log.warn("No background jobs were configured!");
            return;
        }

        log.info("Total background jobs: {}", jobList.size());

        this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(jobList.size());
        for (BackgroundJob job: jobList) {
            log.info("Starting job {}", job.getClass());
            threadPoolExecutor.submit(job);
        }
    }
}
