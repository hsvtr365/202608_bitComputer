package com.bitcomputer.portal.config;

import com.bitcomputer.portal.domain.OrganizationCode;
import com.bitcomputer.portal.domain.OrganizationCodeType;
import com.bitcomputer.portal.repository.EmployeeRepository;
import com.bitcomputer.portal.repository.OrganizationCodeRepository;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class OrganizationCodeConfig {
    @Bean
    @Order(0)
    ApplicationRunner seedOrganizationCodes(OrganizationCodeRepository codes, EmployeeRepository employees) {
        return args -> {
            seed(codes, OrganizationCodeType.DEPARTMENT, List.of(
                    new Item("MANAGEMENT_SUPPORT", "경영지원"), new Item("DEVELOPMENT", "개발"),
                    new Item("SALES", "영업"), new Item("HR", "인사"), new Item("FINANCE", "재무")));
            seed(codes, OrganizationCodeType.POSITION, List.of(
                    new Item("STAFF", "사원"), new Item("SENIOR", "선임"), new Item("ASSISTANT_MANAGER", "대리"),
                    new Item("MANAGER", "과장"), new Item("DEPUTY_GENERAL_MANAGER", "차장"),
                    new Item("GENERAL_MANAGER", "부장"), new Item("EXECUTIVE", "임원"),
                    new Item("ADMINISTRATOR", "관리자")));
            addLegacy(codes, OrganizationCodeType.DEPARTMENT, employees.findDistinctDepartments());
            addLegacy(codes, OrganizationCodeType.POSITION, employees.findDistinctPositions());
        };
    }

    private static void seed(OrganizationCodeRepository repository, OrganizationCodeType type, List<Item> items) {
        var order = 10;
        for (var item : items) {
            if (!repository.existsByTypeAndName(type, item.name())) {
                repository.save(new OrganizationCode(type, item.code(), item.name(), order));
            }
            order += 10;
        }
    }

    private static void addLegacy(OrganizationCodeRepository repository, OrganizationCodeType type,
                                  List<String> names) {
        var order = 1000;
        for (var name : names) {
            if (!repository.existsByTypeAndName(type, name)) {
                repository.save(new OrganizationCode(type,
                        "LEGACY_" + Integer.toUnsignedString(name.hashCode(), 16).toUpperCase(), name, order++));
            }
        }
    }

    private record Item(String code, String name) {}
}
