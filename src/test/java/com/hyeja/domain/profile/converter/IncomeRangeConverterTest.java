package com.hyeja.domain.profile.converter;

import com.hyeja.domain.profile.enums.IncomeRange;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeRangeConverterTest {

    private final IncomeRangeConverter converter = new IncomeRangeConverter();

    @Test
    void convertsLegacyIncomeRangeCodes() {
        Map<String, IncomeRange> legacyCodes = Map.of(
                "INC_0_20", IncomeRange.UNDER_2000,
                "INC_20_30", IncomeRange.R2000_3000,
                "INC_30_40", IncomeRange.R3000_4000,
                "INC_40_UP", IncomeRange.R4000_5000);

        legacyCodes.forEach((code, range) ->
                assertThat(converter.convertToEntityAttribute(code)).isEqualTo(range));
    }

    @ParameterizedTest
    @EnumSource(IncomeRange.class)
    void convertsCurrentIncomeRangeNamesBothWays(IncomeRange range) {
        assertThat(converter.convertToDatabaseColumn(range)).isEqualTo(range.name());
        assertThat(converter.convertToEntityAttribute(range.name())).isEqualTo(range);
    }
}
