package com.hyeja.domain.member.repository;

import com.hyeja.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 회원(member 테이블) 조회·저장을 담당합니다.
// 계정 조회는 JpaRepository 기본 메서드 findById를 사용합니다.
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 회원가입 중복 검사용. 탈퇴 회원(deleted_at 기록)도 포함해 검사합니다(재가입 불가).
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
