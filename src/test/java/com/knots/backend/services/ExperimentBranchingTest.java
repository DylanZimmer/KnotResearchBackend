package com.knots.backend.services;

import com.knots.backend.models.converters.InvariantsConverter;
import com.knots.backend.models.dtos.*;
import com.knots.backend.models.dtos.experiments.*;
import com.knots.backend.models.entities.*;
import com.knots.backend.models.keys.ExperimentStatesKey;
import com.knots.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExperimentBranchingTest {
    private final FullNotationMovesService moves = new FullNotationMovesService();
    private final LocationSpecifyingService locations = new LocationSpecifyingService(moves);

    private ExperimentStatesRepo states;
    private ExperimentsFNRepo notation;
    private ExperimentsRepo experiments;
    private Experiments experiment;
    private ExperimentsService service;

    // One crossing, two strands (0 and 1), writhe 1.
    private List<FullNotation> loop() {
        return List.of(
                new FullNotation(1L, 1L, Placement.over, 1L, 1L, 0L, 1L, Sign.pos),
                new FullNotation(1L, 1L, Placement.under, 1L, 1L, 1L, 0L, Sign.pos));
    }

    private static InvariantsDto writhe(long writhe) {
        return new InvariantsDto(Map.of(InvariantType.Writhe, writhe));
    }

    @BeforeEach
    void setUp() {
        states = mock(ExperimentStatesRepo.class);
        notation = mock(ExperimentsFNRepo.class);
        experiments = mock(ExperimentsRepo.class);
        experiment = new Experiments();
        when(experiments.findById(1L)).thenReturn(Optional.of(experiment));
        when(states.findOgKnotIdsByExperimentIdAndStateNum(1L, 0L)).thenReturn(List.of(1L));
        when(states.findById(any())).thenReturn(Optional.of(new ExperimentStates(
                new ExperimentStatesKey(1L, 1L, 0L, 0L), MoveType.startExperiment, "Starting", writhe(1L))));
        when(notation.findFnListFromState(1L, 1L, 0L, 0L)).thenReturn(loop());

        // Stand-in for SageMath: only writhe, computed straight from the diagram.
        var invariants = mock(InvariantCalculationsService.class);
        when(invariants.getAllInvariants(any())).thenAnswer(call -> {
            List<FullNotation> rows = call.getArgument(0);
            return writhe(rows.stream().filter(r -> r.getPlacement() == Placement.over)
                    .mapToLong(r -> r.getSign().value()).sum());
        });
        service = new ExperimentsService(experiments, mock(FullNotationRolfRepo.class), mock(InvariantRolfRepo.class),
                notation, states, invariants, moves, locations);
    }

    private List<ExperimentStates> savedStates() {
        var captor = ArgumentCaptor.forClass(ExperimentStates.class);
        verify(states, atLeast(0)).save(captor.capture());
        return captor.getAllValues().stream().filter(s -> s.getId().getStateNum() > 0).toList();
    }

    @Test
    void branchMakesOneSubstatePerFittingLocationAndStopsAtFirstGoalLevel() {
        // numTimes 3 allows three levels, but writhe 2 is reached after one twist.
        var result = service.runExperimentWithGoal(1L,
                List.of(new AddTwistRequest(null, Sign.pos, 3L)), writhe(2L), true);

        var saved = savedStates();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(s -> s.getId().getStateNum() == 1L && s.getParentSubStateNum() == 0L);
        assertThat(saved).extracting(s -> s.getId().getSubStateNum()).containsExactly(0L, 1L);
        assertThat(saved).extracting(ExperimentStates::getMoveDetail)
                .containsExactly("strand=0, sign=pos", "strand=1, sign=pos");
        assertThat(result).hasSize(2).allMatch(s -> s.getExperimentStatus().equals(ExperimentsService.STATUS_GOAL_REACHED));
        assertThat(experiment.getExperimentStatus()).isEqualTo(ExperimentsService.EXPERIMENT_GOAL_REACHED);
    }

    @Test
    void branchTreeGrowsPerLevelAndTracksParents() {
        var result = service.runExperimentWithGoal(1L,
                List.of(new AddTwistRequest(null, Sign.pos, 2L)), writhe(3L), true);

        var saved = savedStates();
        var level1 = saved.stream().filter(s -> s.getId().getStateNum() == 1L).toList();
        var level2 = saved.stream().filter(s -> s.getId().getStateNum() == 2L).toList();
        assertThat(level1).hasSize(2);
        // Each level-1 diagram has 4 strands, so 4 twist locations each.
        assertThat(level2).hasSize(8);
        assertThat(level2).extracting(s -> s.getId().getSubStateNum()).containsExactly(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L);
        assertThat(level2).extracting(ExperimentStates::getParentSubStateNum).containsExactly(0L, 0L, 0L, 0L, 1L, 1L, 1L, 1L);
        assertThat(result).hasSize(8);
        // Each saved substate gets its own full-notation rows.
        verify(notation, times(10)).saveAll(any());
    }

    @Test
    void unreachableBranchingGoalStopsAtTheBranchLimit() {
        // The list repeats, so the tree keeps growing: 2, 8, 48, 384, 3840 states, then over 5000.
        var result = service.runExperimentWithGoal(1L,
                List.of(new AddTwistRequest(null, Sign.pos, 2L)), writhe(99L), true);

        assertThat(result).isEmpty();
        assertThat(savedStates()).hasSize(2 + 8 + 48 + 384 + 3840);
        assertThat(experiment.getExperimentStatus()).isEqualTo(ExperimentsService.EXPERIMENT_BRANCH_LIMIT);
    }

    @Test
    void noBranchRepeatsTheMoveListUntilTheGoalIsHit() {
        // One twist per pass: writhe 1 -> 2 -> 3 -> 4 takes three passes through the list.
        var result = service.runExperimentWithGoal(1L,
                List.of(new AddTwistRequest(null, Sign.pos, null)), writhe(4L), false);

        assertThat(savedStates()).hasSize(3);
        assertThat(result).singleElement().satisfies(s -> assertThat(s.getId().getStateNum()).isEqualTo(3L));
        assertThat(experiment.getExperimentStatus()).isEqualTo(ExperimentsService.EXPERIMENT_GOAL_REACHED);
    }

    @Test
    void numTimesAppliesTheMoveThatManyTimesBeforeTheNextMove() {
        // twist x2 then mirror: writhe 1 -> 2 -> 3 -> -3. Goal -3 is only reached after both twists.
        var result = service.runExperimentWithGoal(1L,
                List.of(new AddTwistRequest(null, Sign.pos, 2L), new NoArgsRequest(MoveType.mirror)), writhe(-3L), false);

        var saved = savedStates();
        assertThat(saved).extracting(ExperimentStates::getMoveType)
                .containsExactly(MoveType.addTwist, MoveType.addTwist, MoveType.mirror);
        // The location is found again on the twisted diagram (it now has 4 strands, not 2).
        assertThat(saved.get(1).getInvariants()).isEqualTo(writhe(3L));
        assertThat(result).singleElement().satisfies(s -> assertThat(s.getId().getStateNum()).isEqualTo(3L));
    }

    @Test
    void unreachableLinearGoalStopsAfter1000Iterations() {
        // Mirror flips writhe between -1 and 1 forever, so writhe 5 is never reached.
        var result = service.runExperimentWithGoal(1L,
                List.of(new NoArgsRequest(MoveType.mirror)), writhe(5L), false);

        assertThat(result).isEmpty();
        assertThat(savedStates()).hasSize(ExperimentsService.MAX_GOAL_ITERATIONS);
        assertThat(experiment.getExperimentStatus()).isEqualTo(ExperimentsService.EXPERIMENT_ITERATION_LIMIT);
    }

    @Test
    void linearRunStopsEarlyWhenNoMoveFitsAnywhere() {
        // The one-crossing loop has no slideable triangle, so a full pass changes nothing.
        var result = service.runExperimentWithGoal(1L,
                List.of(new SlideRequest(null, 1L)), writhe(5L), false);

        assertThat(result).isEmpty();
        assertThat(savedStates()).hasSize(1);
        assertThat(experiment.getExperimentStatus()).isEqualTo(ExperimentsService.EXPERIMENT_GOAL_NOT_REACHED);
    }

    @Test
    void noBranchFollowsOnePathOnSubstateZero() {
        var twist = new AddTwistRequest(null, Sign.pos, 1L);
        var result = service.runExperimentWithGoal(1L, List.of(twist, twist, twist), writhe(3L), false);

        var saved = savedStates();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(s -> s.getId().getSubStateNum() == 0L && s.getParentSubStateNum() == 0L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId().getStateNum()).isEqualTo(2L);
    }

    @Test
    void goalAlreadyMetAtStartReturnsStartState() {
        var result = service.runExperimentWithGoal(1L,
                List.of(new AddTwistRequest(null, Sign.pos, 1L)), writhe(1L), true);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId().getStateNum()).isEqualTo(0L);
        assertThat(savedStates()).isEmpty();
    }

    @Test
    void refusesToRunOnAnExperimentThatAlreadyHasMoves() {
        when(states.existsByIdExperimentIdAndIdStateNumGreaterThan(1L, 0L)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.runExperimentWithGoal(1L, List.of(new AddTwistRequest(null, Sign.pos, 1L)), writhe(2L), true));
    }

    @Test
    void goalRequestBodyDeserializes() throws Exception {
        var body = new com.fasterxml.jackson.databind.ObjectMapper().readValue("""
                {"moveRequests": [{"type": "addTwist", "location": null, "typeSpecificChoice": "pos", "numTimes": 2}],
                 "goal": {"values": {"Writhe": 3}},
                 "branch": true}""", GoalExperimentRequest.class);
        assertThat(body.moveRequests()).singleElement().isInstanceOf(AddTwistRequest.class);
        assertThat(body.goal()).isEqualTo(writhe(3L));
        assertThat(body.branch()).isTrue();
    }

    @Test
    void invariantsSurviveTheDatabaseConverter() {
        var converter = new InvariantsConverter();
        var original = new InvariantsDto("t^2 - t + 1", 3L, 3L);
        var json = converter.convertToDatabaseColumn(original);
        // JSON reads small numbers back as Integer; the DTO coerces them back to Long.
        assertThat(converter.convertToEntityAttribute(json)).isEqualTo(original);
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
