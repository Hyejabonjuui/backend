package com.hyeja.domain.member.repository;

import com.hyeja.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 회원(member 테이블) 조회·저장을 담당합니다.
// 계정 조회는 JpaRepository 기본 메서드 findById만 사용하므로 별도 메서드를 두지 않습니다.
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
