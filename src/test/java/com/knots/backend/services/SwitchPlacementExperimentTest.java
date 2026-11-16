package com.knots.backend.services;

import com.knots.backend.models.dtos.InvariantsDto;
import com.knots.backend.models.dtos.MoveType;
import com.knots.backend.models.dtos.Placement;
import com.knots.backend.models.dtos.Sign;
import com.knots.backend.models.dtos.experiments.CrossingTypeDto;
import com.knots.backend.models.dtos.experiments.MoveRequest;
import com.knots.backend.models.dtos.experiments.SwitchPlacementRequest;
import com.knots.backend.models.entities.ExperimentStates;
import com.knots.backend.models.entities.Experiments;
import com.knots.backend.models.entities.FullNotation;
import com.knots.backend.models.keys.ExperimentStatesKey;
import com.knots.backend.repositories.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Switch Crossing Placement as an experiment move: the un-knotting experiment.
class SwitchPlacementExperimentTest {
    private final FullNotationMovesService moves = new FullNotationMovesService();
    private final LocationSpecifyingService locations = new LocationSpecifyingService(moves);

    private List<FullNotation> trefoil() {
        int[] cids = {1, 2, 3, 1, 2, 3};
        int n = cids.length;
        List<FullNotation> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new FullNotation(1L, (long) cids[i], i % 2 == 0 ? Placement.over : Placement.under,
                    (long) cids[(i + n - 1) % n], (long) cids[(i + 1) % n], (long) ((i + n - 1) % n), (long) i, Sign.pos));
        }
        return rows;
    }

    // An ExperimentsService on one knot (the trefoil), with the determinant worked out for real.
    private ExperimentsService service() {
        var states = mock(ExperimentStatesRepo.class);
        var notation = mock(ExperimentsFNRepo.class);
        var experiments = mock(ExperimentsRepo.class);
        when(experiments.findById(1L)).thenReturn(Optional.of(new Experiments()));
        when(states.findOgKnotIdsByExperimentIdAndStateNum(1L, 0L)).thenReturn(List.of(1L));
        when(states.findById(any())).thenReturn(Optional.of(new ExperimentStates(new ExperimentStatesKey(1L, 1L, 0L, 0L),
                MoveType.startExperiment, "Starting", new InvariantsDto(null, 3L, null))));
        when(notation.findFnListFromState(1L, 1L, 0L, 0L)).thenReturn(trefoil());
        var invariants = mock(InvariantCalculationsService.class);
        when(invariants.getAllInvariants(any())).thenAnswer(call ->
                new InvariantsDto(null, AlexanderMatrixTest.determinant(call.getArgument(0)), null));
        return new ExperimentsService(experiments, mock(FullNotationRolfRepo.class), mock(InvariantRolfRepo.class),
                notation, states, invariants, moves, locations);
    }

    @Test
    void anyCrossingFitsWithoutASignFilter() {
        var request = new SwitchPlacementRequest(new CrossingTypeDto(null, null), 1L);
        assertThat(locations.convertRawRequestArgsAll(trefoil(), request, moves.orientAllStrands(trefoil())))
                .extracting(args -> args.getCid()).containsExactly(1L, 2L, 3L);
        var negativeOnly = new SwitchPlacementRequest(new CrossingTypeDto(Sign.neg, null), 1L);
        assertThat(locations.convertRawRequestArgsAll(trefoil(), negativeOnly, moves.orientAllStrands(trefoil()))).isEmpty();
    }

    @Test
    void branchingFindsEveryOneCrossingUnknotting() {
        List<MoveRequest<?, ?>> steps = List.of(new SwitchPlacementRequest(null, 1L));
        var goalStates = service().runExperimentWithGoal(1L, steps, new InvariantsDto(null, 1L, null), true);

        // Changing any one of the trefoil's three crossings unknots it.
        assertThat(goalStates).hasSize(3);
        assertThat(goalStates).allMatch(s -> s.getId().getStateNum() == 1L && s.getMoveType() == MoveType.switchPlacement);
        assertThat(goalStates).extracting(ExperimentStates::getMoveDetail).containsExactlyInAnyOrder("cid=1", "cid=2", "cid=3");
    }

    @Test
    void everywhereAtOnceSwitchesEveryCrossing() {
        // Switching all three crossings gives the mirror trefoil: still determinant 3, so no goal.
        List<MoveRequest<?, ?>> steps = List.of(new SwitchPlacementRequest(null, 0L));
        var goalStates = service().runExperimentWithGoal(1L, steps, new InvariantsDto(null, 1L, null), false);
        assertThat(goalStates).isEmpty();
        assertThat(moves.switchPlacement(moves.switchPlacement(moves.switchPlacement(trefoil(), 1L), 2L), 3L))
                .isEqualTo(moves.mirror(trefoil()));
    }
}
