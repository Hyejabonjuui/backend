package com.hyeja.domain.member.service;

import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Redis는 가짜(mock)로 바꿔, 어떤 키에 무엇을 저장·조회하는지로 동작을 확인합니다.
// 실제 Redis로 발송 → 확인 → 가입 흐름은 수동으로 확인했습니다(PR 참고).
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailVerificationServiceTest {

    private static final String EMAIL = "hyeja@example.com";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> values;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        ReflectionTestUtils.setField(service, "mailFrom", "hyeja.team@gmail.com");
    }

    @Test
    void sendMailsSixDigitCodeAndStoresItForFiveMinutes() {
        when(values.setIfAbsent("email-verification:cooldown:" + EMAIL, "1", Duration.ofSeconds(60))).thenReturn(true);

        long expiresIn = service.send(EMAIL);

        assertThat(expiresIn).isEqualTo(300);
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly(EMAIL);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("email-verification:code:" + EMAIL), code.capture(), eq(Duration.ofMinutes(5)));
        assertThat(code.getValue()).matches("\\d{6}");
        assertThat(mail.getValue().getText()).contains(code.getValue());
    }

    @Test
    void sendRejectsRegisteredEmail() {
        when(memberRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertError(() -> service.send(EMAIL), ErrorStatus.MEMBER_EMAIL_DUPLICATED);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendRejectsWithinSixtySeconds() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertError(() -> service.send(EMAIL), ErrorStatus.VERIFY_RESEND_TOO_SOON);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // 발송이 실패하면 코드를 저장하지 않고, 60초 대기도 풀어 바로 다시 요청할 수 있게 합니다.
    @Test
    void sendFailureReleasesCooldownAndStoresNoCode() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertError(() -> service.send(EMAIL), ErrorStatus.MAIL_SEND_FAILED);
        verify(redisTemplate).delete("email-verification:cooldown:" + EMAIL);
        verify(values, never()).set(eq("email-verification:code:" + EMAIL), anyString(), any(Duration.class));
    }

    @Test
    void confirmMarksEmailVerifiedForThirtyMinutes() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.get("email-verification:failures:" + EMAIL)).thenReturn("0");

        service.confirm(EMAIL, "384021");

        verify(values).set("email-verification:verified:" + EMAIL, "1", Duration.ofMinutes(30));
        verify(redisTemplate).delete("email-verification:code:" + EMAIL);
    }

    @Test
    void confirmCountsWrongCode() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.get("email-verification:failures:" + EMAIL)).thenReturn("2");

        assertError(() -> service.confirm(EMAIL, "000000"), ErrorStatus.VERIFY_CODE_MISMATCH);
        verify(values).increment("email-verification:failures:" + EMAIL);
    }

    @Test
    void confirmRejectsExpiredOrNeverSentCode() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn(null);

        assertError(() -> service.confirm(EMAIL, "384021"), ErrorStatus.VERIFY_CODE_EXPIRED);
    }

    // 5번 틀린 뒤에는 맞는 코드를 넣어도 막고, 코드를 다시 받게 합니다.
    @Test
    void confirmBlocksAfterFiveFailures() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.get("email-verification:failures:" + EMAIL)).thenReturn("5");

        assertError(() -> service.confirm(EMAIL, "384021"), ErrorStatus.VERIFY_TOO_MANY_FAILURES);
        verify(values, never()).set(eq("email-verification:verified:" + EMAIL), anyString(), any(Duration.class));
    }

    @Test
    void checkVerifiedRejectsUnverifiedEmail() {
        when(redisTemplate.hasKey("email-verification:verified:" + EMAIL)).thenReturn(false);

        assertError(() -> service.checkVerified(EMAIL), ErrorStatus.VERIFY_REQUIRED);
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorStatus expected) {
        assertThatThrownBy(call).isInstanceOf(GeneralException.class).extracting("code").isEqualTo(expected);
    }
}
