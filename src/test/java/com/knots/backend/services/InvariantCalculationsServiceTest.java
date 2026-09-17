package com.knots.backend.services;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.sage.AlexanderResponse;
import com.knots.backend.models.entities.FullNotation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvariantCalculationsServiceTest {
    @Test
    void handlesNonzeroCrossingIdsAndAddsCoefficientsOnSharedArcs() {
        SageMathService sage = mock(SageMathService.class);
        when(sage.calculateAlexanderPolynomial(any())).thenReturn(new AlexanderResponse("1", 1L));
        var service = new InvariantCalculationsService(sage);
        var result = service.getAllInvariants(List.of(
                new FullNotation(42L, 17L, "over", 17L, 17L, 0L, 1L, 1L),
                new FullNotation(42L, 17L, "under", 17L, 17L, 1L, 0L, 1L)));
        var matrix = ArgumentCaptor.forClass(LongPair[][].class);
        verify(sage).calculateAlexanderPolynomial(matrix.capture());
        assertThat(matrix.getValue()).hasDimensions(1, 1);
        assertThat(matrix.getValue()[0][0]).isEqualTo(new LongPair(0L, 0L));
        assertThat(result.writhe()).isEqualTo(1L);
    }
}
