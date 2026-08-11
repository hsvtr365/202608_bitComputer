package com.bitcomputer.portal.controller;

import com.bitcomputer.portal.dto.AuthDtos.AuthUser;
import com.bitcomputer.portal.security.EmployeePrincipal;
import com.bitcomputer.portal.service.EmployeeService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final EmployeeService employees;

    public AuthController(EmployeeService employees) { this.employees = employees; }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUser> me(@AuthenticationPrincipal EmployeePrincipal principal) {
        if (principal == null) return ResponseEntity.noContent().build();
        var employee = employees.getEntity(principal.id());
        return ResponseEntity.ok(new AuthUser(employee.getId(), employee.getEmployeeNumber(), employee.getName(),
                employee.getEmail(), employee.getRole(), employee.getStatus()));
    }
}
