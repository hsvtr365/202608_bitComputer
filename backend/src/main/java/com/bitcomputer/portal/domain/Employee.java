package com.bitcomputer.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_number", columnNames = "employee_number"),
        @UniqueConstraint(name = "uk_employee_email", columnNames = "email")
})
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_number", nullable = false, length = 40)
    private String employeeNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 30)
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private Instant terminationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = EmployeeStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String value) { employeeNumber = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate value) { dateOfBirth = value; }
    public String getDepartment() { return department; }
    public void setDepartment(String value) { department = value; }
    public String getPosition() { return position; }
    public void setPosition(String value) { position = value; }
    public Role getRole() { return role; }
    public void setRole(Role value) { role = value; }
    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus value) { status = value; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate value) { hireDate = value; }
    public Instant getTerminationDate() { return terminationDate; }
    public void setTerminationDate(Instant value) { terminationDate = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
