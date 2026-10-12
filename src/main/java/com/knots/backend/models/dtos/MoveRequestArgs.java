package com.knots.backend.models.dtos;

import com.knots.backend.models.dtos.Sign;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveRequestArgs {
    //For addTwist, r3Slide
    private Long strand;

    //For addTwist
    private Sign sign;

    //For addR2
    private LongPair underStrandOriented;
    private LongPair overStrandOriented;

    //For r3Slide
    private List<Long> triangleBdy;
    private Long cid;

    //For saddle
    private Long strand1;
    private Long strand2;

}
