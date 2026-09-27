package com.hyeja.domain.member.init;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.enums.Role;
import com.hyeja.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInitializerTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminInitializer initializer = new AdminInitializer(memberRepository, passwordEncoder);

    @Test
    void createsAdminWithEncodedPassword() {
        setAdmin("admin@hyeja.test", "admin-pass");
        when(passwordEncoder.encode("admin-pass")).thenReturn("encoded");

        initializer.run();

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@hyeja.test");
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    // 값이 비어 있으면(팀원·CI·테스트 기본) 아무것도 만들지 않습니다.
    @Test
    void skipsWhenEmailOrPasswordIsBlank() {
        setAdmin("", "admin-pass");
        initializer.run();
        setAdmin("admin@hyeja.test", "");
        initializer.run();

        verify(memberRepository, never()).save(any());
    }

    // 서버를 다시 켜도 같은 이메일로 또 만들지 않습니다.
    @Test
    void skipsWhenEmailAlreadyExists() {
        setAdmin("admin@hyeja.test", "admin-pass");
        when(memberRepository.existsByEmail("admin@hyeja.test")).thenReturn(true);

        initializer.run();

        verify(memberRepository, never()).save(any());
    }

    private void setAdmin(String email, String password) {
        ReflectionTestUtils.setField(initializer, "email", email);
        ReflectionTestUtils.setField(initializer, "password", password);
    }
}
