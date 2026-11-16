package com.knots.backend.services;

import com.knots.backend.models.dtos.*;
import com.knots.backend.models.dtos.experiments.*;
import com.knots.backend.models.entities.*;
import com.knots.backend.models.keys.ExperimentStatesKey;
import com.knots.backend.repositories.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlideAndCrossingChangeTest {
    private final FullNotationMovesService moves = new FullNotationMovesService();
    private final LocationSpecifyingService locations = new LocationSpecifyingService(moves);

    private List<FullNotation> loop() {
        return List.of(
                new FullNotation(1L, 1L, Placement.over, 1L, 1L, 0L, 1L, Sign.pos),
                new FullNotation(1L, 1L, Placement.under, 1L, 1L, 1L, 0L, Sign.pos));
    }

    // Every diagram reachable from the loop with two pokes / twists. Plenty of these contain
    // slideable triangles.
    private List<List<FullNotation>> smallDiagrams() {
        List<List<FullNotation>> all = new ArrayList<>();
        List<List<FullNotation>> level = List.of(loop());
        for (int depth = 0; depth < 2; depth++) {
            List<List<FullNotation>> next = new ArrayList<>();
            for (var d : level) {
                for (var args : locations.convertRawRequestArgsAll(d, new AddPokeRequest(null, 1L), moves.orientAllStrands(d))) {
                    next.add(moves.addPoke(d, args.getUnderStrandOriented(), args.getOverStrandOriented()));
                }
                for (Sign sign : Sign.values()) {
                    for (var args : locations.convertRawRequestArgsAll(d, new AddTwistRequest(null, sign, 1L), moves.orientAllStrands(d))) {
                        next.add(moves.addTwist(d, args.getStrand(), args.getSign()));
                    }
                }
            }
            all.addAll(next);
            level = next;
        }
        return all;
    }

    private List<List<Long>> strandBoundaries(List<FullNotation> fn) {
        return moves.getOrientedBoundaries(fn).stream().map(b -> b.stream().map(LongPair::x).toList()).toList();
    }

    // Each strand runs from exactly one crossing to exactly one crossing, and cidBefore / cidAfter agree.
    private void assertConsistent(List<FullNotation> rows) {
        Set<Long> strands = new HashSet<>();
        rows.forEach(r -> { strands.add(r.getStrandBefore()); strands.add(r.getStrandAfter()); });
        for (Long strand : strands) {
            var into = rows.stream().filter(r -> r.getStrandAfter().equals(strand)).toList();
            var outOf = rows.stream().filter(r -> r.getStrandBefore().equals(strand)).toList();
            assertThat(into).as("rows ending strand %s", strand).hasSize(1);
            assertThat(outOf).as("rows starting strand %s", strand).hasSize(1);
            assertThat(into.get(0).getCidAfter()).isEqualTo(outOf.get(0).getCrossingId());
            assertThat(outOf.get(0).getCidBefore()).isEqualTo(into.get(0).getCrossingId());
        }
    }

    private long writhe(List<FullNotation> rows) {
        return rows.stream().filter(r -> r.getPlacement() == Placement.over).mapToLong(r -> r.getSign().value()).sum();
    }

    private Set<Long> crossingsOn(List<FullNotation> fn, Long strand) {
        Set<Long> cids = new HashSet<>();
        for (var r : fn) {
            if (r.getStrandBefore().equals(strand) || r.getStrandAfter().equals(strand)) cids.add(r.getCrossingId());
        }
        return cids;
    }

    @Test
    void twistsKeepCrossingLinksConsistent() {
        // addTwist used to swap cidBefore / cidAfter on the new crossing; only visible on 2+ crossings.
        smallDiagrams().forEach(this::assertConsistent);
    }

    @Test
    void slideIsAValidReidemeisterThreeMove() {
        int triangles = 0;
        for (var diagram : smallDiagrams()) {
            for (var triangle : moves.slideableList(diagram, strandBoundaries(diagram))) {
                if (!moves.isSlideableTriangle(diagram, triangle)) {
                    continue;
                }
                triangles++;
                var copyOfInput = new ArrayList<>(diagram.stream().map(r -> new FullNotation(r.getKnotId(), r.getCrossingId(),
                        r.getPlacement(), r.getCidBefore(), r.getCidAfter(), r.getStrandBefore(), r.getStrandAfter(), r.getSign())).toList());
                var slid = moves.slideTriangle(diagram, triangle);

                assertThat(diagram).as("input untouched").isEqualTo(copyOfInput);
                assertConsistent(slid);
                assertThat(slid).hasSameSizeAs(diagram);
                assertThat(new HashSet<>(slid)).as("diagram changed").isNotEqualTo(new HashSet<>(diagram));
                assertThat(writhe(slid)).isEqualTo(writhe(diagram));
                assertThat(moves.getOrientedBoundaries(slid)).as("still planar").hasSize(slid.size() / 2 + 2);
                assertThat(moves.slideableList(slid, strandBoundaries(slid)))
                        .as("the triangle is still a face").anyMatch(t -> new HashSet<>(t).equals(new HashSet<>(triangle)));
                assertThat(new HashSet<>(moves.slideTriangle(slid, triangle))).as("sliding back undoes it")
                        .isEqualTo(new HashSet<>(diagram));

                // The (strand, crossing) form used by /Slide gives the same result for any strand.
                for (Long strand : triangle) {
                    var others = triangle.stream().filter(t -> !t.equals(strand)).toList();
                    Set<Long> opposite = crossingsOn(diagram, others.get(0));
                    opposite.retainAll(crossingsOn(diagram, others.get(1)));
                    assertThat(new HashSet<>(moves.Slide(diagram, strand, opposite.iterator().next())))
                            .isEqualTo(new HashSet<>(slid));
                }
            }
        }
        assertThat(triangles).isGreaterThan(20);
    }

    @Test
    void slideLocationsComeFromSlideableList() {
        int found = 0;
        for (var diagram : smallDiagrams()) {
            var args = locations.convertRawRequestArgsAll(diagram, new SlideRequest(null, 1L), moves.orientAllStrands(diagram));
            for (var arg : args) {
                assertThat(moves.isSlideableTriangle(diagram, arg.getTriangleBdy())).isTrue();
                assertThat(moves.slideableList(diagram, strandBoundaries(diagram)))
                        .anyMatch(t -> new HashSet<>(t).equals(new HashSet<>(arg.getTriangleBdy())));
            }
            found += args.size();
            // A filter no crossing can match leaves nothing.
            var impossible = new SlideLoc(new CrossingTypeDto(Sign.pos, null), null);
            var allNegative = moves.mirror(diagram).stream().allMatch(r -> r.getSign() == Sign.neg);
            if (allNegative) {
                var mirrored = moves.mirror(diagram);
                assertThat(locations.convertRawRequestArgsAll(mirrored, new SlideRequest(impossible, 1L), moves.orientAllStrands(mirrored))).isEmpty();
            }
        }
        assertThat(found).isGreaterThan(20);
    }

    @Test
    void slideRequestsDeserializeAsSlideRequests() throws Exception {
        MoveRequest<?, ?> request = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                "{\"type\":\"slide\",\"location\":{\"crossing\":{\"sign\":\"neg\"}},\"numTimes\":1}", MoveRequest.class);
        assertThat(request).isInstanceOf(SlideRequest.class);
        assertThat(((SlideRequest) request).location().crossing().sign()).isEqualTo(Sign.neg);
    }

    @Test
    void experimentsCanSlide() {
        var diagram = smallDiagrams().stream()
                .filter(d -> !locations.convertRawRequestArgsAll(d, new SlideRequest(null, 1L), moves.orientAllStrands(d)).isEmpty())
                .findFirst().orElseThrow();
        var states = mock(ExperimentStatesRepo.class);
        var notation = mock(ExperimentsFNRepo.class);
        var experiments = mock(ExperimentsRepo.class);
        when(experiments.findById(1L)).thenReturn(Optional.of(new Experiments()));
        when(states.findOgKnotIdsByExperimentIdAndStateNum(1L, 0L)).thenReturn(List.of(1L));
        when(states.findById(any())).thenReturn(Optional.of(new ExperimentStates(new ExperimentStatesKey(1L, 1L, 0L, 0L),
                MoveType.startExperiment, "Starting", new InvariantsDto(null, null, writhe(diagram)))));
        when(notation.findFnListFromState(1L, 1L, 0L, 0L)).thenReturn(diagram);
        var invariants = mock(InvariantCalculationsService.class);
        when(invariants.getAllInvariants(any())).thenAnswer(call -> new InvariantsDto(null, 7L, writhe(call.getArgument(0))));
        var service = new ExperimentsService(experiments, mock(FullNotationRolfRepo.class), mock(InvariantRolfRepo.class),
                notation, states, invariants, moves, locations);

        // Determinant 7 is what the stand-in returns for any moved diagram, so one slide reaches it.
        var result = service.runExperimentWithGoal(1L, List.of(new SlideRequest(null, 1L)),
                new InvariantsDto(null, 7L, null), true);

        assertThat(result).isNotEmpty().allMatch(s -> s.getMoveType() == MoveType.slide && s.getMoveDetail().startsWith("triangle="));
        var saved = ArgumentCaptor.forClass(List.class);
        verify(notation, atLeastOnce()).saveAll(saved.capture());
        for (Object rows : saved.getAllValues()) {
            @SuppressWarnings("unchecked")
            var efn = (List<ExperimentsFullNotation>) rows;
            assertConsistent(efn.stream().map(e -> new FullNotation(1L, e.getId().getCrossingId(), e.getId().getPlacement(),
                    e.getCidBefore(), e.getCidAfter(), e.getStrandBefore(), e.getStrandAfter(), e.getSign())).toList());
        }
    }

    @Test
    void switchPlacementChangesOneCrossing() {
        var diagram = moves.addTwist(loop(), 0L, Sign.pos);
        var switched = moves.switchPlacement(diagram, 1L);

        assertThat(diagram).as("input untouched").isEqualTo(moves.addTwist(loop(), 0L, Sign.pos));
        for (int i = 0; i < diagram.size(); i++) {
            var before = diagram.get(i);
            var after = switched.get(i);
            if (before.getCrossingId().equals(1L)) {
                assertThat(after.getPlacement()).isNotEqualTo(before.getPlacement());
                assertThat(after.getSign()).isEqualTo(before.getSign().opposite());
            } else {
                assertThat(after).isEqualTo(before);
            }
            assertThat(after.getStrandBefore()).isEqualTo(before.getStrandBefore());
            assertThat(after.getStrandAfter()).isEqualTo(before.getStrandAfter());
        }
        assertConsistent(switched);
        // Still exactly one over and one under row per crossing.
        assertThat(switched.stream().filter(r -> r.getCrossingId().equals(1L)).map(FullNotation::getPlacement))
                .containsExactlyInAnyOrder(Placement.over, Placement.under);
        assertThat(writhe(switched)).isEqualTo(writhe(diagram) - 2);
        assertThat(moves.switchPlacement(switched, 1L)).as("switching twice undoes it").isEqualTo(diagram);
    }
}
