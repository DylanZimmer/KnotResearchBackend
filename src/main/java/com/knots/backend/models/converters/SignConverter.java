package com.knots.backend.models.converters;

import com.knots.backend.models.dtos.Sign;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SignConverter implements AttributeConverter<Sign, Long> {
    @Override
    public Long convertToDatabaseColumn(Sign sign) {
        return sign == null ? null : sign.value();
    }

    @Override
    public Sign convertToEntityAttribute(Long value) {
        return value == null ? null : Sign.fromValue(value);
    }
}
