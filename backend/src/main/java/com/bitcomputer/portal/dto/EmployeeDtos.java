package com.bitcomputer.portal.dto;

import com.bitcomputer.portal.domain.Employee;
import com.bitcomputer.portal.domain.EmployeeStatus;
import com.bitcomputer.portal.domain.Role;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;

public final class EmployeeDtos {
    private EmployeeDtos() {}

    public record EmployeeResponse(
            Long id,
            String employeeNumber,
            String name,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String department,
            String position,
            Role role,
            EmployeeStatus status,
            LocalDate hireDate,
            Instant terminationDate,
            Instant createdAt,
            Instant updatedAt) {
        public static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(e.getId(), e.getEmployeeNumber(), e.getName(), e.getEmail(),
                    e.getPhone(), e.getDateOfBirth(), e.getDepartment(), e.getPosition(), e.getRole(),
                    e.getStatus(), e.getHireDate(), e.getTerminationDate(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public record CreateEmployeeRequest(
            @NotBlank @Size(max = 40) String employeeNumber,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 30) String phone,
            @NotNull @Past LocalDate dateOfBirth,
            @NotBlank @Size(max = 100) String department,
            @NotBlank @Size(max = 100) String position,
            @NotNull Role role,
            @NotNull @PastOrPresent LocalDate hireDate) {}

    public record UpdateEmployeeRequest(
            @Size(min = 1, max = 100) String name,
            @Email @Size(max = 200) String email,
            @Size(max = 30) String phone,
            @Past LocalDate dateOfBirth,
            @Size(min = 1, max = 100) String department,
            @Size(min = 1, max = 100) String position,
            Role role,
            @PastOrPresent LocalDate hireDate) {}

    public record UpdateMeRequest(@Size(max = 30) String phone) {}
}
