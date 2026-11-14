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
    //For addTwist, Slide
    private Long strand;

    //For addTwist
    private Sign sign;

    //For addPoke
    private LongPair underStrandOriented;
    private LongPair overStrandOriented;

    //For Slide
    private List<Long> triangleBdy;
    private Long cid;

    //For remPoke (with cid)
    private Long cid2;

    //For saddle
    private Long strand1;
    private Long strand2;

}
