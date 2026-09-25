package com.hyeja.domain.favorite.service;

import com.hyeja.domain.favorite.converter.FavoriteConverter;
import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteItemDTO;
import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteListDTO;
import com.hyeja.domain.favorite.entity.Favorite;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final MemberService memberService;
    private final PolicyRepository policyRepository;
    private final FavoriteRepository favoriteRepository;

    @Transactional
    public FavoriteItemDTO createFavorite(Long memberId, String policyId) {
        Member member = memberService.getActiveMember(memberId);
        Policy policy = policyRepository.findById(policyId)
                .filter(value -> !value.isDeleted() && Boolean.TRUE.equals(value.getActiveYn()))
                .orElseThrow(() -> new GeneralException(ErrorStatus.POLICY_NOT_FOUND));

        if (favoriteRepository.existsByMemberMemberIdAndPolicyPolicyId(memberId, policyId)) {
            throw new GeneralException(ErrorStatus.FAVORITE_ALREADY_EXISTS);
        }

        try {
            Favorite favorite = favoriteRepository.saveAndFlush(
                    Favorite.builder()
                            .member(member)
                            .policy(policy)
                            .build()
            );
            return FavoriteConverter.toFavoriteItemDTO(favorite);
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(ErrorStatus.FAVORITE_ALREADY_EXISTS);
        }
    }

    public FavoriteListDTO getMyFavorites(Long memberId, int page, int size) {
        memberService.getActiveMember(memberId);
        return FavoriteConverter.toFavoriteListDTO(
                favoriteRepository.findAllActiveByMemberId(memberId, PageRequest.of(page, size))
        );
    }
}
