package com.hyeja.domain.member.service;

import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
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
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        ReflectionTestUtils.setField(service, "mailFrom", "hyeja.team@gmail.com");
    }

    @Test
    void sendMailsSixDigitCodeAndStoresItForFiveMinutes() throws Exception {
        when(values.setIfAbsent("email-verification:cooldown:" + EMAIL, "1", Duration.ofSeconds(60))).thenReturn(true);

        long expiresIn = service.send(EMAIL);

        assertThat(expiresIn).isEqualTo(300);
        ArgumentCaptor<MimeMessage> mail = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getAllRecipients()).containsExactly(new InternetAddress(EMAIL));
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("email-verification:code:" + EMAIL), code.capture(), eq(Duration.ofMinutes(5)));
        assertThat(code.getValue()).matches("\\d{6}");
        // 재발송해도 틀린 횟수를 초기화하지 않습니다(초기화하면 4번 틀리고 재발송을 반복해 잠금을 피할 수 있음).
        verify(values, never()).set(eq("email-verification:failures:" + EMAIL), anyString(), any(Duration.class));
        // HTML 본문과 글자 본문에 코드와 유효 시간이 들어가고, 템플릿 자리표시({{...}})는 남지 않아야 합니다.
        assertThat(bodyOf(mail.getValue())).contains("<html", code.getValue(), "5분").doesNotContain("{{");
    }

    @Test
    void sendRejectsRegisteredEmail() {
        when(memberRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertError(() -> service.send(EMAIL), ErrorStatus.MEMBER_EMAIL_DUPLICATED);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendRejectsLockedEmail() {
        when(redisTemplate.hasKey("email-verification:locked:" + EMAIL)).thenReturn(true);

        assertError(() -> service.send(EMAIL), ErrorStatus.VERIFY_TOO_MANY_FAILURES);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendRejectsWithinSixtySeconds() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertError(() -> service.send(EMAIL), ErrorStatus.VERIFY_RESEND_TOO_SOON);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // 발송이 실패하면 코드를 저장하지 않고, 60초 대기도 풀어 바로 다시 요청할 수 있게 합니다.
    @Test
    void sendFailureReleasesCooldownAndStoresNoCode() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        doThrow(new MailSendException("down")).when(mailSender).send(any(MimeMessage.class));

        assertError(() -> service.send(EMAIL), ErrorStatus.MAIL_SEND_FAILED);
        verify(redisTemplate).delete("email-verification:cooldown:" + EMAIL);
        verify(values, never()).set(eq("email-verification:code:" + EMAIL), anyString(), any(Duration.class));
    }

    @Test
    void confirmMarksEmailVerifiedForThirtyMinutes() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.increment("email-verification:failures:" + EMAIL)).thenReturn(1L);

        service.confirm(EMAIL, "384021");

        verify(values).set("email-verification:verified:" + EMAIL, "1", Duration.ofMinutes(30));
        verify(redisTemplate).delete("email-verification:code:" + EMAIL);
    }

    // 틀리면 횟수를 세고, 남은 기회를 에러 응답의 result로 알려 줍니다.
    @Test
    void confirmCountsWrongCodeAndReturnsRemainingAttempts() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.increment("email-verification:failures:" + EMAIL)).thenReturn(2L);

        assertThatThrownBy(() -> service.confirm(EMAIL, "000000"))
                .isInstanceOf(GeneralException.class)
                .satisfies(e -> {
                    assertThat(((GeneralException) e).getCode()).isEqualTo(ErrorStatus.VERIFY_CODE_MISMATCH);
                    assertThat(e).extracting("result").extracting("remainingAttempts").isEqualTo(3L);
                });
        verify(values).increment("email-verification:failures:" + EMAIL);
    }

    // 첫 시도에만 1시간 유효 시간을 걸어, 그 1시간 동안은 재발송해도 횟수가 이어집니다.
    @Test
    void firstAttemptKeepsCountForOneHour() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.increment("email-verification:failures:" + EMAIL)).thenReturn(1L);

        assertError(() -> service.confirm(EMAIL, "000000"), ErrorStatus.VERIFY_CODE_MISMATCH);
        verify(redisTemplate).expire("email-verification:failures:" + EMAIL, Duration.ofHours(1));
    }

    // 동시에 여러 요청을 보내 5번을 넘긴 요청은 맞는 코드여도 통과시키지 않습니다.
    @Test
    void confirmRejectsCorrectCodeBeyondFifthAttempt() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.increment("email-verification:failures:" + EMAIL)).thenReturn(6L);

        assertError(() -> service.confirm(EMAIL, "384021"), ErrorStatus.VERIFY_TOO_MANY_FAILURES);
        verify(values, never()).set(eq("email-verification:verified:" + EMAIL), anyString(), any(Duration.class));
    }

    @Test
    void confirmRejectsExpiredOrNeverSentCode() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn(null);

        assertError(() -> service.confirm(EMAIL, "384021"), ErrorStatus.VERIFY_CODE_EXPIRED);
    }

    // 5번째로 틀린 순간 1시간 잠그고, 코드도 지워 더 이상 쓸 수 없게 합니다.
    @Test
    void confirmLocksForOneHourOnFifthFailure() {
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");
        when(values.increment("email-verification:failures:" + EMAIL)).thenReturn(5L);

        assertError(() -> service.confirm(EMAIL, "000000"), ErrorStatus.VERIFY_TOO_MANY_FAILURES);
        verify(values).set("email-verification:locked:" + EMAIL, "1", Duration.ofHours(1));
        verify(redisTemplate).delete("email-verification:code:" + EMAIL);
        // 횟수를 지우면 잠금 직전의 동시 요청이 1부터 다시 세므로 남겨 둡니다.
        verify(redisTemplate, never()).delete("email-verification:failures:" + EMAIL);
    }

    // 잠긴 동안은 맞는 코드를 넣어도 막습니다.
    @Test
    void confirmRejectsWhileLocked() {
        when(redisTemplate.hasKey("email-verification:locked:" + EMAIL)).thenReturn(true);
        when(values.get("email-verification:code:" + EMAIL)).thenReturn("384021");

        assertError(() -> service.confirm(EMAIL, "384021"), ErrorStatus.VERIFY_TOO_MANY_FAILURES);
        verify(values, never()).set(eq("email-verification:verified:" + EMAIL), anyString(), any(Duration.class));
    }

    @Test
    void checkVerifiedRejectsUnverifiedEmail() {
        when(redisTemplate.hasKey("email-verification:verified:" + EMAIL)).thenReturn(false);

        assertError(() -> service.checkVerified(EMAIL), ErrorStatus.VERIFY_REQUIRED);
    }

    // 메일은 HTML·글자 본문이 여러 조각(Multipart)으로 들어 있어, 조각을 모두 이어 붙여 확인합니다.
    private static String bodyOf(Part part) throws Exception {
        if (part.getContent() instanceof Multipart multipart) {
            StringBuilder body = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                body.append(bodyOf(multipart.getBodyPart(i)));
            }
            return body.toString();
        }
        return part.getContent().toString();
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorStatus expected) {
        assertThatThrownBy(call).isInstanceOf(GeneralException.class).extracting("code").isEqualTo(expected);
    }
}
