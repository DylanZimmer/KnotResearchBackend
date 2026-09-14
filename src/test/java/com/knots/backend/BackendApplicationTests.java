package com.knots.backend;

import com.knots.backend.models.dtos.InvariantsDto;
import com.knots.backend.models.entities.*;
import com.knots.backend.models.keys.*;
import com.knots.backend.repositories.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendApplicationTests {

    @Autowired private TestRestTemplate http;
    @Autowired private InvariantRolfRepo invariantsRepo;
    @Autowired private ExperimentsFNRepo notationRepo;
    @Autowired private ExperimentStatesRepo statesRepo;
    @Autowired private FullNotationRolfRepo rolfNotationRepo;
    @Autowired private EntityManager entityManager;

    @Test
    void rejectsMissingDataAndInvalidMovesOverHttp() {
        assertThat(http.getForEntity("/api/rolf/rolf_invariants?knotId=999999", String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.postForEntity("/api/exp/start_experiment", java.util.List.of(), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.postForEntity("/api/exp/start_experiment", java.util.List.of(999999L), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.postForEntity("/api/exp/perform_move_single_state?experimentId=999999&ogKnotId=42&stateNum=0&move=mirror", null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.postForEntity("/api/exp/perform_move_all_in_state?experimentId=999999&stateNum=0&move=invalid", null, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void servesStoredInvariantsOverHttp() {
        InvariantsRolf invariants = new InvariantsRolf();
        invariants.setKnotId(42L);
        invariants.setAlexander_polynomial("t^2 - t + 1");
        invariants.setDeterminant(3L);
        invariants.setWrithe(3L);
        invariantsRepo.saveAndFlush(invariants);
        try {
            var response = http.getForEntity(
                    "/api/rolf/rolf_invariants?knotId=42", InvariantsDto.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(new InvariantsDto("t^2 - t + 1", 3L, 3L));
        } finally {
            invariantsRepo.deleteById(42L);
        }
    }

    @Test
    @Transactional
    void reloadsCompositeKeysAndProjectsOnlyTheRequestedState() {
        var key = new ExperimentsFNKey(7L, 42L, 0L, "over", 3L);
        notationRepo.save(new ExperimentsFullNotation(key, 2L, 4L, 5L, 6L, 1L));
        notationRepo.save(new ExperimentsFullNotation(
                new ExperimentsFNKey(7L, 42L, 1L, "under", 3L), 4L, 2L, 6L, 5L, -1L));
        var stateKey = new ExperimentStatesKey(7L, 42L, 0L);
        statesRepo.save(new ExperimentStates(stateKey, null, "starting", "1", 1L, 1L));
        var rolfKey = new FullNotationKey(42L, 3L, "over");
        FullNotationRolf rolf = new FullNotationRolf();
        rolf.setId(rolfKey);
        rolfNotationRepo.save(rolf);
        entityManager.flush();
        entityManager.clear();

        assertThat(notationRepo.findById(key)).isPresent();
        assertThat(statesRepo.findById(stateKey)).isPresent();
        assertThat(rolfNotationRepo.findById(rolfKey)).isPresent();
        assertThat(statesRepo.findOgKnotIdsByExperimentIdAndStateNum(7L, 0L)).containsExactly(42L);
        assertThat(notationRepo.findFnListFromState(7L, 42L, 0L))
                .containsExactly(new FullNotation(42L, 3L, "over", 2L, 4L, 5L, 6L, 1L));
        assertThat(notationRepo.findFnListFromState(8L, 42L, 0L)).isEmpty();
    }
}
