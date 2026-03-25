package com.guenbon.jochuckhub.dto.request;

import com.guenbon.jochuckhub.entity.Position;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SaveLineupRequest {

    @NotNull
    @Size(min = 4, max = 4, message = "4개 쿼터 데이터가 필요합니다.")
    @Valid
    private List<QuarterEntry> quarters;

    @Getter
    @NoArgsConstructor
    public static class QuarterEntry {

        @NotNull
        private Integer quarter; // 1~4

        @NotNull
        @Size(min = 10, max = 10, message = "각 쿼터는 정확히 10명이어야 합니다.")
        @Valid
        private List<PlayerEntry> players;
    }

    @Getter
    @NoArgsConstructor
    public static class PlayerEntry {

        @NotNull
        private Long memberId;

        @NotNull
        private Position position;
    }
}
