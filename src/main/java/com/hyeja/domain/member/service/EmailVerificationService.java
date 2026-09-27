package com.hyeja.domain.member.service;

import com.hyeja.domain.member.dto.EmailVerificationResponseDTO;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 회원가입 이메일 인증. 발송 → 확인 → 가입 순서로 쓰입니다.
 * 인증 코드·시도 횟수·재발송 대기·인증 완료 표시는 모두 Redis에 유효 시간(TTL)을 걸어 저장합니다.
 * 시간이 지나면 Redis가 알아서 지우므로 DB 테이블이나 정리 작업이 필요 없습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    static final Duration CODE_TTL = Duration.ofMinutes(5);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    // 인증에 성공한 뒤 이 시간 안에 가입해야 합니다.
    static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    static final int MAX_FAILURES = 5;
    // 5번 틀리면 이 시간 동안 확인·재발송을 모두 막습니다. 바꾸면 ErrorStatus.VERIFY_TOO_MANY_FAILURES 문구도 함께 고칩니다.
    static final Duration LOCK_TTL = Duration.ofHours(1);

    private static final SecureRandom RANDOM = new SecureRandom();
    // 메일 디자인은 resources/mail/verification-code.html에서 고칩니다. 서버가 뜰 때 한 번 읽어 둡니다.
    private static final String MAIL_TEMPLATE = readTemplate("mail/verification-code.html");

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;

    // 보내는 사람 주소(.env의 MAIL_USERNAME). 비어 있으면 메일 대신 로그로 코드를 보여 줍니다(아래 send 참고).
    @Value("${spring.mail.username:}")
    private String mailFrom;

    /**
     * 6자리 인증 코드를 만들어 메일로 보냅니다. 다시 보내면 새 코드로 바뀌고 틀린 횟수도 초기화됩니다.
     * 이미 가입된 이메일이면 MEMBER_002, 5번 틀려 잠겼으면 VERIFY_005, 60초 안에 다시 요청하면 VERIFY_004,
     * 메일 발송이 실패하면 MAIL_001입니다.
     */
    public long send(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus.MEMBER_EMAIL_DUPLICATED);
        }
        // 재발송으로 틀린 횟수를 초기화해 잠금을 피하지 못하게, 잠긴 동안은 발송도 막습니다.
        checkNotLocked(email);
        // setIfAbsent: 키가 없을 때만 저장합니다. 이미 있으면(60초 안에 보낸 적 있음) false입니다.
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key("cooldown", email), "1", RESEND_COOLDOWN))) {
            throw new GeneralException(ErrorStatus.VERIFY_RESEND_TOO_SOON);
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        try {
            deliver(email, code);
        } catch (MailException e) {
            log.warn("인증 메일 발송 실패: {}", email, e);
            // 발송에 실패했으니 60초 기다리지 않고 바로 다시 요청할 수 있게 합니다.
            redisTemplate.delete(key("cooldown", email));
            throw new GeneralException(ErrorStatus.MAIL_SEND_FAILED);
        }

        redisTemplate.opsForValue().set(key("code", email), code, CODE_TTL);
        redisTemplate.opsForValue().set(key("failures", email), "0", CODE_TTL);
        redisTemplate.delete(key("verified", email));
        return CODE_TTL.toSeconds();
    }

    /**
     * 코드가 맞으면 "인증 완료"를 30분 동안 표시해, 그동안 이 이메일로 가입할 수 있게 합니다.
     * 잠겼으면 VERIFY_005, 코드가 없거나 만료됐으면 VERIFY_002입니다.
     * 틀리면 VERIFY_001과 함께 남은 시도 횟수를 알려 주고, 5번째로 틀린 순간 1시간 동안 잠그고 VERIFY_005입니다.
     */
    public void confirm(String email, String code) {
        checkNotLocked(email);
        String saved = redisTemplate.opsForValue().get(key("code", email));
        if (saved == null) {
            throw new GeneralException(ErrorStatus.VERIFY_CODE_EXPIRED);
        }
        if (!saved.equals(code)) {
            // increment는 기존 유효 시간을 유지한 채 1을 더하고, 더한 뒤의 값을 돌려줍니다.
            Long failures = redisTemplate.opsForValue().increment(key("failures", email));
            long remaining = MAX_FAILURES - (failures == null ? MAX_FAILURES : failures);
            if (remaining <= 0) {
                redisTemplate.opsForValue().set(key("locked", email), "1", LOCK_TTL);
                redisTemplate.delete(key("code", email));
                redisTemplate.delete(key("failures", email));
                throw new GeneralException(ErrorStatus.VERIFY_TOO_MANY_FAILURES);
            }
            throw new GeneralException(ErrorStatus.VERIFY_CODE_MISMATCH,
                    new EmailVerificationResponseDTO.MismatchDTO(remaining));
        }

        redisTemplate.delete(key("code", email));
        redisTemplate.delete(key("failures", email));
        redisTemplate.opsForValue().set(key("verified", email), "1", VERIFIED_TTL);
    }

    // 회원가입 직전에 호출합니다. 인증하지 않았거나 30분이 지났으면 VERIFY_003입니다.
    public void checkVerified(String email) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key("verified", email)))) {
            throw new GeneralException(ErrorStatus.VERIFY_REQUIRED);
        }
    }

    private void checkNotLocked(String email) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key("locked", email)))) {
            throw new GeneralException(ErrorStatus.VERIFY_TOO_MANY_FAILURES);
        }
    }

    // 가입이 끝나면 인증 완료 표시를 지워, 같은 인증으로 다시 가입하지 못하게 합니다.
    public void clearVerified(String email) {
        redisTemplate.delete(key("verified", email));
    }

    private void deliver(String email, String code) {
        // 메일 계정이 없는 로컬 환경(팀원 PC 등)에서는 메일 대신 서버 로그로 코드를 확인합니다.
        if (mailFrom.isBlank()) {
            log.info("[로컬 개발] MAIL_USERNAME이 없어 메일 대신 로그로 출력합니다. {} 인증 코드: {}", email, code);
            return;
        }
        String minutes = String.valueOf(CODE_TTL.toMinutes());
        String html = MAIL_TEMPLATE.replace("{{code}}", code).replace("{{minutes}}", minutes);
        // HTML을 못 보여 주는 메일 앱에서는 아래 글자만 보입니다.
        String plain = "인증 코드: " + code + "\n\n" + minutes + "분 안에 회원가입 화면에 입력해 주세요.";

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("[혜자] 회원가입 인증 코드");
            helper.setText(plain, html);
        } catch (MessagingException e) {
            // MailException으로 바꿔 던져, send()에서 발송 실패(MAIL_001)로 똑같이 처리합니다.
            throw new MailPreparationException("인증 메일 작성 실패", e);
        }
        mailSender.send(message);
    }

    private static String readTemplate(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String key(String type, String email) {
        return "email-verification:" + type + ":" + email;
    }
}
