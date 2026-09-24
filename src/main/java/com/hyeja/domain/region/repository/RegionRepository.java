package com.hyeja.domain.region.repository; // 프로젝트 구조에 맞게 패키지 수정

import com.hyeja.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionRepository extends JpaRepository<Region, String> {
}
