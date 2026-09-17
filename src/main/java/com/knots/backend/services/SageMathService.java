package com.knots.backend.services;

import com.knots.backend.models.dtos.LongPair;
import com.knots.backend.models.dtos.sage.AlexanderRequest;
import com.knots.backend.models.dtos.sage.AlexanderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class SageMathService {

    private final RestClient restClient;

    public AlexanderResponse calculateAlexanderPolynomial(LongPair[][] matrix) {
        validateMatrix(matrix);

        try {
            AlexanderResponse response = restClient.post()
                    .uri("/alexander")
                    .body(AlexanderRequest.usingLastRowAndColumn(matrix))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, serverResponse) -> {
                        throw new SageMathServiceException(
                                "SageMath calculation failed with HTTP status "
                                        + serverResponse.getStatusCode());
                    })
                    .body(AlexanderResponse.class);

            if (response == null
                    || response.polynomial() == null
                    || response.polynomial().isBlank()
                    || response.determinant() == null) {
                throw new SageMathServiceException(
                        "SageMath returned a malformed Alexander response");
            }

            return response;

        } catch (SageMathServiceException exception) {
            throw exception;

        } catch (DecodingException exception) {
            throw new SageMathServiceException(
                    "SageMath returned malformed JSON", exception);

        } catch (RestClientException exception) {
            throw new SageMathServiceException(
                    "Unable to communicate with SageMath", exception);
        }
    }

    private void validateMatrix(LongPair[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException(
                    "Alexander matrix must not be empty");
        }

        for (LongPair[] row : matrix) {
            if (row == null || row.length != matrix.length) {
                throw new IllegalArgumentException(
                        "Alexander matrix must be square");
            }
        }
    }
}