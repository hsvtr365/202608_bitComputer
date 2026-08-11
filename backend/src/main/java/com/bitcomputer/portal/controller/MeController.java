package com.bitcomputer.portal.controller;

import com.bitcomputer.portal.dto.EmployeeDtos.EmployeeResponse;
import com.bitcomputer.portal.dto.EmployeeDtos.UpdateMeRequest;
import com.bitcomputer.portal.security.EmployeePrincipal;
import com.bitcomputer.portal.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final EmployeeService employees;

    public MeController(EmployeeService employees) { this.employees = employees; }

    @GetMapping
    public EmployeeResponse me(@AuthenticationPrincipal EmployeePrincipal principal) {
        return employees.get(principal.id());
    }

    @PatchMapping
    public EmployeeResponse update(@AuthenticationPrincipal EmployeePrincipal principal,
                                   @Valid @RequestBody UpdateMeRequest request) {
        return employees.updateMe(principal.id(), request);
    }
}
