package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MatchLineupResponse {

    private Long matchId;
    private List<QuarterLineup> quarters;

    @Getter
    @AllArgsConstructor
    public static class QuarterLineup {
        private int quarter;
        private List<PlayerAssignment> players;
    }

    @Getter
    @AllArgsConstructor
    public static class PlayerAssignment {
        private Long memberId;
        private String memberName;
        private Position assignedPosition;
        private String positionFit; // MAIN / SUB / OTHER
    }
}
