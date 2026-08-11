package com.bitcomputer.portal.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_codes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_organization_code", columnNames = {"type", "code"}),
        @UniqueConstraint(name = "uk_organization_name", columnNames = {"type", "name"})
})
public class OrganizationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationCodeType type;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected OrganizationCode() {}

    public OrganizationCode(OrganizationCodeType type, String code, String name, int displayOrder) {
        this.type = type;
        this.code = code;
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public OrganizationCodeType getType() { return type; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
}
