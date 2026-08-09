package com.example.paymentsystem.audit.application;

import com.example.paymentsystem.audit.application.dto.AuditLogCommand;
import com.example.paymentsystem.audit.application.dto.AuditLogSearchCondition;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.audit.domain.AuditLog;
import com.example.paymentsystem.audit.infrastructure.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/** 감사 로그 저장·조회 유스케이스의 주요 분기를 검증한다. */
class AuditLogServiceTest {
    private final AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
    private final AuditLogService service = new AuditLogService(repository);

    /** 허용된 명령을 엔티티로 변환해 저장하는지 검증한다. */
    @Test
    void recordSavesAuditLog() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 10, 0);
        Mockito.when(repository.save(ArgumentMatchers.any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.record(new AuditLogCommand("trace-1", "client-1", "GET", "/api/v1/test", 200, null, 1,
                now, now.plusNanos(1_000_000)));

        Mockito.verify(repository).save(ArgumentMatchers.any(AuditLog.class));
    }

    /** 존재하지 않는 감사 로그 단건 조회가 표준 예외를 반환하는지 검증한다. */
    @Test
    void getRejectsMissingAuditLog() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.empty());
        Assertions.assertThatThrownBy(() -> service.get(1L)).isInstanceOf(ApplicationException.class)
                .hasMessage("감사 로그를 찾을 수 없습니다.");
    }

    /** 역전된 검색 기간을 Repository 호출 전에 거절하는지 검증한다. */
    @Test
    void searchRejectsInvalidDateRange() {
        AuditLogSearchCondition condition = AuditLogSearchCondition.builder()
                .startDate(LocalDateTime.of(2026, 8, 10, 0, 0))
                .endDate(LocalDateTime.of(2026, 8, 9, 0, 0)).page(1).size(20).build();
        Assertions.assertThatThrownBy(() -> service.search(condition)).isInstanceOf(ApplicationException.class);
        Mockito.verifyNoInteractions(repository);
    }
}
