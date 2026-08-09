package com.example.paymentsystem.reconciliation.presentation;

import com.example.paymentsystem.payment.presentation.query.dto.PageResponse;
import com.example.paymentsystem.reconciliation.application.ReconciliationExecutionPort;
import com.example.paymentsystem.reconciliation.application.ReconciliationOperationsService;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.TriggerType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import com.example.paymentsystem.reconciliation.presentation.dto.ReconciliationDtos.ActionRequest;
import com.example.paymentsystem.reconciliation.presentation.dto.ReconciliationDtos.ActionResponse;
import com.example.paymentsystem.reconciliation.presentation.dto.ReconciliationDtos.CaseDetailResponse;
import com.example.paymentsystem.reconciliation.presentation.dto.ReconciliationDtos.CaseResponse;
import com.example.paymentsystem.reconciliation.presentation.dto.ReconciliationDtos.RecheckRequest;
import com.example.paymentsystem.reconciliation.presentation.dto.ReconciliationDtos.RunRequest;
import com.example.paymentsystem.reconciliation.presentation.dto.ReconciliationDtos.RunResponse;
import com.example.paymentsystem.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 운영자가 대사를 실행하고 불일치 판정 이력을 관리하는 API다. */
@Tag(name = "운영 대사")
@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class ReconciliationController {
  private final ReconciliationExecutionPort executor;
  private final ReconciliationOperationsService operations;

  @Operation(summary = "대사 실행")
  @PostMapping("/reconciliations/runs")
  public ResponseEntity<ApiResponse<RunResponse>> run(@Valid @RequestBody RunRequest request) {
    ReconciliationRun run =
        executor.execute(
            request.rangeStart(), request.rangeEnd(), TriggerType.MANUAL, request.requestedBy());
    return ResponseEntity.ok(ApiResponse.success(RunResponse.from(run)));
  }

  @Operation(summary = "대사 실행 목록")
  @GetMapping("/reconciliations/runs")
  public ResponseEntity<ApiResponse<PageResponse<RunResponse>>> runs(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(PageResponse.from(operations.runs(page, size), RunResponse::from)));
  }

  @Operation(summary = "대사 실행 상세")
  @GetMapping("/reconciliations/runs/{id}")
  public ResponseEntity<ApiResponse<RunResponse>> run(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(RunResponse.from(operations.run(id))));
  }

  @Operation(summary = "대사 Case 목록")
  @GetMapping("/reconciliation-cases")
  public ResponseEntity<ApiResponse<PageResponse<CaseResponse>>> cases(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(PageResponse.from(operations.cases(page, size), CaseResponse::from)));
  }

  @Operation(summary = "대사 Case 상세")
  @GetMapping("/reconciliation-cases/{id}")
  public ResponseEntity<ApiResponse<CaseDetailResponse>> detail(@PathVariable Long id) {
    var detail = operations.detail(id);
    List<ActionResponse> actions = detail.actions().stream().map(ActionResponse::from).toList();
    return ResponseEntity.ok(
        ApiResponse.success(
            new CaseDetailResponse(CaseResponse.from(detail.reconciliationCase()), actions)));
  }

  @Operation(summary = "대사 Case 운영 조치 기록")
  @PostMapping("/reconciliation-cases/{id}/actions")
  public ResponseEntity<ApiResponse<CaseResponse>> action(
      @PathVariable Long id, @Valid @RequestBody ActionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            CaseResponse.from(
                operations.action(
                    id,
                    request.actionType(),
                    request.operatorId(),
                    request.reason(),
                    request.externalReference()))));
  }

  @Operation(summary = "대사 Case 재검증")
  @PostMapping("/reconciliation-cases/{id}/recheck")
  public ResponseEntity<ApiResponse<CaseResponse>> recheck(
      @PathVariable Long id, @Valid @RequestBody RecheckRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            CaseResponse.from(operations.recheck(id, request.operatorId(), request.reason()))));
  }
}
