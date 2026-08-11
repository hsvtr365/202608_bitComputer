package com.bitcomputer.portal.controller;

import com.bitcomputer.portal.dto.EmployeeDtos.*;
import com.bitcomputer.portal.security.EmployeePrincipal;
import com.bitcomputer.portal.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {
    private final EmployeeService employees;

    public AdminEmployeeController(EmployeeService employees) { this.employees = employees; }

    @GetMapping
    public EmployeePage list(@RequestParam(required = false) @Size(max = 100)
                             @Pattern(regexp = "[A-Za-z0-9가-힣 _-]*") String q,
                             @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
                             @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1)
                             @jakarta.validation.constraints.Max(50) int size) {
        return employees.list(q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employees.create(request);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) { return employees.get(id); }

    @PatchMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return employees.update(id, request);
    }

    @PostMapping("/{id}/terminate")
    public EmployeeResponse terminate(@PathVariable Long id,
                                      @AuthenticationPrincipal EmployeePrincipal principal) {
        return employees.terminate(id, principal.id());
    }
}
