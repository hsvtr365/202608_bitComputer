package com.bitcomputer.portal.service;

import com.bitcomputer.portal.domain.Employee;
import com.bitcomputer.portal.domain.EmployeeStatus;
import com.bitcomputer.portal.domain.Role;
import com.bitcomputer.portal.domain.OrganizationCodeType;
import com.bitcomputer.portal.dto.EmployeeDtos.*;
import com.bitcomputer.portal.exception.AppException;
import com.bitcomputer.portal.repository.EmployeeRepository;
import com.bitcomputer.portal.repository.OrganizationCodeRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private final EmployeeRepository employees;
    private final OrganizationCodeRepository organizationCodes;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employees, OrganizationCodeRepository organizationCodes,
                           PasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.organizationCodes = organizationCodes;
        this.passwordEncoder = passwordEncoder;
    }

    public Employee getEntity(Long id) {
        return employees.findById(id).orElseThrow(() ->
                new AppException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "직원을 찾을 수 없습니다."));
    }

    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(getEntity(id));
    }

    public List<EmployeeResponse> list(String query) {
        var rows = StringUtils.hasText(query)
                ? employees.findByNameContainingIgnoreCaseOrEmployeeNumberContainingIgnoreCaseOrderByEmployeeNumberAsc(
                        query.trim(), query.trim())
                : employees.findAllByOrderByEmployeeNumberAsc();
        return rows.stream().map(EmployeeResponse::from).toList();
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        var email = normalizeEmail(request.email());
        if (employees.existsByEmployeeNumber(request.employeeNumber().trim())) {
            throw new AppException(HttpStatus.CONFLICT, "DUPLICATE_EMPLOYEE_NUMBER", "이미 사용 중인 사번입니다.");
        }
        if (employees.existsByEmailIgnoreCase(email)) {
            throw new AppException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
        }
        var department = validateCode(OrganizationCodeType.DEPARTMENT, request.department(), "부서");
        var position = validateCode(OrganizationCodeType.POSITION, request.position(), "직급");
        var employee = new Employee();
        employee.setEmployeeNumber(request.employeeNumber().trim());
        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.phone() != null) employee.setPhone(trimToNull(request.phone()));
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setDepartment(department);
        employee.setPosition(position);
        employee.setRole(request.role());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setHireDate(request.hireDate());
        return EmployeeResponse.from(employees.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        var employee = getEntity(id);
        if (request.email() != null) {
            var email = normalizeEmail(request.email());
            employees.findByEmailIgnoreCase(email)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new AppException(HttpStatus.CONFLICT,
                            "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."); });
            employee.setEmail(email);
        }
        if (request.name() != null) employee.setName(request.name().trim());
        if (request.phone() != null) employee.setPhone(trimToNull(request.phone()));
        if (request.dateOfBirth() != null) employee.setDateOfBirth(request.dateOfBirth());
        if (request.department() != null) employee.setDepartment(
                validateCode(OrganizationCodeType.DEPARTMENT, request.department(), "부서"));
        if (request.position() != null) employee.setPosition(
                validateCode(OrganizationCodeType.POSITION, request.position(), "직급"));
        if (request.role() != null) employee.setRole(request.role());
        if (request.hireDate() != null) employee.setHireDate(request.hireDate());
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse updateMe(Long id, UpdateMeRequest request) {
        var employee = getEntity(id);
        if (request.email() != null) {
            var email = normalizeEmail(request.email());
            employees.findByEmailIgnoreCase(email)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new AppException(HttpStatus.CONFLICT,
                            "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."); });
            employee.setEmail(email);
        }
        if (request.name() != null) employee.setName(request.name().trim());
        if (request.phone() != null) employee.setPhone(trimToNull(request.phone()));
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse terminate(Long id, Long actorId) {
        if (id.equals(actorId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "SELF_TERMINATION_NOT_ALLOWED",
                    "자기 자신을 퇴사 처리할 수 없습니다.");
        }
        var employee = getEntity(id);
        if (employee.getRole() == Role.ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "ADMIN_TERMINATION_NOT_ALLOWED",
                    "관리자 계정은 퇴사 처리할 수 없습니다.");
        }
        if (employee.getStatus() == EmployeeStatus.TERMINATED) return EmployeeResponse.from(employee);
        employee.setStatus(EmployeeStatus.TERMINATED);
        employee.setTerminationDate(Instant.now());
        return EmployeeResponse.from(employee);
    }

    private static String normalizeEmail(String value) { return value.trim().toLowerCase(); }
    private String validateCode(OrganizationCodeType type, String value, String label) {
        var normalized = value.trim();
        if (!organizationCodes.existsByTypeAndName(type, normalized)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_ORGANIZATION_CODE",
                    "등록되지 않은 " + label + "입니다.");
        }
        return normalized;
    }
    private static String trimToNull(String value) {
        if (value == null) return null;
        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
