package com.bitcomputer.portal.controller;

import com.bitcomputer.portal.dto.EmployeeDtos.*;
import com.bitcomputer.portal.security.EmployeePrincipal;
import com.bitcomputer.portal.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {
    private final EmployeeService employees;

    public AdminEmployeeController(EmployeeService employees) { this.employees = employees; }

    @GetMapping
    public List<EmployeeResponse> list(@RequestParam(required = false) String q) { return employees.list(q); }

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
