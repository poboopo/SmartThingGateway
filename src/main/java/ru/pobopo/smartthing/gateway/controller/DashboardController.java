package ru.pobopo.smartthing.gateway.controller;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.gateway.annotation.AcceptCloudRequest;
import ru.pobopo.smartthing.model.gateway.dashboard.*;
import ru.pobopo.smartthing.gateway.service.DashboardService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AcceptCloudRequest
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public Collection<DashboardGroup> getGroups() {
        return dashboardService.getGroups();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DashboardGroup createGroup(@RequestBody DashboardGroup group) throws ValidationException {
        return dashboardService.createGroup(group);
    }

    @PutMapping
    public DashboardGroup updateGroup(@RequestBody DashboardGroup group) throws ValidationException {
        return dashboardService.updateGroup(group);
    }

    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable UUID id) throws ValidationException {
        dashboardService.deleteGroup(id);
    }

    @GetMapping("/values")
    public List<DashboardGroupValues> values() {
        return dashboardService.getValues();
    }

    @GetMapping("/values/{id}")
    public List<DashboardObservableValues> groupValues(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean force
    ) {
        return dashboardService.getGroupValues(id);
    }

    @PutMapping("/values/{id}/update")
    public void updateValues(@PathVariable UUID id) {
        dashboardService.updateValues(id);
    }
}
