package ru.pobopo.smart.thing.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pobopo.smart.thing.gateway.jobs.DeviceLogsJob;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;
import ru.pobopo.smart.thing.gateway.jobs.BackgroundJob;

@Configuration
public class BackgroundJobsConfig {

    @Bean
    public List<BackgroundJob> jobs(
        DeviceSearchJob searchService,
        DeviceLogsJob logsService
    ) {
        return List.of(searchService, logsService);
    }
}
