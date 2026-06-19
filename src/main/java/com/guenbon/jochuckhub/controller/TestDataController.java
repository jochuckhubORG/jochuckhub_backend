package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.service.TestDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "Test Data", description = "로컬 테스트용 더미 데이터 API (운영 환경에서는 사용 금지)")
public class TestDataController {

    private final TestDataService testDataService;

    @PostMapping("/matches/{matchId}/lineup-setup")
    @Operation(summary = "라인업 테스트 팀원 추가",
            description = "자동 라인업 테스트를 위해 더미 팀원 14명(ATTEND 투표)을 자동 추가합니다. " +
                    "이미 ATTEND 투표자가 있으면 14명이 되도록 부족한 만큼만 추가합니다.")
    public ResponseEntity<Map<String, Object>> setupLineupTest(@PathVariable Long matchId) {
        int added = testDataService.setupLineupTest(matchId);
        return ResponseEntity.ok(Map.of(
                "message", "테스트 팀원 추가 완료",
                "addedCount", added
        ));
    }

    @DeleteMapping("/matches/{matchId}/lineup-cleanup")
    @Operation(summary = "라인업 테스트 팀원 제거",
            description = "lineup-setup으로 추가한 더미 팀원과 투표를 모두 삭제합니다.")
    public ResponseEntity<Map<String, Object>> cleanupLineupTest(@PathVariable Long matchId) {
        int removed = testDataService.cleanupLineupTest(matchId);
        return ResponseEntity.ok(Map.of(
                "message", "테스트 팀원 제거 완료",
                "removedCount", removed
        ));
    }
}
