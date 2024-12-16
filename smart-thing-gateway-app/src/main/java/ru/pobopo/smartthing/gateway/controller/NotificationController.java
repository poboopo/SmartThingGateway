package ru.pobopo.smartthing.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.gateway.aspect.AcceptCloudRequest;
import ru.pobopo.smartthing.gateway.exception.BadRequestException;
import ru.pobopo.smartthing.gateway.service.notification.NotificationService;
import ru.pobopo.smartthing.model.stomp.DeviceNotification;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping("/api/notification")
@Tag(name = "Notification controller")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(
            summary = "Send notification",
            description = "Send notification from device to gateway and connected cloud"
    )
    @PostMapping
    public UUID sendNotification(@RequestBody DeviceNotification notificationRequest) throws BadRequestException {
        return notificationService.sendNotification(notificationRequest);
    }

    @Operation(
            summary = "Get all user notifications"
    )
    @AcceptCloudRequest
    @GetMapping
    public Collection<DeviceNotification> getNotification() {
        return notificationService.getNotifications();
    }

    @Operation(
            summary = "Delete notification"
    )
    @AcceptCloudRequest
    @DeleteMapping
    public boolean markSeen(@RequestParam UUID id) {
        return notificationService.markSeen(id);
    }
}
