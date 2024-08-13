package ru.pobopo.smartthing.gateway.service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.gateway.jobs.BackgroundJob;

@Component
@Slf4j
public class BackgroundJobsService {
    private final ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public BackgroundJobsService(List<BackgroundJob> jobList) {
        this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(jobList.size());
        log.info("Total background jobs: {}", jobList.size());

        for (BackgroundJob job: jobList) {
            log.info("Starting job {}", job.getClass());
            threadPoolExecutor.submit(job);
        }
    }
}
