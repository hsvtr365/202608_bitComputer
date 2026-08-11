package com.bitcomputer.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bitcomputer.portal.domain.Employee;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckClient;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckDtos.History;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckDtos.HistoryItem;
import com.bitcomputer.portal.integration.backgroundcheck.KoreanNameMapper;
import com.bitcomputer.portal.service.BackgroundCheckService;
import com.bitcomputer.portal.service.EmployeeService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BackgroundCheckServiceTest {
    @Test
    void historyIsOrderedByMostRecentCompletion() {
        var employees = mock(EmployeeService.class);
        var client = mock(BackgroundCheckClient.class);
        var employee = new Employee();
        employee.setEmployeeNumber("EMP-001");
        when(employees.getEntity(1L)).thenReturn(employee);
        when(client.history("EMP-001")).thenReturn(new History("EMP-001", List.of(
                item("old", "2026-01-01T10:00:00Z", "2026-01-01T11:00:00Z"),
                item("pending", "2026-01-03T10:00:00Z", null),
                item("new", "2026-01-02T10:00:00Z", "2026-01-02T11:00:00Z")), 3));

        var service = new BackgroundCheckService(employees, mock(KoreanNameMapper.class), client);

        var result = service.history(1L);

        assertThat(result.checks()).extracting(HistoryItem::checkId).containsExactly("new", "old", "pending");
        assertThat(result.totalCount()).isEqualTo(3);
    }

    private static HistoryItem item(String id, String createdAt, String completedAt) {
        return new HistoryItem(id, completedAt == null ? "pending" : "clear", Instant.parse(createdAt),
                completedAt == null ? null : Instant.parse(completedAt));
    }
}
