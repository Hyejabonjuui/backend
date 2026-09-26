package com.hyeja.domain.favorite.converter;

import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteItemDTO;
import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteListDTO;
import com.hyeja.domain.favorite.entity.Favorite;
import org.springframework.data.domain.Page;

public final class FavoriteConverter {

    private FavoriteConverter() {
    }

    public static FavoriteItemDTO toFavoriteItemDTO(Favorite favorite) {
        return FavoriteItemDTO.builder()
                .favoriteId(favorite.getFavoriteId())
                .policyId(favorite.getPolicy().getPolicyId())
                .policyName(favorite.getPolicy().getPolicyName())
                .categoryCode(favorite.getPolicy().getCategory())
                .categoryName(favorite.getPolicy().getCategory().getLabel())
                .supportContent(favorite.getPolicy().getSupportContent())
                .applyEndDate(favorite.getPolicy().getApplyEndDate())
                .applyPeriodCode(favorite.getPolicy().getApplyPeriodCode())
                .applyUrl(favorite.getPolicy().getApplyUrl())
                .createdAt(favorite.getCreatedAt())
                .build();
    }

    public static FavoriteListDTO toFavoriteListDTO(Page<Favorite> favoritePage) {
        var items = favoritePage.getContent().stream()
                .map(FavoriteConverter::toFavoriteItemDTO)
                .toList();
        return FavoriteListDTO.builder()
                .favorites(items)
                .page(favoritePage.getNumber())
                .size(favoritePage.getSize())
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .hasNext(favoritePage.hasNext())
                .build();
    }
}
