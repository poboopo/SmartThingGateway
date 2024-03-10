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
import ru.pobopo.smart.thing.gateway.service.MessageBrokerService;

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
            CloudService cloudService,
            MessageBrokerService brokerService,
            LogJobsService logsJobs
    )  {
        return args -> {
            logsJobs.start();
            try {
                cloudService.login();
            } catch (Throwable exception) {
                log.error("Failed to login in cloud: {}", exception.getMessage());
            }
            try {
                brokerService.connect();
            } catch (Throwable exception) {
                log.error("Failed to connect to cloud: {}", exception.getMessage());
            }
        };
    }
}
