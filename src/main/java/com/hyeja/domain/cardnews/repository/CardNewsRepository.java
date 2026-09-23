package com.hyeja.domain.cardnews.repository;

import com.hyeja.domain.cardnews.entity.CardNews;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository 
public interface CardNewsRepository extends JpaRepository<CardNews, Long> {

    // LIMIT 4를 포함하여 대표 카드(card_no = 1)를 마감일 오름차순으로 상위 4개 조회
    @Query("SELECT cn FROM CardNews cn JOIN FETCH cn.policy p WHERE cn.cardNo = 1 ORDER BY p.applyEndDate ASC LIMIT 4")
    List<CardNews> findTop4CardNews();

    // 특정 정책 ID와 카드 번호(card_No)로 카드뉴스가 이미 존재하는지 확인하는 메서드
    boolean existsByPolicy_PolicyIdAndCardNo(String policyId, Long cardNo);
}