package com.hyeja.domain.policy.converter;

import com.hyeja.domain.policy.enums.PolicyCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PolicyCategoryConverter implements AttributeConverter<PolicyCategory, String> {

    private static final String LEGACY_HOUSING_CATEGORY = "주거";

    @Override
    public String convertToDatabaseColumn(PolicyCategory category) {
        return category == null ? null : category.name();
    }

    @Override
    public PolicyCategory convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        if (LEGACY_HOUSING_CATEGORY.equals(value)) {
            return PolicyCategory.OTHER;
        }
        return PolicyCategory.valueOf(value);
    }
}
