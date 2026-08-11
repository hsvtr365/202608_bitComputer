package com.bitcomputer.portal.controller;

import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckDtos.*;
import com.bitcomputer.portal.service.BackgroundCheckService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminBackgroundCheckController {
    private final BackgroundCheckService checks;

    public AdminBackgroundCheckController(BackgroundCheckService checks) { this.checks = checks; }

    @GetMapping("/employees/{employeeId}/background-checks/name-parts")
    public NameParts nameParts(@PathVariable Long employeeId) { return checks.nameParts(employeeId); }

    @PostMapping("/employees/{employeeId}/background-checks")
    @ResponseStatus(HttpStatus.CREATED)
    public Created create(@PathVariable Long employeeId,
                          @Valid @RequestBody(required = false) CreateOverrideRequest request) {
        return checks.create(employeeId, request);
    }

    @GetMapping("/employees/{employeeId}/background-checks")
    public HistoryPage history(@PathVariable Long employeeId,
                               @RequestParam(defaultValue = "0") @Min(0) int page,
                               @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return checks.history(employeeId, page, size);
    }

    @GetMapping("/background-checks/{checkId}")
    public Result get(@PathVariable String checkId) { return checks.get(checkId); }
}
