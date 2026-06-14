package com.knots.backend.services;

import com.knots.backend.models.dtos.KnotDto;
import com.knots.backend.repositories.KnotRepo;

import org.springframework.stereotype.Service;
import java.util.*;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KnotService {

    private final KnotRepo knotRepo;

    public List<KnotDto> getGroupedKnots() {
        return knotRepo.getGroupedKnotsRaw().stream()
                .sorted(Comparator
                        .comparingInt((Object[] row) -> Integer.parseInt(((String) row[0]).replaceAll("[^0-9]", "")))
                        .thenComparing(row -> ((String) row[0]).replaceAll("[0-9]", ""))
                )
                .map(row -> new KnotDto(
                        (String) row[0],
                        Arrays.asList((String[]) row[1]).stream()
                                .sorted(Comparator.comparingInt(Integer::parseInt))
                                .toList()
                ))
                .toList();
    }

    public Long getKnotId(String crossing_num, String rolf_index) {
        return knotRepo.findKnotId(crossing_num, rolf_index);
    }

    public Long getDiagramId(String crossing_num, String rolf_index) {
        return knotRepo.findDiagramId(crossing_num, rolf_index);
    }
}
