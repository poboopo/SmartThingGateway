package ru.pobopo.smart.thing.gateway.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smart.thing.gateway.controller.model.SendNotificationRequest;
import ru.pobopo.smart.thing.gateway.exception.BadRequestException;
import ru.pobopo.smart.thing.gateway.service.NotificationService;

@RestController
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public void sendNotification(@RequestBody SendNotificationRequest notificationRequest) throws BadRequestException {
        if (notificationRequest.getDevice() == null || notificationRequest.getDevice().isEmpty()) {
            throw new BadRequestException("Device info is required!");
        }

        if (notificationRequest.getNotification() == null || StringUtils.isEmpty(notificationRequest.getNotification().getMessage())) {
            throw new BadRequestException("Notification caption is required!");
        }

        notificationService.sendNotification(notificationRequest);
    }
}
