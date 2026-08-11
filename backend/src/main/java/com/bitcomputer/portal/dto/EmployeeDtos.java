package com.bitcomputer.portal.dto;

import com.bitcomputer.portal.domain.Employee;
import com.bitcomputer.portal.domain.EmployeeStatus;
import com.bitcomputer.portal.domain.Role;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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

    public record EmployeePage(List<EmployeeResponse> employees, int page, int size,
                               long totalCount, int totalPages) {}

    public record CreateEmployeeRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_-]+") String employeeNumber,
            @NotBlank @Size(max = 100) @Pattern(regexp = "[가-힣]{2,100}") String name,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 30) @Pattern(regexp = "[0-9+() -]*") String phone,
            @NotNull @Past LocalDate dateOfBirth,
            @NotBlank @Size(max = 100) String department,
            @NotBlank @Size(max = 100) String position,
            @NotNull Role role,
            @NotNull @PastOrPresent LocalDate hireDate) {}

    public record UpdateEmployeeRequest(
            @Size(min = 2, max = 100) @Pattern(regexp = "[가-힣]{2,100}") String name,
            @Email @Size(min = 3, max = 200) String email,
            @Size(max = 30) @Pattern(regexp = "[0-9+() -]*") String phone,
            @Past LocalDate dateOfBirth,
            @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*") String department,
            @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*") String position,
            Role role,
            @PastOrPresent LocalDate hireDate) {}

    public record UpdateMeRequest(
            @Size(min = 2, max = 100) @Pattern(regexp = "[가-힣]{2,100}") String name,
            @Email @Size(min = 3, max = 200) String email,
            @Size(max = 30) @Pattern(regexp = "[0-9+() -]*") String phone) {}
}
