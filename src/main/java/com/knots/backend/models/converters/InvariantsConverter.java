package com.knots.backend.models.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knots.backend.models.dtos.InvariantType;
import com.knots.backend.models.dtos.InvariantsDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

// Stores InvariantsDto as a JSON object keyed by InvariantType name, so new invariant
// types can be added without a schema change.
@Converter
public class InvariantsConverter implements AttributeConverter<InvariantsDto, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<InvariantType, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(InvariantsDto invariants) {
        if (invariants == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(invariants.values());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize invariants", e);
        }
    }

    @Override
    public InvariantsDto convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return new InvariantsDto(MAPPER.readValue(json, MAP_TYPE));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read invariants: " + json, e);
        }
    }
}
