package com.hyeja.domain.policy.repository;

import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.entity.PolicyRegionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, PolicyRegionId> {
    List<PolicyRegion> findAllByPolicy_PolicyId(String policyId);
}
