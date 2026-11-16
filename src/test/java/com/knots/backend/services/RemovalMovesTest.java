package com.knots.backend.services;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.Placement;
import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.experiments.AddPokeRequest;
import com.knots.backend.models.dtos.experiments.AddTwistRequest;
import com.knots.backend.models.entities.FullNotation;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemovalMovesTest {
    private final FullNotationMovesService moves = new FullNotationMovesService();
    private final LocationSpecifyingService locations = new LocationSpecifyingService(moves);

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

    private List<FullNotation> copyOf(List<FullNotation> rows) {
        return rows.stream().map(fn -> new FullNotation(fn.getKnotId(), fn.getCrossingId(), fn.getPlacement(),
                fn.getCidBefore(), fn.getCidAfter(), fn.getStrandBefore(), fn.getStrandAfter(), fn.getSign())).toList();
    }

    private void assertConsistent(List<FullNotation> rows) {
        Set<Long> strands = new HashSet<>();
        rows.forEach(r -> { strands.add(r.getStrandBefore()); strands.add(r.getStrandAfter()); });
        for (Long strand : strands) {
            var into = rows.stream().filter(r -> r.getStrandAfter().equals(strand)).toList();
            var outOf = rows.stream().filter(r -> r.getStrandBefore().equals(strand)).toList();
            assertThat(into).hasSize(1);
            assertThat(outOf).hasSize(1);
            assertThat(into.get(0).getCidAfter()).isEqualTo(outOf.get(0).getCrossingId());
            assertThat(outOf.get(0).getCidBefore()).isEqualTo(into.get(0).getCrossingId());
        }
        // Ids stay contiguous after a removal.
        assertThat(strands).containsExactlyInAnyOrderElementsOf(
                java.util.stream.LongStream.range(0, strands.size()).boxed().toList());
        assertThat(moves.getOrientedBoundaries(rows)).hasSize(rows.size() / 2 + 2);
    }

    @Test
    void storedKnotsHaveNoTwistsOrPokes() {
        for (var knot : List.of(trefoil(), figureEight())) {
            assertThat(moves.getAllTwisted(knot)).isEmpty();
            assertThat(moves.getAllPoked(knot)).isEmpty();
            assertThat(moves.remPokesAndTwists(knot)).isEqualTo(knot);
        }
    }

    @Test
    void removingATwistGivesBackTheOriginalDiagram() {
        for (var knot : List.of(trefoil(), figureEight())) {
            for (Sign sign : Sign.values()) {
                for (var args : locations.convertRawRequestArgsAll(knot, new AddTwistRequest(null, sign, 1L), moves.orientAllStrands(knot))) {
                    var twisted = moves.addTwist(knot, args.getStrand(), sign);
                    long newCrossing = twisted.stream().mapToLong(FullNotation::getCrossingId).max().orElseThrow();
                    assertThat(moves.getAllTwisted(twisted)).containsExactly(newCrossing);
                    var untwisted = moves.remTwist(twisted, newCrossing);
                    assertThat(new HashSet<>(untwisted)).isEqualTo(new HashSet<>(knot));
                }
            }
        }
    }

    @Test
    void removingAPokeKeepsTheKnot() {
        int pokes = 0;
        for (var knot : List.of(trefoil(), figureEight())) {
            long determinant = AlexanderMatrixTest.determinant(knot);
            for (var args : locations.convertRawRequestArgsAll(knot, new AddPokeRequest(null, 1L), moves.orientAllStrands(knot))) {
                var poked = moves.addPoke(knot, args.getUnderStrandOriented(), args.getOverStrandOriented());
                long first = knot.stream().mapToLong(FullNotation::getCrossingId).max().orElseThrow() + 1;
                LongPair added = new LongPair(first, first + 1);
                assertThat(moves.getAllPoked(poked)).contains(added);
                var before = copyOf(poked);
                var unpoked = moves.remPoke(poked, added.x(), added.y());
                assertThat(poked).as("input untouched").isEqualTo(before);
                assertThat(unpoked).hasSameSizeAs(knot);
                assertConsistent(unpoked);
                assertThat(AlexanderMatrixTest.determinant(unpoked)).isEqualTo(determinant);
                pokes++;
            }
        }
        assertThat(pokes).isGreaterThan(10);
    }

    @Test
    void removingEveryTwistAndPokeSimplifiesBackToTheKnot() {
        var knot = trefoil();
        var cluttered = moves.addTwist(knot, 0L, Sign.neg);
        cluttered = moves.addTwist(cluttered, 4L, Sign.pos);
        var pokeArgs = locations.convertRawRequestArgsSingle(cluttered, new AddPokeRequest(null, 1L), moves.orientAllStrands(cluttered));
        cluttered = moves.addPoke(cluttered, pokeArgs.getUnderStrandOriented(), pokeArgs.getOverStrandOriented());
        assertThat(cluttered).hasSize(14);

        var simplified = moves.remPokesAndTwists(cluttered);
        assertThat(simplified).hasSize(6);
        assertConsistent(simplified);
        assertThat(moves.getAllTwisted(simplified)).isEmpty();
        assertThat(moves.getAllPoked(simplified)).isEmpty();
        assertThat(AlexanderMatrixTest.determinant(simplified)).isEqualTo(3);
    }

    @Test
    void theOneCrossingLoopUntwistsToTheUnknot() {
        var loop = List.of(
                new FullNotation(1L, 1L, Placement.over, 1L, 1L, 0L, 1L, Sign.pos),
                new FullNotation(1L, 1L, Placement.under, 1L, 1L, 1L, 0L, Sign.pos));
        assertThat(moves.getAllTwisted(loop)).containsExactly(1L);
        assertThat(moves.remTwist(loop, 1L)).isEmpty();
        assertThat(moves.remPokesAndTwists(loop)).isEmpty();
    }

    @Test
    void experimentsBranchOnTwistRemoval() {
        // Trefoil plus two positive twists: writhe 5. Each twist removed lowers it by one.
        var start = moves.addTwist(moves.addTwist(trefoil(), 0L, Sign.pos), 4L, Sign.pos);
        var states = org.mockito.Mockito.mock(com.knots.backend.repositories.ExperimentStatesRepo.class);
        var notation = org.mockito.Mockito.mock(com.knots.backend.repositories.ExperimentsFNRepo.class);
        var experiments = org.mockito.Mockito.mock(com.knots.backend.repositories.ExperimentsRepo.class);
        org.mockito.Mockito.when(experiments.findById(1L)).thenReturn(Optional.of(new com.knots.backend.models.entities.Experiments()));
        org.mockito.Mockito.when(states.findOgKnotIdsByExperimentIdAndStateNum(1L, 0L)).thenReturn(List.of(1L));
        org.mockito.Mockito.when(states.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(
                new com.knots.backend.models.entities.ExperimentStates(new com.knots.backend.models.keys.ExperimentStatesKey(1L, 1L, 0L, 0L),
                        com.knots.backend.models.dtos.MoveType.startExperiment, "Starting", new com.knots.backend.models.dtos.InvariantsDto(null, null, 5L))));
        org.mockito.Mockito.when(notation.findFnListFromState(1L, 1L, 0L, 0L)).thenReturn(start);
        var invariants = org.mockito.Mockito.mock(InvariantCalculationsService.class);
        org.mockito.Mockito.when(invariants.getAllInvariants(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> {
            List<FullNotation> rows = call.getArgument(0);
            return new com.knots.backend.models.dtos.InvariantsDto(null, null,
                    rows.stream().filter(r -> r.getPlacement() == Placement.over).mapToLong(r -> r.getSign().value()).sum());
        });
        var service = new ExperimentsService(experiments, org.mockito.Mockito.mock(com.knots.backend.repositories.FullNotationRolfRepo.class),
                org.mockito.Mockito.mock(com.knots.backend.repositories.InvariantRolfRepo.class), notation, states, invariants, moves, locations);

        var goalStates = service.runExperimentWithGoal(1L, List.of(new com.knots.backend.models.dtos.experiments.RemTwistRequest(null, 1L)),
                new com.knots.backend.models.dtos.InvariantsDto(null, null, 3L), true);

        // Two twists to remove, in either order: 2 branches, then 1 each.
        assertThat(goalStates).hasSize(2);
        assertThat(goalStates).allMatch(s -> s.getId().getStateNum() == 2L && s.getMoveType() == com.knots.backend.models.dtos.MoveType.remTwist);
        assertThat(goalStates).allMatch(s -> s.getMoveDetail().startsWith("cid="));
    }

    @Test
    void removingSomethingThatIsNotThereIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> moves.remTwist(trefoil(), 1L));
        assertThrows(IllegalArgumentException.class, () -> moves.remPoke(trefoil(), 1L, 2L));
    }
}
