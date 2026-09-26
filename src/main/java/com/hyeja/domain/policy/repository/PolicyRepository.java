package com.hyeja.domain.policy.repository;

import com.hyeja.domain.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    // "주거" 카테고리 정책을 마감일 오름차순으로 조회
    List<Policy> findAllByOrderByApplyEndDateAsc();
}
