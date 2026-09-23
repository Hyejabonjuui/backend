package com.hyeja.domain.cardnews.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.hyeja.domain.cardnews.entity.CardNews;

@Repository 
public interface CardNewsRepository extends JpaRepository<CardNews, Long> {

    @Query("SELECT cn FROM CardNews cn JOIN FETCH cn.policy p WHERE cn.cardNo = 1 ORDER BY p.applyEndDate ASC LIMIT 4")
    List<CardNews> findTop4CardNews();
}