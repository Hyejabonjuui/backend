package com.hyeja.domain.profile.converter;

import com.hyeja.domain.profile.enums.IncomeRange;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IncomeRangeConverter implements AttributeConverter<IncomeRange, String> {

    @Override
    public String convertToDatabaseColumn(IncomeRange range) {
        return range == null ? null : range.name();
    }

    @Override
    public IncomeRange convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "INC_0_20" -> IncomeRange.UNDER_2000;
            case "INC_20_30" -> IncomeRange.R2000_3000;
            case "INC_30_40" -> IncomeRange.R3000_4000;
            case "INC_40_UP" -> IncomeRange.R4000_5000;
            default -> IncomeRange.valueOf(value);
        };
    }
}
