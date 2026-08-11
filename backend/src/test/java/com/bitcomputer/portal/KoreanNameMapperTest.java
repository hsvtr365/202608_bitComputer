package com.bitcomputer.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.bitcomputer.portal.integration.backgroundcheck.KoreanNameMapper;
import org.junit.jupiter.api.Test;

class KoreanNameMapperTest {
    private final KoreanNameMapper mapper = new KoreanNameMapper();

    @Test
    void splitsSingleSurname() {
        var result = mapper.split("김민준");
        assertThat(result.lastName()).isEqualTo("김");
        assertThat(result.firstName()).isEqualTo("민준");
    }

    @Test
    void splitsCompoundSurname() {
        var result = mapper.split("남궁민수");
        assertThat(result.lastName()).isEqualTo("남궁");
        assertThat(result.firstName()).isEqualTo("민수");
    }
}
