package com.example.paymentsystem.service.audit;

import com.example.paymentsystem.dto.audit.AuditLogCommand;
import com.example.paymentsystem.domain.audit.AuditLog;
import com.example.paymentsystem.repository.audit.AuditLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/** 감사 요청 이력 저장 유스케이스를 검증한다. */
class AuditLogServiceTest {
    private final AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
    private final AuditLogService service = new AuditLogService(repository);

    /** 허용된 명령을 감사 엔티티로 변환해 저장하는지 검증한다. */
    @Test
    void recordSavesAuditLog() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 10, 0);

        service.record(new AuditLogCommand("trace-1", "client-1", "GET", "/api/v1/test", 200, null, 1,
                now, now.plusNanos(1_000_000), "v1:iv:cipher", false));

        Mockito.verify(repository).save(ArgumentMatchers.any(AuditLog.class));
    }
}
