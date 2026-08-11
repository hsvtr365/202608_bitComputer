package com.bitcomputer.portal.integration.backgroundcheck;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BackgroundCheckDtos {
    private BackgroundCheckDtos() {}

    public record CreateRequest(String employeeId, String firstName, String lastName, LocalDate dateOfBirth) {}

    public record CreateOverrideRequest(
            @Size(max = 100) @Pattern(regexp = "[\\p{L}][\\p{L} -]{0,99}") String firstName,
            @Size(max = 100) @Pattern(regexp = "[\\p{L}][\\p{L} -]{0,99}") String lastName) {}

    public record Created(String checkId, String employeeId, String status, Instant createdAt, String message) {}

    public record Result(String checkId, String employeeId, String firstName, String lastName,
                         LocalDate dateOfBirth, String status, Boolean criminalRecord,
                         Boolean educationVerified, Boolean employmentVerified, String creditScore,
                         Instant createdAt, Instant completedAt) {}

    public record HistoryItem(String checkId, String status, Instant createdAt, Instant completedAt) {}
    public record History(String employeeId, List<HistoryItem> checks, int totalCount) {}
    public record ExternalError(String error, String message, Integer retryAfter, Integer statusCode) {}
    public record NameParts(String firstName, String lastName) {}
}
