package com.bitcomputer.portal.security;

import com.bitcomputer.portal.domain.Employee;
import com.bitcomputer.portal.domain.EmployeeStatus;
import com.bitcomputer.portal.domain.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record EmployeePrincipal(Long id, String email, String passwordHash, String name, Role role,
                                EmployeeStatus status) implements UserDetails {
    public static EmployeePrincipal from(Employee employee) {
        return new EmployeePrincipal(employee.getId(), employee.getEmail(), employee.getPasswordHash(),
                employee.getName(), employee.getRole(), employee.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return status == EmployeeStatus.ACTIVE; }
}
