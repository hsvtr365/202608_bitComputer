package com.bitcomputer.portal.repository;

import com.bitcomputer.portal.domain.OrganizationCode;
import com.bitcomputer.portal.domain.OrganizationCodeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationCodeRepository extends JpaRepository<OrganizationCode, Long> {
    boolean existsByTypeAndName(OrganizationCodeType type, String name);
    List<OrganizationCode> findByTypeOrderByDisplayOrderAscNameAsc(OrganizationCodeType type);
}
