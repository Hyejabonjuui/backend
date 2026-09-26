package com.hyeja.domain.policy.repository;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    // "주거" 카테고리 정책을 마감일 오름차순으로 조회
    List<Policy> findAllByOrderByApplyEndDateAsc();

    @Query("""
            select policy
            from Policy policy
            where policy.deletedAt is null
              and policy.activeYn = true
              and policy.applyPeriodCode <> '0057003'
              and (policy.applyEndDate is null or policy.applyEndDate >= :today)
              and (:category is null or policy.category = :category)
              and (
                  :onlyEligible = false
                  or (
                      (
                          policy.ageLimitYn = false
                          or (
                              (policy.minAge is null or policy.minAge <= :age)
                              and (policy.maxAge is null or policy.maxAge >= :age)
                          )
                      )
                      and (
                          policy.houselessYn is null
                          or policy.houselessYn = false
                          or :houselessYn = true
                      )
                      and (
                          policy.employmentCodes is null
                          or trim(cast(policy.employmentCodes as string)) = ''
                          or cast(policy.employmentCodes as string) like '%NO_RESTRICTION%'
                          or cast(policy.employmentCodes as string) = :employmentCode
                          or cast(policy.employmentCodes as string) like concat(:employmentCode, ',%')
                          or cast(policy.employmentCodes as string) like concat('%,', :employmentCode)
                          or cast(policy.employmentCodes as string)
                              like concat('%,', concat(:employmentCode, ',%'))
                      )
                      and (
                          not exists (
                              select policyRegion.id
                              from PolicyRegion policyRegion
                              where policyRegion.policy = policy
                                and policyRegion.deletedAt is null
                                and policyRegion.region.deletedAt is null
                          )
                          or exists (
                              select matchingRegion.id
                              from PolicyRegion matchingRegion
                              where matchingRegion.policy = policy
                                and matchingRegion.deletedAt is null
                                and matchingRegion.region.deletedAt is null
                                and matchingRegion.region.regionCode = :regionCode
                          )
                      )
                  )
              )
            """)
    Page<Policy> findHousingPoliciesForMember(
            @Param("category") PolicyCategory category,
            @Param("onlyEligible") boolean onlyEligible,
            @Param("today") LocalDate today,
            @Param("age") int age,
            @Param("houselessYn") boolean houselessYn,
            @Param("employmentCode") String employmentCode,
            @Param("regionCode") String regionCode,
            Pageable pageable
    );
}
