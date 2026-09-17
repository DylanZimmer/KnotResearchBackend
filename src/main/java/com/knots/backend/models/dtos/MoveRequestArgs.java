package com.knots.backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveRequestArgs {
    //For addTwist
    private Long strand;
    private Long sign;

    //For addR2
    private Long strand1;
    private Long strand2;
}
