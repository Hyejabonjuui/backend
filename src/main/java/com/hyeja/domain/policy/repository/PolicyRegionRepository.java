package com.hyeja.domain.policy.repository;

import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.entity.PolicyRegionId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, PolicyRegionId> {

    @Query("""
            select policyRegion
            from PolicyRegion policyRegion
            join fetch policyRegion.region region
            where policyRegion.policy.policyId in :policyIds
              and policyRegion.deletedAt is null
              and region.deletedAt is null
            order by policyRegion.policy.policyId, region.regionCode
            """)
    List<PolicyRegion> findAllActiveByPolicyIds(
            @Param("policyIds") Collection<String> policyIds
    );
}
