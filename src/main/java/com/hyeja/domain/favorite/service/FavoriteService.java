package com.hyeja.domain.favorite.service;

import com.hyeja.domain.favorite.converter.FavoriteConverter;
import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteListDTO;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final MemberService memberService;
    private final FavoriteRepository favoriteRepository;

    public FavoriteListDTO getMyFavorites(Long memberId, int page, int size) {
        memberService.getActiveMember(memberId);
        return FavoriteConverter.toFavoriteListDTO(
                favoriteRepository.findAllActiveByMemberId(memberId, PageRequest.of(page, size))
        );
    }
}
