package com.bitcomputer.portal.integration.backgroundcheck;

import com.bitcomputer.portal.exception.AppException;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckDtos.NameParts;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class KoreanNameMapper {
    private static final Set<String> COMPOUND_SURNAMES = Set.of(
            "남궁", "황보", "제갈", "선우", "사공", "서문", "독고");

    public NameParts split(String rawName) {
        var name = rawName == null ? "" : rawName.replaceAll("\\s+", "");
        if (name.length() < 2) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_KOREAN_NAME",
                    "Background Check를 위해 두 글자 이상의 이름이 필요합니다.");
        }
        var surnameLength = name.length() >= 3 && COMPOUND_SURNAMES.contains(name.substring(0, 2)) ? 2 : 1;
        return new NameParts(name.substring(surnameLength), name.substring(0, surnameLength));
    }
}
