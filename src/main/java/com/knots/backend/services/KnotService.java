package com.knots.backend.services;

import com.knots.backend.models.dtos.KnotDto;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.repositories.FullNotationRolfRepo;
import com.knots.backend.repositories.KnotRepo;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;
import java.util.*;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KnotService {

    private final KnotRepo knotRepo;
    private final FullNotationRolfRepo fullNotationRolfRepo;

    public List<KnotDto> getGroupedKnots() {
        return knotRepo.getGroupedKnotsRaw().stream()
                .sorted(Comparator
                        .comparingInt((Object[] row) -> Integer.parseInt(((String) row[0]).replaceAll("[^0-9]", "")))
                        .thenComparing(row -> ((String) row[0]).replaceAll("[0-9]", ""))
                )
                .map(row -> new KnotDto(
                        (String) row[0],
                        rolfIndexes(row[1]).stream()
                                .sorted(Comparator.comparingInt(Integer::parseInt))
                                .toList()
                ))
                .toList();
    }

    // array_agg comes back as String[] from Postgres but as Object[] / java.sql.Array from other drivers (e.g. H2).
    private static List<String> rolfIndexes(Object aggregated) {
        Object array = aggregated;
        if (aggregated instanceof java.sql.Array sqlArray) {
            try {
                array = sqlArray.getArray();
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException("Could not read rolf indexes", e);
            }
        }
        return Arrays.stream((Object[]) array).map(String::valueOf).toList();
    }

    public List<FullNotation> getFullNotation(Long knotId) {
        List<FullNotation> rows = fullNotationRolfRepo.findById_KnotId(knotId).stream()
                .map(row -> new FullNotation(row.getId().getKnotId(), row.getId().getCrossingId(), row.getId().getPlacement(),
                        row.getCidBefore(), row.getCidAfter(), row.getStrandBefore(), row.getStrandAfter(), row.getSign()))
                .sorted(Comparator.comparing(FullNotation::getStrandBefore))
                .toList();
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No full notation stored for knot " + knotId);
        }
        return rows;
    }

    public Long getKnotId(String crossing_num, String rolf_index) {
        return knotRepo.findKnotId(crossing_num, rolf_index);
    }

    public Long getDiagramId(String crossing_num, String rolf_index) {
        return knotRepo.findDiagramId(crossing_num, rolf_index);
    }
}
