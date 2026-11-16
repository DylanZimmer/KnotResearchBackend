package com.knots.backend.config;

import com.knots.backend.models.dtos.LongPair;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

// Lets request parameters like underStrandOriented=3,1 bind to LongPair (used by /api/moves/addPoke).
@Component
public class StringToLongPairConverter implements Converter<String, LongPair> {
    @Override
    public LongPair convert(String source) {
        String[] parts = source.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected two numbers separated by a comma, got: " + source);
        }
        return new LongPair(Long.parseLong(parts[0].trim()), Long.parseLong(parts[1].trim()));
    }
}
