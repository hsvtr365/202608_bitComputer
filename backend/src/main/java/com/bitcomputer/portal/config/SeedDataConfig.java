package com.bitcomputer.portal.config;

import com.bitcomputer.portal.domain.Role;
import com.bitcomputer.portal.dto.EmployeeDtos.CreateEmployeeRequest;
import com.bitcomputer.portal.repository.EmployeeRepository;
import com.bitcomputer.portal.service.EmployeeService;
import java.time.LocalDate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

@Configuration
public class SeedDataConfig {
    @Bean
    @Order(1)
    ApplicationRunner seedData(SeedProperties seed, EmployeeRepository repository, EmployeeService service) {
        return args -> {
            if (!seed.enabled()) return;
            require(seed.adminEmail(), seed.adminPassword(), seed.employeeEmail(), seed.employeePassword(),
                    seed.terminatedEmail(), seed.terminatedPassword());

            if (!repository.existsByEmailIgnoreCase(seed.adminEmail())) {
                service.create(new CreateEmployeeRequest("ADM-001", "관리자", seed.adminEmail(),
                        seed.adminPassword(), "010-1000-1000", LocalDate.of(1985, 1, 15),
                        "경영지원", "관리자", Role.ADMIN, LocalDate.of(2020, 1, 2)));
            }
            if (!repository.existsByEmailIgnoreCase(seed.employeeEmail())) {
                service.create(new CreateEmployeeRequest("EMP-001", "김민준", seed.employeeEmail(),
                        seed.employeePassword(), "010-2000-2000", LocalDate.of(1990, 3, 15),
                        "개발", "선임", Role.EMPLOYEE, LocalDate.of(2024, 2, 1)));
            }
            if (!repository.existsByEmailIgnoreCase(seed.terminatedEmail())) {
                var terminated = service.create(new CreateEmployeeRequest("EMP-999", "남궁민수",
                        seed.terminatedEmail(), seed.terminatedPassword(), "010-9999-9999",
                        LocalDate.of(1988, 7, 20), "영업", "과장", Role.EMPLOYEE,
                        LocalDate.of(2021, 5, 3)));
                var adminId = repository.findByEmailIgnoreCase(seed.adminEmail()).orElseThrow().getId();
                service.terminate(terminated.id(), adminId);
            }
        };
    }

    private static void require(String... values) {
        for (var value : values) {
            if (!StringUtils.hasText(value)) {
                throw new IllegalStateException("Seed is enabled, but seed account values are missing.");
            }
        }
    }
}
