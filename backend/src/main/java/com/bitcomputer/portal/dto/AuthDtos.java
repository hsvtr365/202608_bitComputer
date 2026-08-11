package com.bitcomputer.portal.dto;

import com.bitcomputer.portal.domain.EmployeeStatus;
import com.bitcomputer.portal.domain.Role;

public final class AuthDtos {
    private AuthDtos() {}

    public record AuthUser(Long id, String employeeNumber, String name, String email, Role role,
                           EmployeeStatus status) {}
}
