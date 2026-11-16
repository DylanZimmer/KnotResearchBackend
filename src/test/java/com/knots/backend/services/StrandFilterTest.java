package com.knots.backend.services;

import com.knots.backend.models.dtos.Placement;
import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.experiments.AddTwistRequest;
import com.knots.backend.models.dtos.experiments.CrossingTypeDto;
import com.knots.backend.models.dtos.experiments.StrandTypeDto;
import com.knots.backend.models.entities.FullNotation;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

// crossing_before = the crossing a strand starts at, crossing_after = the one it ends at,
// matching how the frontend fills them in (a strand's first and second crossing).
class StrandFilterTest {
    private final FullNotationMovesService moves = new FullNotationMovesService();
    private final LocationSpecifyingService locations = new LocationSpecifyingService(moves);

    // Figure-eight. Passage i is at crossing cids[i] (over for even i); strand i leaves passage i
    // and arrives at passage i + 1.
    private static final int[] CIDS = {1, 2, 3, 1, 4, 3, 2, 4};
    private static final Map<Integer, Sign> SIGNS = Map.of(1, Sign.pos, 2, Sign.neg, 3, Sign.neg, 4, Sign.pos);

    private List<FullNotation> figureEight() {
        int n = CIDS.length;
        List<FullNotation> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new FullNotation(1L, (long) CIDS[i], i % 2 == 0 ? Placement.over : Placement.under,
                    (long) CIDS[(i + n - 1) % n], (long) CIDS[(i + 1) % n], (long) ((i + n - 1) % n), (long) i, SIGNS.get(CIDS[i])));
        }
        return rows;
    }

    private Set<Long> strands(StrandTypeDto filter) {
        var fn = figureEight();
        return new TreeSet<>(locations.checkStrand(fn, filter, moves.orientAllStrands(fn)).keySet());
    }

    private Set<Long> expected(java.util.function.IntPredicate strand) {
        Set<Long> result = new TreeSet<>();
        for (int i = 0; i < CIDS.length; i++) if (strand.test(i)) result.add((long) i);
        return result;
    }

    @Test
    void crossingBeforeIsWhereTheStrandStarts() {
        assertThat(strands(new StrandTypeDto(new CrossingTypeDto(null, Placement.over), null, null)))
                .as("strands leaving an over passage").isEqualTo(expected(i -> i % 2 == 0));
        assertThat(strands(new StrandTypeDto(new CrossingTypeDto(Sign.pos, null), null, null)))
                .as("strands starting at a positive crossing").isEqualTo(expected(i -> SIGNS.get(CIDS[i]) == Sign.pos));
    }

    @Test
    void crossingAfterIsWhereTheStrandEnds() {
        int n = CIDS.length;
        assertThat(strands(new StrandTypeDto(null, new CrossingTypeDto(null, Placement.over), null)))
                .as("strands arriving at an over passage").isEqualTo(expected(i -> (i + 1) % 2 == 0));
        assertThat(strands(new StrandTypeDto(null, new CrossingTypeDto(Sign.pos, null), null)))
                .as("strands ending at a positive crossing").isEqualTo(expected(i -> SIGNS.get(CIDS[(i + 1) % n]) == Sign.pos));
    }

    @Test
    void bothEndsAndEmptyFilters() {
        int n = CIDS.length;
        // From an over passage at a positive crossing to a negative crossing.
        assertThat(strands(new StrandTypeDto(new CrossingTypeDto(Sign.pos, Placement.over), new CrossingTypeDto(Sign.neg, null), null)))
                .isEqualTo(expected(i -> i % 2 == 0 && SIGNS.get(CIDS[i]) == Sign.pos && SIGNS.get(CIDS[(i + 1) % n]) == Sign.neg));
        assertThat(strands(new StrandTypeDto(new CrossingTypeDto(null, null), null, null))).hasSize(n);
        assertThat(strands(null)).hasSize(n);
    }

    @Test
    void addTwistUsesTheFilter() {
        var fn = figureEight();
        var request = new AddTwistRequest(new StrandTypeDto(new CrossingTypeDto(null, Placement.under), null, null), Sign.pos, 1L);
        assertThat(locations.convertRawRequestArgsAll(fn, request, moves.orientAllStrands(fn)))
                .extracting(args -> args.getStrand()).containsExactly(1L, 3L, 5L, 7L);
    }
}
