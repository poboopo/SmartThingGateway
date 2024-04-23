package ru.pobopo.smart.thing.gateway.controller;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smart.thing.gateway.exception.DashboardFileException;
import ru.pobopo.smart.thing.gateway.model.DashboardGroup;
import ru.pobopo.smart.thing.gateway.model.DashboardObservable;
import ru.pobopo.smart.thing.gateway.service.DashboardService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public List<DashboardGroup> getGroups() throws IOException, DashboardFileException {
        return dashboardService.getGroups();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DashboardGroup createGroup(@RequestBody DashboardGroup group) throws ValidationException, IOException, DashboardFileException {
        return dashboardService.createGroup(group);
    }

    @PutMapping("/{id}")
    public void updateGroupObservables(@PathVariable UUID id, @RequestBody List<DashboardObservable> observables) throws ValidationException, IOException, DashboardFileException {
        dashboardService.updateGroupObservables(id, observables);
    }

    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable UUID id) throws ValidationException, IOException, DashboardFileException {
        dashboardService.deleteGroup(id);
    }
}
