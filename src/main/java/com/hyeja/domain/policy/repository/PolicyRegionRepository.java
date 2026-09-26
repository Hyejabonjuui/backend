package com.hyeja.domain.policy.repository;

import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.entity.PolicyRegionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, PolicyRegionId> {
    List<PolicyRegion> findAllByPolicy_PolicyId(String policyId);

    @Modifying
    @Query("delete from PolicyRegion policyRegion "
            + "where policyRegion.policy.policyId = :policyId")
    void deleteAllByPolicy_PolicyId(@Param("policyId") String policyId);
}
