package ru.pobopo.smart.thing.gateway.rabbitmq.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.pobopo.smart.thing.gateway.exception.LogoutException;
import ru.pobopo.smart.thing.gateway.exception.MissingValueException;
import ru.pobopo.smart.thing.gateway.jobs.DeviceSearchJob;
import ru.pobopo.smart.thing.gateway.model.DeviceFullInfo;
import ru.pobopo.smart.thing.gateway.model.DeviceInfo;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.GatewayCommand;
import ru.pobopo.smart.thing.gateway.rabbitmq.message.MessageResponse;
import ru.pobopo.smart.thing.gateway.service.DeviceService;

@Slf4j
public class GatewayCommandProcessor implements MessageProcessor {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DeviceSearchJob searchJob;
    private final DeviceService deviceService;

    public GatewayCommandProcessor(DeviceSearchJob searchJob, DeviceService deviceService) {
        this.searchJob = searchJob;
        this.deviceService = deviceService;
    }

    @Override
    public MessageResponse process(String message) throws Exception {
        GatewayCommand gatewayCommand = objectMapper.readValue(message, GatewayCommand.class);
        if (StringUtils.isBlank(gatewayCommand.getRequestId())) {
            throw new MissingValueException("Request id is missing!");
        }

        MessageResponse response = new MessageResponse();
        response.setRequestId(gatewayCommand.getRequestId());

        if (StringUtils.isBlank(gatewayCommand.getCommand())) {
            response.setSuccess(false);
            response.setResponse("Command is missing!");
            return response;
        }

        switch (gatewayCommand.getCommand()) {
            case "ping" -> response.setResponse("pong");
            case "search" -> response.setResponse(searchDevices());
            case "logout" -> {
                log.info("Logout event!");
                throw new LogoutException();
            }
            default -> {
                response.setResponse("Unknown command");
                response.setSuccess(false);
            }
        }

        return response;
    }

    private String searchDevices() throws JsonProcessingException, InterruptedException {
        Set<DeviceInfo> infoSet = searchJob.getRecentFoundDevices();
        if (infoSet.isEmpty()) {
            return "[]";
        }
        int size = infoSet.size();
        CountDownLatch latch = new CountDownLatch(size);
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        infoSet.forEach(info -> {
            executorService.submit(() -> {
                loadDeviceFullInfo(info);
                latch.countDown();
            });
        });
        latch.await();
        return objectMapper.writeValueAsString(infoSet);
    }

    private void loadDeviceFullInfo(DeviceInfo info) {
        try {
            info.setFullInfo(deviceService.getDeviceFullInfo(info));
        } catch (Exception exception) {
            log.error("Failed to load {} full info: {}", info, exception.getMessage());
        }
    }
}
