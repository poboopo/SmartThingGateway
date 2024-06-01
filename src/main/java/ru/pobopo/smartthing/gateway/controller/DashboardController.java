package ru.pobopo.smartthing.gateway.controller;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.gateway.exception.DashboardFileException;
import ru.pobopo.smartthing.gateway.model.DashboardGroup;
import ru.pobopo.smartthing.gateway.model.DashboardGroupValues;
import ru.pobopo.smartthing.gateway.model.DashboardValues;
import ru.pobopo.smartthing.gateway.service.DashboardService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public List<DashboardGroup> getGroups() {
        return dashboardService.getGroups();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DashboardGroup createGroup(@RequestBody DashboardGroup group) throws ValidationException, IOException {
        return dashboardService.createGroup(group);
    }

    @PutMapping
    public void updateGroup(@RequestBody DashboardGroup group) throws ValidationException, IOException {
        dashboardService.updateGroup(group);
    }

    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable UUID id) throws ValidationException, IOException {
        dashboardService.deleteGroup(id);
    }

    @GetMapping("/values")
    public List<DashboardGroupValues> values() {
        return dashboardService.getValues();
    }

    @GetMapping("/values/{id}")
    public DashboardValues groupValues(@PathVariable UUID id) {
        return dashboardService.getGroupValues(id);
    }

    @PutMapping("/values/{id}")
    public DashboardValues groupValuesUpdate(@PathVariable UUID id) {
        return dashboardService.updateGroupValues(id);
    }
}
