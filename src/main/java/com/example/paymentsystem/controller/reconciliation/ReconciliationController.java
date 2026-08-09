package com.example.paymentsystem.controller.reconciliation;

import com.example.paymentsystem.common.response.ApiResponse;
import com.example.paymentsystem.dto.reconciliation.ReconciliationBreakResponse;
import com.example.paymentsystem.service.reconciliation.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 자동 탐지된 경량 대사 불일치를 읽기 전용으로 제공한다. */
@RestController
@RequestMapping("/api/v1/ops/reconciliation-breaks")
@RequiredArgsConstructor
@Tag(name = "Reconciliation", description = "경량 원장·잔고 대사 조회 API")
public class ReconciliationController {
    private final ReconciliationService reconciliationService;

    /** 저장된 대사 불일치를 최신순으로 조회한다. */
    @GetMapping
    @Operation(summary = "대사 불일치 목록 조회")
    public ApiResponse<List<ReconciliationBreakResponse>> findAll() {
        return ApiResponse.success(reconciliationService.findAll().stream()
                .map(ReconciliationBreakResponse::from).toList());
    }
}
