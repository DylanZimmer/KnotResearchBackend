package com.knots.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knots.backend.models.dtos.*;
import com.knots.backend.models.dtos.experiments.*;
import com.knots.backend.models.entities.*;
import com.knots.backend.repositories.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExperimentMovesTest {
    private final FullNotationMovesService moves = new FullNotationMovesService();
    private final LocationSpecifyingService locations = new LocationSpecifyingService(moves);

    private List<FullNotation> loop() {
        return List.of(
                new FullNotation(1L, 1L, Placement.over, 1L, 1L, 0L, 1L, Sign.pos),
                new FullNotation(1L, 1L, Placement.under, 1L, 1L, 1L, 0L, Sign.pos));
    }

    private void assertConnected(List<FullNotation> rows) {
        Set<Long> strands = new HashSet<>();
        rows.forEach(row -> { strands.add(row.getStrandBefore()); strands.add(row.getStrandAfter()); });
        for (Long strand : strands) {
            var sources = rows.stream().filter(row -> row.getStrandAfter().equals(strand)).toList();
            var targets = rows.stream().filter(row -> row.getStrandBefore().equals(strand)).toList();
            assertThat(sources).hasSize(1);
            assertThat(targets).hasSize(1);
            assertThat(sources.get(0).getCidAfter()).isEqualTo(targets.get(0).getCrossingId());
            assertThat(targets.get(0).getCidBefore()).isEqualTo(sources.get(0).getCrossingId());
        }
        for (Long crossing : rows.stream().map(FullNotation::getCrossingId).distinct().toList()) {
            var pair = rows.stream().filter(row -> row.getCrossingId().equals(crossing)).toList();
            assertThat(pair).hasSize(2);
            assertThat(pair.get(0).getSign()).isEqualTo(pair.get(1).getSign());
            assertThat(pair).extracting(FullNotation::getPlacement).containsExactlyInAnyOrder(Placement.over, Placement.under);
        }
    }

    @Test
    void pokeKeepsConnectivityForBothOrdersAndDirections() {
        for (long under = 0; under < 2; under++) {
            for (long direction = 0; direction < 2; direction++) {
                var original = loop();
                var output = moves.addPoke(original, new LongPair(under, direction), new LongPair(1 - under, 1L));
                assertThat(output).hasSize(6);
                assertConnected(output);
                assertThat(original).isEqualTo(loop());
                assertThat(moves.getOrientedBoundaries(output)).isNotEmpty();
            }
        }
    }

    @Test
    void saddleHandlesSharedRowsWithoutMutatingInput() {
        var original = loop();
        var output = moves.saddle(original, 0L, 1L);
        assertConnected(output);
        assertThat(output).hasSize(2);
        assertThat(original).isEqualTo(loop());
        assertThat(output.get(0).getStrandBefore()).isEqualTo(1L);
        assertThat(output.get(1).getStrandBefore()).isEqualTo(0L);
    }

    // Standard trefoil: three crossings, alternating over / under.
    private List<FullNotation> trefoil() {
        long[] cids = {1, 2, 3, 1, 2, 3};
        List<FullNotation> rows = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            rows.add(new FullNotation(1L, cids[i], i % 2 == 0 ? Placement.over : Placement.under,
                    cids[(i + 5) % 6], cids[(i + 1) % 6], (long) ((i + 5) % 6), (long) i, Sign.pos));
        }
        return rows;
    }

    @Test
    void bothRequestsDeserializeSelectAndPersistOneMove() throws Exception {
        for (String type : List.of("addPoke", "saddle")) {
            // The one-crossing loop has no saddle (its outer region's two strands run opposite ways
            // around it), so saddles are tried on the trefoil's triangular regions.
            boolean saddle = type.equals("saddle");
            List<FullNotation> diagram = saddle ? trefoil() : loop();
            MoveRequest<?, ?> request = new ObjectMapper().readValue(
                    "{\"type\":\"" + type + "\",\"location\":{\"numCrossings\":" + (saddle ? 3 : 2) + "},\"numTimes\":1}", MoveRequest.class);
            assertThat(request.type().name()).isEqualTo(type);
            assertThat(request.location()).isInstanceOf(BoundaryTypeDto.class);
            var args = locations.convertRawRequestArgsSingle(diagram, request, moves.orientAllStrands(diagram));
            assertThat(args).isNotNull();
            if (saddle) {
                var output = moves.saddle(diagram, args.getStrand1(), args.getStrand2());
                assertConnected(output);
                // Still planar: faces = crossings + 2 (one piece) or + 3 (two pieces).
                assertThat(moves.getOrientedBoundaries(output).size()).isIn(output.size() / 2 + 2, output.size() / 2 + 3);
            }
            if (request instanceof AddPokeRequest) {
                var output = moves.addPoke(loop(), args.getUnderStrandOriented(), args.getOverStrandOriented());
                assertThat(moves.getOrientedBoundaries(output)).hasSize(5);
            }
            var states = mock(ExperimentStatesRepo.class);
            when(states.findById(any())).thenReturn(Optional.of(new ExperimentStates(
                    new com.knots.backend.models.keys.ExperimentStatesKey(1L, 1L, 0L, 0L),
                    MoveType.startExperiment, "Starting", new InvariantsDto("1", 1L, 1L))));
            when(states.existsById(any())).thenReturn(false);
            var notation = mock(ExperimentsFNRepo.class);
            when(notation.findFnListFromState(1L, 1L, 0L, 0L)).thenReturn(diagram);
            var invariants = mock(InvariantCalculationsService.class);
            when(invariants.getAllInvariants(any())).thenAnswer(call -> {
                assertConnected(call.getArgument(0));
                return new InvariantsDto("1", 1L, 1L);
            });
            var service = new ExperimentsService(mock(ExperimentsRepo.class), mock(FullNotationRolfRepo.class),
                    mock(InvariantRolfRepo.class), notation, states, invariants, moves, locations);
            service.nextMoveSingleInstance(1L, 1L, 0L, 0L, request);
            verify(notation).saveAll(any());
            verify(states).save(argThat(state -> state.getId().getStateNum() == 1L && state.getMoveType() == request.type()));
        }
    }

    @Test
    void everySelectedPokePreservesPlanarFaces() {
        var diagrams = new ArrayList<List<FullNotation>>();
        diagrams.add(loop());
        diagrams.add(moves.orientationFlip(loop()));
        diagrams.add(moves.mirror(loop()));
        // Exercise boundaries with more than two strands and both relative directions.
        var first = locations.convertRawRequestArgsSingle(loop(), new AddPokeRequest(null, 1L), moves.orientAllStrands(loop()));
        diagrams.add(moves.addPoke(loop(), first.getUnderStrandOriented(), first.getOverStrandOriented()));
        for (var diagram : diagrams) {
            for (var args : locations.convertRawRequestArgsAll(diagram, new AddPokeRequest(null, 1L), moves.orientAllStrands(diagram))) {
                var output = moves.addPoke(diagram, args.getUnderStrandOriented(), args.getOverStrandOriented());
                assertConnected(output);
                assertThat(moves.getOrientedBoundaries(output)).hasSize(output.size() / 2 + 2);
            }
        }
    }

    @Test
    void saddleChoicesKeepTheDiagramPlanar() {
        var knots = new ArrayList<List<FullNotation>>();
        knots.add(trefoil());
        knots.add(moves.mirror(trefoil()));
        for (var args : locations.convertRawRequestArgsAll(trefoil(), new AddPokeRequest(null, 1L), moves.orientAllStrands(trefoil()))) {
            knots.add(moves.addPoke(trefoil(), args.getUnderStrandOriented(), args.getOverStrandOriented()));
        }
        int saddles = 0;
        for (var knot : knots) {
            for (var args : locations.convertRawRequestArgsAll(knot, new SaddleRequest(null, 1L), moves.orientAllStrands(knot))) {
                var output = moves.saddle(knot, args.getStrand1(), args.getStrand2());
                assertConnected(output);
                assertThat(moves.getOrientedBoundaries(output).size()).isIn(output.size() / 2 + 2, output.size() / 2 + 3);
                saddles++;
            }
        }
        assertThat(saddles).isGreaterThan(10);
    }

    @Test
    void bothSelectorsHonorBoundarySize() {
        var filter = new BoundaryTypeDto(null, null, 99);
        assertThat(locations.identifyAllPossiblePokes(loop(), filter, moves.orientAllStrands(loop()), moves.getOrientedBoundaries(loop()))).isEmpty();
        assertThat(locations.identifyAllPossibleSaddles(loop(), filter, moves.orientAllStrands(loop()), moves.getOrientedBoundaries(loop()))).isEmpty();
    }
}
