package com.hyeja.domain.member.init;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 서버가 켜질 때 .env의 ADMIN_EMAIL·ADMIN_PASSWORD로 관리자(ADMIN) 계정을 만듭니다.
 * 관리자는 정책 동기화·알림 생성처럼 관리자 전용 API를 호출할 때만 씁니다(SecurityConfig 참고).
 * - 두 값 중 하나라도 비어 있으면 건너뜁니다. 관리자가 필요 없는 팀원·CI·테스트는 설정하지 않아도 됩니다.
 * - 같은 이메일이 이미 있으면 건너뜁니다. 서버를 다시 켜도 계정이 늘어나지 않습니다.
 * 비밀번호를 코드나 시드에 두지 않아 배포 환경에서도 그대로 쓸 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String email;

    @Value("${admin.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("[AdminInitializer] ADMIN_EMAIL·ADMIN_PASSWORD가 없어 관리자 계정 생성을 건너뜁니다.");
            return;
        }
        if (memberRepository.existsByEmail(email)) {
            // 이미 있는 계정의 권한은 바꾸지 않습니다. 일반 회원 이메일을 넣었다면 다른 이메일을 쓰세요.
            log.info("[AdminInitializer] {} 계정이 이미 있어 건너뜁니다.", email);
            return;
        }
        memberRepository.save(Member.admin(email, passwordEncoder.encode(password)));
        log.info("[AdminInitializer] 관리자 계정을 만들었습니다: {}", email);
    }
}
