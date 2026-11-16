package com.knots.backend.services;

import com.knots.backend.models.dtos.InvariantType;
import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.Placement;
import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.experiments.AddPokeRequest;
import com.knots.backend.models.dtos.experiments.AddTwistRequest;
import com.knots.backend.models.dtos.sage.AlexanderResponse;
import com.knots.backend.models.entities.FullNotation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The determinant |Delta(-1)| doesn't change under Reidemeister moves, so it checks that the
// Alexander matrix is built from the right arcs on diagrams the moves produce.
class AlexanderMatrixTest {
    private final FullNotationMovesService moves = new FullNotationMovesService();
    private final LocationSpecifyingService locations = new LocationSpecifyingService(moves);

    // Alternating diagram from a Gauss code: passage i is at cids[i], over on even i.
    private List<FullNotation> diagram(int[] cids, Map<Integer, Sign> signs) {
        int n = cids.length;
        List<FullNotation> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new FullNotation(1L, (long) cids[i], i % 2 == 0 ? Placement.over : Placement.under,
                    (long) cids[(i + n - 1) % n], (long) cids[(i + 1) % n], (long) ((i + n - 1) % n), (long) i, signs.get(cids[i])));
        }
        return rows;
    }

    private List<FullNotation> trefoil() {
        return diagram(new int[]{1, 2, 3, 1, 2, 3}, Map.of(1, Sign.pos, 2, Sign.pos, 3, Sign.pos));
    }

    private List<FullNotation> figureEight() {
        return diagram(new int[]{1, 2, 3, 1, 4, 3, 2, 4}, Map.of(1, Sign.pos, 2, Sign.neg, 3, Sign.neg, 4, Sign.pos));
    }

    /** |det| of the matrix minus its last row and column, with t = -1 (what SageMath reports). */
    static long determinant(List<FullNotation> fnList) {
        SageMathService sage = mock(SageMathService.class);
        when(sage.calculateAlexanderPolynomial(any())).thenReturn(new AlexanderResponse("?", 0L));
        new InvariantCalculationsService(sage).getAllInvariants(fnList);
        ArgumentCaptor<LongPair[][]> captor = ArgumentCaptor.forClass(LongPair[][].class);
        verify(sage).calculateAlexanderPolynomial(captor.capture());
        LongPair[][] matrix = captor.getValue();
        int size = matrix.length - 1;
        if (size == 0) {
            return 1;
        }
        long[][] m = new long[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                m[r][c] = matrix[r][c].x() - matrix[r][c].y();
            }
        }
        // Bareiss elimination: exact integer determinant.
        long sign = 1, previous = 1;
        for (int k = 0; k < size - 1; k++) {
            if (m[k][k] == 0) {
                int swap = k + 1;
                while (swap < size && m[swap][k] == 0) swap++;
                if (swap == size) return 0;
                long[] tmp = m[k]; m[k] = m[swap]; m[swap] = tmp;
                sign = -sign;
            }
            for (int i = k + 1; i < size; i++) {
                for (int j = k + 1; j < size; j++) {
                    m[i][j] = (m[i][j] * m[k][k] - m[i][k] * m[k][j]) / previous;
                }
            }
            previous = m[k][k];
        }
        return Math.abs(sign * m[size - 1][size - 1]);
    }

    @Test
    void storedDiagramsGiveTheirDeterminants() {
        assertThat(determinant(trefoil())).isEqualTo(3);
        assertThat(determinant(figureEight())).isEqualTo(5);
    }

    @Test
    void twistsKeepTheDeterminant() {
        // A twist puts two over-passages in a row: the case the old arc lookup got wrong.
        for (List<FullNotation> knot : List.of(trefoil(), figureEight())) {
            long expected = determinant(knot);
            for (Sign sign : Sign.values()) {
                for (var args : locations.convertRawRequestArgsAll(knot, new AddTwistRequest(null, sign, 1L), moves.orientAllStrands(knot))) {
                    var twisted = moves.addTwist(knot, args.getStrand(), sign);
                    assertThat(determinant(twisted)).as("twist %s on S%d", sign, args.getStrand()).isEqualTo(expected);
                }
            }
        }
    }

    @Test
    void pokesKeepTheDeterminant() {
        for (List<FullNotation> knot : List.of(trefoil(), figureEight())) {
            long expected = determinant(knot);
            for (var args : locations.convertRawRequestArgsAll(knot, new AddPokeRequest(null, 1L), moves.orientAllStrands(knot))) {
                var poked = moves.addPoke(knot, args.getUnderStrandOriented(), args.getOverStrandOriented());
                assertThat(determinant(poked)).isEqualTo(expected);
            }
        }
    }

    @Test
    void aCrossingChangeUnknotsTheTrefoil() {
        assertThat(determinant(moves.switchPlacement(trefoil(), 1L))).isEqualTo(1);
    }

    @Test
    void unknotAndSplitLinksSkipSageMath() {
        SageMathService sage = mock(SageMathService.class);
        var service = new InvariantCalculationsService(sage);
        assertThat(service.getAllInvariants(List.of()).values())
                .containsEntry(InvariantType.Alexander_Polynomial, "1").containsEntry(InvariantType.Determinant, 1L);
        // Two one-crossing loops, one passing entirely over the other: a split link.
        List<FullNotation> split = List.of(
                new FullNotation(1L, 1L, Placement.over, 2L, 2L, 0L, 1L, Sign.pos),
                new FullNotation(1L, 2L, Placement.over, 1L, 1L, 1L, 0L, Sign.pos),
                new FullNotation(1L, 1L, Placement.under, 2L, 2L, 2L, 3L, Sign.pos),
                new FullNotation(1L, 2L, Placement.under, 1L, 1L, 3L, 2L, Sign.pos));
        assertThat(service.getAllInvariants(split).values()).containsEntry(InvariantType.Determinant, 0L);
        verifyNoInteractions(sage);
    }
}
