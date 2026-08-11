package com.bitcomputer.portal.security;

import com.bitcomputer.portal.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {
    private final EmployeeRepository employees;

    public EmployeeUserDetailsService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return employees.findByEmailIgnoreCase(email)
                .map(EmployeePrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
