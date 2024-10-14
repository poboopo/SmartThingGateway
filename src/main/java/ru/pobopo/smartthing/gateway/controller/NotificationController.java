package ru.pobopo.smartthing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.service.notification.NotificationService;
import ru.pobopo.smartthing.model.DeviceInfo;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;

@RestController
@RequestMapping("/api/notification")
@Tag(name = "Notification controller")
public class NotificationController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Send notification",
            description = "Send notification from device to gateway and connected cloud"
    )
    @PostMapping
    public void sendNotification(@RequestBody GatewayNotification notificationRequest) throws BadRequestException {
        DeviceInfo info = notificationRequest.getDevice();
        if (info == null || StringUtils.isEmpty(info.getIp()) || StringUtils.isEmpty(info.getName())) {
            throw new BadRequestException("Device info is required!");
        }

        if (notificationRequest.getNotification() == null || StringUtils.isEmpty(notificationRequest.getNotification().getMessage())) {
            throw new BadRequestException("Notification caption is required!");
        }

        notificationService.sendNotification(notificationRequest);
    }
}
