package com.hyeja.domain.policy.repository;

import com.hyeja.domain.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    // 내부 분류와 관계없이 적재된 주거 정책 전체를 마감일 오름차순으로 조회합니다.
    List<Policy> findAllByOrderByApplyEndDateAsc();
}
