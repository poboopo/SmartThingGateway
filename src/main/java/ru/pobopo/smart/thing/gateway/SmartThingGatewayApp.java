package ru.pobopo.smart.thing.gateway;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.pobopo.smart.thing.gateway.service.LogJobsService;
import ru.pobopo.smart.thing.gateway.service.CloudService;
import ru.pobopo.smart.thing.gateway.service.ConfigurationService;

@Slf4j
@SpringBootApplication
public class SmartThingGatewayApp {
    public static void main(String[] args) {
        SpringApplication.run(SmartThingGatewayApp.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    CommandLineRunner run(
            ConfigurationService configurationService,
            CloudService cloudService,
            LogJobsService logsJobs
    )  {
        return args -> {
            logsJobs.start();
            configurationService.loadConfiguration();
            try {
                cloudService.authorize();
            } catch (Throwable exception) {
                log.error("Failed to authorize", exception);
            }
        };
    }
}
