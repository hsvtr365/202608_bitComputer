package com.bitcomputer.portal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bitcomputer.portal.domain.Employee;
import com.bitcomputer.portal.domain.EmployeeStatus;
import com.bitcomputer.portal.domain.Role;
import com.bitcomputer.portal.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        employees.deleteAll();
    }

    @Test
    void employeeCannotAccessAdminApi() throws Exception {
        create("EMP-001", "employee@test.com", Role.EMPLOYEE, EmployeeStatus.ACTIVE);
        var session = login("employee@test.com");
        mvc.perform(get("/api/admin/employees").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isForbidden());
    }

    @Test
    void terminatedEmployeeCannotLogin() throws Exception {
        create("EMP-002", "terminated@test.com", Role.EMPLOYEE, EmployeeStatus.TERMINATED);
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", "terminated@test.com").param("password", "Password1!"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void existingSessionIsBlockedAfterTermination() throws Exception {
        var employee = create("EMP-003", "active@test.com", Role.EMPLOYEE, EmployeeStatus.ACTIVE);
        var session = login("active@test.com");
        mvc.perform(get("/api/me").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());

        employee.setStatus(EmployeeStatus.TERMINATED);
        employees.saveAndFlush(employee);

        mvc.perform(get("/api/me").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isForbidden());
    }

    private HttpSession login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email).param("password", "Password1!"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private Employee create(String number, String email, Role role, EmployeeStatus status) {
        var employee = new Employee();
        employee.setEmployeeNumber(number);
        employee.setName("김민준");
        employee.setEmail(email);
        employee.setPasswordHash(passwordEncoder.encode("Password1!"));
        employee.setPhone("010-0000-0000");
        employee.setDateOfBirth(LocalDate.of(1990, 1, 1));
        employee.setDepartment("개발");
        employee.setPosition("사원");
        employee.setRole(role);
        employee.setStatus(status);
        employee.setHireDate(LocalDate.of(2024, 1, 1));
        return employees.saveAndFlush(employee);
    }
}
