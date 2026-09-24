package com.hyeja.domain.policy.converter;

import com.hyeja.domain.policy.enums.PolicyCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyCategoryConverterTest {

    private final PolicyCategoryConverter converter = new PolicyCategoryConverter();

    @Test
    void convertsLegacyHousingCategoryToOther() {
        assertThat(converter.convertToEntityAttribute("주거")).isEqualTo(PolicyCategory.OTHER);
    }

    @ParameterizedTest
    @EnumSource(PolicyCategory.class)
    void convertsCurrentCategoryNamesBothWays(PolicyCategory category) {
        assertThat(converter.convertToDatabaseColumn(category)).isEqualTo(category.name());
        assertThat(converter.convertToEntityAttribute(category.name())).isEqualTo(category);
    }
}
