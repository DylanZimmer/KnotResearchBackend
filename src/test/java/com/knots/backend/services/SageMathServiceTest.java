package com.knots.backend.services;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.sage.AlexanderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SageMathServiceTest {

    private MockRestServiceServer server;
    private SageMathService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sage-math:8000");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new SageMathService(builder.build());
    }

    @Test
    void sendsLongPairsAndNullEntriesAndReturnsPolynomial() {
        LongPair[][] matrix = {
                {new LongPair(-1L, 1L), new LongPair(1L, 0L), new LongPair(0L, -1L)},
                {new LongPair(1L, 0L), new LongPair(-2L, 1L), new LongPair(1L, -1L)},
                {null, null, null}
        };
        server.expect(once(), requestTo("http://sage-math:8000/alexander"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"matrix":[[{"x":-1,"y":1},{"x":1,"y":0},{"x":0,"y":-1}],
                        [{"x":1,"y":0},{"x":-2,"y":1},{"x":1,"y":-1}],
                        [null,null,null]],"removeRow":null,"removeColumn":null}
                        """))
                .andRespond(withSuccess("{\"polynomial\":\"t^2 - 3*t + 1\",\"determinant\":5}", MediaType.APPLICATION_JSON));

        assertThat(service.calculateAlexanderPolynomial(matrix)).isEqualTo(new AlexanderResponse("t^2 - 3*t + 1", 5L));
        server.verify();
    }

    @Test
    void wrapsHttpErrors() {
        LongPair[][] matrix = {{new LongPair(1L, 0L)}};
        server.expect(requestTo("http://sage-math:8000/alexander")).andRespond(withServerError());

        assertThatThrownBy(() -> service.calculateAlexanderPolynomial(matrix))
                .isInstanceOf(SageMathServiceException.class)
                .hasMessageContaining("HTTP status 500");
    }

    @Test
    void rejectsMalformedResponses() {
        LongPair[][] matrix = {{new LongPair(1L, 0L)}};
        server.expect(requestTo("http://sage-math:8000/alexander"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.calculateAlexanderPolynomial(matrix))
                .isInstanceOf(SageMathServiceException.class)
                .hasMessageContaining("malformed");
    }
}
