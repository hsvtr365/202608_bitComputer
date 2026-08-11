package com.bitcomputer.portal.controller;

import com.bitcomputer.portal.domain.OrganizationCode;
import com.bitcomputer.portal.domain.OrganizationCodeType;
import com.bitcomputer.portal.repository.OrganizationCodeRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization-codes")
public class OrganizationCodeController {
    private final OrganizationCodeRepository codes;

    public OrganizationCodeController(OrganizationCodeRepository codes) { this.codes = codes; }

    @GetMapping("/departments")
    public List<Item> departments() { return list(OrganizationCodeType.DEPARTMENT); }

    @GetMapping("/positions")
    public List<Item> positions() { return list(OrganizationCodeType.POSITION); }

    private List<Item> list(OrganizationCodeType type) {
        return codes.findByTypeOrderByDisplayOrderAscNameAsc(type).stream().map(Item::from).toList();
    }

    public record Item(String code, String name) {
        static Item from(OrganizationCode code) { return new Item(code.getCode(), code.getName()); }
    }
}
