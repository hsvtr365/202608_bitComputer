package com.bitcomputer.portal.service;

import com.bitcomputer.portal.exception.AppException;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckClient;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckDtos.*;
import com.bitcomputer.portal.integration.backgroundcheck.KoreanNameMapper;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BackgroundCheckService {
    private final EmployeeService employees;
    private final KoreanNameMapper nameMapper;
    private final BackgroundCheckClient client;

    public BackgroundCheckService(EmployeeService employees, KoreanNameMapper nameMapper,
                                  BackgroundCheckClient client) {
        this.employees = employees;
        this.nameMapper = nameMapper;
        this.client = client;
    }

    public NameParts nameParts(Long employeeId) {
        return nameMapper.split(employees.getEntity(employeeId).getName());
    }

    public Created create(Long employeeId, CreateOverrideRequest override) {
        var employee = employees.getEntity(employeeId);
        var mapped = nameMapper.split(employee.getName());
        var firstName = choose(override == null ? null : override.firstName(), mapped.firstName());
        var lastName = choose(override == null ? null : override.lastName(), mapped.lastName());
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_BACKGROUND_CHECK_NAME",
                    "성과 이름을 모두 입력해 주세요.");
        }
        return client.create(new CreateRequest(employee.getEmployeeNumber(), firstName, lastName,
                employee.getDateOfBirth()));
    }

    public HistoryPage history(Long employeeId, int page, int size) {
        var history = client.history(employees.getEntity(employeeId).getEmployeeNumber());
        var checks = history.checks().stream()
                .sorted(Comparator.comparing(HistoryItem::completedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HistoryItem::createdAt, Comparator.reverseOrder()))
                .toList();
        var totalCount = checks.size();
        var totalPages = Math.max(1, (totalCount + size - 1) / size);
        var effectivePage = Math.min(page, totalPages - 1);
        var from = effectivePage * size;
        var to = Math.min(from + size, totalCount);
        return new HistoryPage(history.employeeId(), checks.subList(from, to), totalCount, effectivePage, size,
                totalPages);
    }

    public Result get(String checkId) { return client.get(checkId); }

    private static String choose(String override, String fallback) {
        return StringUtils.hasText(override) ? override.trim() : fallback;
    }
}
