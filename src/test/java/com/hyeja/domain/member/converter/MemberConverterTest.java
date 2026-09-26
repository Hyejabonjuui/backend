package com.hyeja.domain.member.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberConverterTest {

    // @ 앞이 3글자를 넘으면 앞 3글자, 3글자 이하면 첫 글자만 남기고 가립니다. 도메인은 그대로입니다.
    @Test
    void masksEmailLocalPart() {
        assertThat(MemberConverter.maskEmail("mingy@hyeja.kr")).isEqualTo("min***@hyeja.kr");
        assertThat(MemberConverter.maskEmail("abcd@hyeja.kr")).isEqualTo("abc***@hyeja.kr");
        assertThat(MemberConverter.maskEmail("abc@hyeja.kr")).isEqualTo("a***@hyeja.kr");
        assertThat(MemberConverter.maskEmail("ab@hyeja.kr")).isEqualTo("a***@hyeja.kr");
        assertThat(MemberConverter.maskEmail("a@hyeja.kr")).isEqualTo("a***@hyeja.kr");
    }
}
