package com.bitcomputer.portal.security;

import com.bitcomputer.portal.domain.EmployeeStatus;
import com.bitcomputer.portal.dto.ApiError;
import com.bitcomputer.portal.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveEmployeeFilter extends OncePerRequestFilter {
    private final EmployeeRepository employees;
    private final ObjectMapper objectMapper;

    public ActiveEmployeeFilter(EmployeeRepository employees, ObjectMapper objectMapper) {
        this.employees = employees;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof EmployeePrincipal principal) {
            var current = employees.findById(principal.id()).orElse(null);
            if (current == null || current.getStatus() != EmployeeStatus.ACTIVE) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(),
                        ApiError.of("ACCOUNT_TERMINATED", "퇴사 처리된 계정은 접근할 수 없습니다."));
                return;
            }
            var fresh = EmployeePrincipal.from(current);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(fresh, null, fresh.getAuthorities()));
        }
        chain.doFilter(request, response);
    }
}
