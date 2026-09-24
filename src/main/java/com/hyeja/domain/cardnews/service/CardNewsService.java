package com.hyeja.domain.cardnews.service;

import com.hyeja.domain.cardnews.dto.CardNewsResponseDTO;
import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardNewsService {

    private final CardNewsRepository cardNewsRepository;

    public List<CardNewsResponseDTO> getGuestCardNews() {
        // Pageable 없이 레포지토리에서 상위 4개를 바로 조회
        List<CardNews> cardNewsList = cardNewsRepository.findTop4CardNews();

        return cardNewsList.stream()
                .map(cn -> CardNewsResponseDTO.builder()
                        .policyId(cn.getPolicy().getPolicyId())
                        .policyName(cn.getPolicy().getPolicyName())
                        .description(cn.getBody())
                        .applyEndDate(cn.getPolicy().getApplyEndDate() != null ? cn.getPolicy().getApplyEndDate().toString() : null)
                        .build()
                )
                .collect(Collectors.toList());
    }
}