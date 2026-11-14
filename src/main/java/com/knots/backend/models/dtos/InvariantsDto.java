package com.knots.backend.models.dtos;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record InvariantsDto(
        Map<InvariantType, Object> values
) {
    // Drops null entries and coerces each value to the type its InvariantType declares, so a
    // determinant read back from JSON (Integer) still equals one computed here (Long).
    public InvariantsDto {
        Map<InvariantType, Object> normalized = new EnumMap<>(InvariantType.class);
        if (values != null) {
            values.forEach((type, value) -> {
                if (type != null && value != null) {
                    normalized.put(type, coerce(type, value));
                }
            });
        }
        values = Collections.unmodifiableMap(normalized);
    }

    // Used by the JPQL constructor queries and anywhere the three classic invariants are at hand.
    public InvariantsDto(String alexanderPolynomial, Long determinant, Long writhe) {
        this(mapOf(alexanderPolynomial, determinant, writhe));
    }

    public Object get(InvariantType type) {
        return values.get(type);
    }

    private static Map<InvariantType, Object> mapOf(String alexanderPolynomial, Long determinant, Long writhe) {
        Map<InvariantType, Object> map = new EnumMap<>(InvariantType.class);
        if (alexanderPolynomial != null) map.put(InvariantType.Alexander_Polynomial, alexanderPolynomial);
        if (determinant != null) map.put(InvariantType.Determinant, determinant);
        if (writhe != null) map.put(InvariantType.Writhe, writhe);
        return map;
    }

    private static Object coerce(InvariantType type, Object value) {
        if (type.getValueType() == Long.class && value instanceof Number number) {
            return number.longValue();
        }
        if (type.getValueType() == String.class) {
            return value.toString();
        }
        return value;
    }
}
