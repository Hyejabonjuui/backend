package com.hyeja.domain.policy.repository;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    @Query("""
            select policy
            from Policy policy
            where policy.deletedAt is null
              and policy.activeYn = true
              and (policy.applyEndDate is null or policy.applyEndDate >= :today)
              and (:category is null or policy.category = :category)
            """)
    Page<Policy> findGuestHousingPolicies(
            @Param("category") PolicyCategory category,
            @Param("today") LocalDate today,
            Pageable pageable
    );
}
