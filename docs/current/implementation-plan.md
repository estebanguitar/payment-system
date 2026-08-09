# MVP 단순화 및 레이어드 아키텍처 전환 구현계획

## 1. 문서 정보

| 항목 | 내용 |
| --- | --- |
| 목적 | 불필요한 기능과 추상화를 제거하고 현재 규모에 맞는 레이어드 모놀리스로 전환한다. |
| 상태 | 구현 완료 |
| 작업 기준 | `main`에서만 작업 |
| 변경 원칙 | 기능 제거, 패키지 이동, 감사 암호화 변경을 검토 가능한 커밋으로 분리한다. |

## 2. 최종 기능 범위

유지할 기능은 고객·지갑, 충전, 결제, 전액·부분 취소, 이력 조회, 멱등성, Fake PG, PG 응답 암호화, Swagger/OpenAPI, 공통 오류 코드, JaCoCo, Docker 실행이다.

Outbox는 다음 최소 흐름만 유지한다.

1. 결제 또는 취소 원장과 `PaymentOutbox(PENDING)`를 동일 트랜잭션에서 저장한다.
2. 커밋 후 이벤트 Listener가 해당 Outbox를 즉시 처리한다.
3. 처리 실패 시 `RETRY`와 재시도 횟수·다음 실행 시각을 기록한다.
4. 모놀리스 Scheduler가 남아 있는 `PENDING/RETRY`를 주기적으로 조회해 재처리한다.
5. 상태 조건부 갱신과 PG 멱등키로 Listener/Scheduler 중복 실행을 방어한다.

감사 로그는 API 요청 이력 저장만 유지한다. 조회·삭제·보존 Scheduler는 제외하며, 요청/응답 등 민감 원문은 AES-256-GCM으로 암호화한다.

## 3. 제거 계획

### 3.1 잔고·원장 대사

- `src/main/java/.../reconciliation/**` 전체를 제거한다.
- `src/test/java/.../reconciliation/**` 및 대사 HTTP 통합 테스트를 제거한다.
- `/api/v1/operations/reconciliations` OpenAPI 경로를 제거한다.
- 대사 오류 코드, 설정 및 다른 계층의 참조를 제거한다.
- V5 migration은 이미 적용 가능한 이력이므로 수정·삭제하지 않는다. unused table 유지 여부와 새 drop migration은 구현 착수 시 DB 초기화 정책을 확인한 뒤 별도 결정한다.

### 3.2 감사 로그 과잉 기능

- `AuditLogQueryController`, 검색 요청/응답 DTO, `AuditLogSpecifications`와 조회 Service 메서드를 제거한다.
- 감사 로그 공개 조회·삭제 API를 OpenAPI에서 제거한다.
- `AuditLogFilter`, 저장 Service, Entity와 Repository는 축소해 유지한다.

### 3.3 과도한 아키텍처 요소

- 내부 구현이 하나뿐이고 테스트 대역 외 교체 필요가 없는 `port/in`, `port/out`을 직접 Service 의존으로 전환한다.
- 단순 전달만 하는 Service는 호출 흐름을 검토해 인접 Service로 병합한다.
- DTO는 HTTP 계약과 여러 Service 사이의 명확한 경계에만 유지한다.
- 외부 PG와 암호화기는 실제 외부 경계이므로 인터페이스를 유지한다.

## 4. 목표 패키지와 파일 이동

```text
com.example.paymentsystem
├─ domain/{customer,wallet,payment,cancellation,outbox,audit}
├─ repository/{customer,wallet,payment,cancellation,outbox,audit,idempotency,pg}
├─ service/{customer,wallet,payment,cancellation,outbox,audit,idempotency}
├─ controller/{wallet,payment,query}
├─ dto/{wallet,payment,cancellation,query,outbox,audit}
├─ integration/pg
├─ scheduler/outbox
└─ common/{response,exception,config,security}
```

주요 이동 기준은 다음과 같다.

| 현재 | 목표 |
| --- | --- |
| `*/domain/*` | `domain/<domain>/*` |
| `*/infrastructure/repository/*` | `repository/<domain>/*` |
| `*/application/service/*`, query service | `service/<domain>/*` |
| `*/presentation/*Controller` | `controller/<domain>/*` |
| `*/presentation/**/dto`, `*/application/dto` | `dto/<domain>/*` |
| `pg/application/port`, `pg/infrastructure` | `integration/pg/*` |
| `outbox/infrastructure/scheduling` | `scheduler/outbox/*` |
| `shared/*` | `common/*` |

`PaymentEventListener`와 `PaymentCancelEventListener`는 `service/outbox`에 배치해 AFTER_COMMIT 즉시 처리의 시작점임을 드러낸다. 패키지 이동 중 클래스명, REST path, JSON property, JPA table/column은 변경하지 않는다.

## 5. 감사 로그 암호화 상세

### 5.1 데이터 모델

`AuditLog`는 검색용 메타데이터와 암호화 payload를 분리한다.

- 평문: `id`, `requestedAt`, `completedAt`, `httpMethod`, `normalizedPath`, `statusCode`, `durationMs`, `traceId`
- 암호화: 호출자 식별값, 원 URL/query, request headers allowlist, request body, response body
- 암호화 envelope: `cipherText`, `nonce`, `keyVersion` 또는 하나의 버전형 JSON payload

암호화에는 기존 `AesGcmPayloadEncryptor`의 공통 보안 구현을 재사용하되 PG 타입에 종속된 이름과 패키지는 `common/security`로 옮긴다. 매 저장마다 새 96-bit nonce를 생성하고 인증 태그를 검증한다. 키 누락·길이 오류는 애플리케이션 시작 시 실패하도록 설정 검증을 둔다.

### 5.2 수집과 저장

- `ContentCachingRequestWrapper`와 `ContentCachingResponseWrapper`로 body를 캡처하고 응답 body를 반드시 클라이언트에 복사한다.
- binary/multipart/stream 응답은 body 저장 대상에서 제외하고 content type·크기만 기록한다.
- body 최대 저장 크기를 설정해 메모리와 DB 사용량을 제한하고 초과 여부를 기록한다.
- 인증 토큰, cookie, password, CVV 등은 파싱 가능한 JSON/header에서 제거한 후 payload 전체를 암호화한다.
- 저장은 `REQUIRES_NEW`로 수행하고 실패 시 trace ID를 포함한 애플리케이션 오류 로그만 남긴다.

### 5.3 DDL

기존 V4를 변경하지 않는다. 필요한 신규 암호문·nonce·버전·본문 크기 컬럼과 기존 평문 컬럼 정리는 새 migration으로 추가한다. 모든 신규 컬럼 comment는 `CREATE TABLE` 신규 생성 시 내부에, 기존 테이블 변경이면 DB가 지원하는 `COMMENT ON COLUMN` 문으로 명시한다.

## 6. Outbox 단순화 상세

- `PaymentCreationService`/취소 생성 Service는 업무 원장과 Outbox를 한 `@Transactional` 안에서 저장한다.
- 트랜잭션 안에서 Spring 이벤트를 발행하고 Listener는 `phase = AFTER_COMMIT`으로 해당 Outbox ID를 받는다.
- Listener는 `OutboxRecoveryService.process(outboxId)`를 호출하되 예외를 요청 스레드 밖으로 전파해 업무 커밋 결과를 바꾸지 않는다.
- `OutboxScheduler`는 실행 가능 시각이 지난 `PENDING/RETRY`만 제한 개수로 조회한다.
- 처리 시작은 `WHERE status IN (...)` 조건부 update 또는 동등한 잠금으로 단일 처리자를 선점한다.
- 성공은 완료 상태, 기술 실패는 backoff가 계산된 `RETRY`로 전환한다. 별도 Worker 전용 lease/heartbeat/격리 모델은 제거한다.

## 7. 테스트 변경

- 제거된 대사 및 감사 조회 API 테스트를 삭제한다.
- 감사 Filter의 성공·예외·404, body 복사, 크기 제한, binary 제외를 검증한다.
- 암호문에 원문과 secret이 나타나지 않고 올바른 키로만 복호화되는지 검증한다.
- nonce가 요청마다 달라 동일 원문도 다른 암호문이 되는지 검증한다.
- 감사 저장 실패가 업무 응답을 바꾸지 않는지 통합 테스트한다.
- Payment/Cancel + Outbox 원자 커밋, AFTER_COMMIT 즉시 처리, 실패 후 Scheduler 복구를 검증한다.
- Listener와 Scheduler 경합 시 유효한 후속 처리가 한 번만 일어나는지 검증한다.
- ArchUnit은 단순 레이어 의존 규칙만 유지한다.
- `clean test jacocoTestReport bootJar`와 Docker Compose 기동을 최종 검증한다.

## 8. 구현 및 커밋 순서

1. `docs: MVP 범위와 레이어드 아키텍처 재정의`
2. `refactor: 잔고 대사 기능 제거`
3. `refactor: 감사 로그를 요청 이력 저장으로 축소`
4. `feat: 감사 요청 응답 암호화 저장`
5. `refactor: 레이어드 패키지 구조로 단순화`
6. `refactor: 모놀리스 outbox 즉시 처리와 scheduler 복구 단순화`
7. `test: 단순화 아키텍처 회귀 테스트 보강`
8. `docs: 구현 결과와 API 명세 현행화`

각 커밋은 컴파일 가능해야 한다. 기능 삭제와 대규모 rename을 같은 커밋에 섞지 않으며 Lombok·주석·import 규칙을 적용한다.

## 9. 위험과 대응

| 위험 | 대응 |
| --- | --- |
| 적용된 Flyway checksum 손상 | V1~V5를 수정하지 않고 forward-only migration만 추가 |
| 감사 body 저장으로 메모리·DB 증가 | content type/크기 제한과 truncation 메타데이터 적용 |
| 암호화했지만 token이 장기 보존됨 | 복호화 불필요 secret은 암호화 전에 제거 |
| AFTER_COMMIT과 Scheduler 중복 처리 | 조건부 상태 전이와 PG 멱등키 유지 |
| 패키지 이동 중 Spring scan 누락 | Context, Repository, HTTP 통합 테스트 실행 |
| 기능 제거가 API 소비자에 영향 | 제거 API를 OpenAPI와 README에 명시하고 핵심 결제 API 회귀 검증 |

## 10. 완료 기준

- `reconciliation` 런타임 코드와 API가 없다.
- 독립 Worker 없이 한 Spring Boot 프로세스에서 즉시 처리와 Scheduler 복구가 동작한다.
- 감사 로그는 요청 이력 저장만 수행하고 공개 조회·삭제 기능이 없다.
- 감사 원문과 민감정보가 DB에 평문으로 저장되지 않는다.
- 목표 레이어드 패키지 외 과거 중첩 패키지와 불필요 Port가 남지 않는다.
- 공개 결제·지갑·취소·조회 API와 멱등성 계약이 유지된다.
- 전체 테스트, JaCoCo HTML, bootJar 및 Docker Compose 검증이 통과한다.

## 11. 승인 요청 사항

1. 잔고·원장 대사 기능과 감사 조회 API를 완전히 제거한다. - 기능 단순화 이후 잔고/원장 대사는 추가하도록 함.
2. 이미 병합된 Flyway V4/V5는 수정하지 않고 신규 migration만 허용한다. - 동의
3. 단일 레이어드 모놀리스 패키지로 전환하고 내부 Port를 최소화한다. - 동의
4. Outbox는 AFTER_COMMIT 즉시 처리와 동일 프로세스 Scheduler 복구만 유지한다. - 동의
5. 감사 요청·응답은 AES-256-GCM으로 암호화하되 복호화 불필요 secret은 제거한다. - 동의
6. 위 순서대로 `main`에서 구현하고 태스크별 커밋을 분리한다. - 동의

## 12. 구현 결과

- 잔고·원장 대사 런타임 코드, 테스트와 운영 API를 제거했다. Flyway V5는 적용 이력 보호를 위해 유지했다.
- 감사 로그 검색·단건 조회 API와 검색 전용 DTO·Specification을 제거했다.
- 감사 Filter가 호출자·URL·query·요청 body·응답 body를 수집하고 AES-256-GCM 암호문으로 저장하도록 변경했다.
- password·token·authorization·cookie·CVV 계열 JSON 값은 암호화 전에 마스킹하며 텍스트 body는 요청·응답별 최대 4,096자로 제한한다.
- Flyway V6로 암호화 payload와 truncation 컬럼을 추가하고 기존 migration checksum을 보존했다.
- 운영 코드와 테스트를 `domain`, `repository`, `service`, `controller`, `dto`, `integration`, `scheduler`, `common` 레이어로 이동하고 비어 있는 과거 패키지를 제거했다.
- Outbox 상태를 `PENDING`, `RETRY`, `COMPLETED`, `FAILED`로 명확히 하고 Flyway V7에서 기존 `INIT`, `PUBLISHED` 데이터를 전진 변환한다.
- AFTER_COMMIT Listener의 즉시 처리와 같은 프로세스 Scheduler의 `PENDING/RETRY` 복구를 유지했다.
- ArchUnit 규칙을 새 레이어드 구조에 맞춰 변경하고 과거 도메인 우선 최상위 패키지의 재생성을 차단했다.
- `clean test jacocoTestReport bootJar`로 115개 테스트, JaCoCo HTML과 실행 JAR 생성을 확인했다.
- 현재 PC에는 Docker CLI가 없어 Compose 실제 기동 검증은 수행하지 못했다.
- 테스트는 `src/test/unit/java`와 `src/test/integration/java` source set으로 물리 분리했다.
- 단위 테스트는 payment, wallet, cancellation, outbox, audit, idempotency, pg, common 도메인 아래 역할별로 배치했다.
- 통합 테스트는 api, persistence, payment, audit, outbox, architecture 관심사별로 배치하되 Java package는 운영 코드와 동일하게 유지해 불필요한 접근 제한자 변경을 방지했다.
# 승인 범위 반영 (2026-08-09)

- 결제와 취소 패키지를 `payment`로 통합하고 Result 전용 서비스를 제거한다.
- 단일 호출 인터페이스인 `OutboxRecoveryUseCase`를 제거하고 `OutboxProcessor`로 실제 PG 처리 책임을 모은다.
- Listener는 즉시 처리 트리거, Scheduler는 복구 트리거로 제한한다.
- 경량 대사 도메인·저장소·서비스·스케줄러·조회 API와 Flyway DDL을 추가한다.
- 과거 문서는 `docs/archive`, 현재 기준 문서는 `docs/current`로 구분한다.
# 공통 웹·PG 연동 패키지 정리 계획 (승인·구현 완료)

## 목적

레이어 이름과 실제 책임이 어긋난 패키지를 정리하고, 중복된 `integration.pg.pg` 경로를 제거한다. 기능과 빈 이름은 변경하지 않는 순수 패키지 리팩터링으로 수행한다.

## 코드 수준 변경

1. 감사 로그 웹 지원 코드를 `controller.audit`에서 `common.audit`로 이동한다.
   - `AuditLogFilter`
   - `AuditRequestContext`
   - `AuditValueSanitizer`
   - 감사 로그 전용 필터·컨텍스트·마스킹이라는 공통 횡단 관심사 책임을 유지한다.
2. `TraceIdFilter`는 감사 로그와 분리하여 `common.web`으로 이동한다.
   - 모든 HTTP 요청의 추적 ID를 생성·전파하는 전역 웹 관심사로 정의한다.
   - `AuditLogFilter`는 `common.web.TraceIdFilter`를 import하여 기존 request attribute 계약을 유지한다.
3. `integration.pg.pg`의 타입을 한 단계 위인 `integration.pg`로 이동한다.
   - `PgClient`, Command/Result, 상태·오류 타입과 예외를 모두 포함한다.
   - `FakePgClient`, 결제 처리 서비스, `OutboxProcessor`, 테스트의 import를 일괄 수정한다.
4. PG 암호화 구현과 설정을 `integration.pg.security`로 이동한다.
   - `AesGcmPayloadEncryptor`
   - `EncryptionException`
   - `PaymentSecurityProperties`
   - 기존 `PayloadEncryptor`와 같은 패키지에 두어 암호화 계약·구현·설정·예외를 응집한다.
   - `InfrastructureConfiguration`과 관련 테스트의 import를 수정한다.
5. 이동 후 비어 있는 `controller.audit`, `integration.pg.pg` 패키지 디렉터리를 제거한다.

## 테스트 및 완료 기준

- 감사 로그 필터 단위·통합 테스트의 패키지와 경로를 새 운영 코드 구조에 맞춘다.
- PG 클라이언트 및 암호화 단위 테스트의 package/import를 새 경로에 맞춘다.
- `rg`로 이전 패키지 참조가 남지 않았는지 확인한다.
- `clean test jacocoTestReport bootJar`와 `git diff --check`를 통과한다.
- API, 암호화 포맷, 설정 키, 데이터베이스 스키마와 런타임 동작은 변경하지 않는다.
# Outbox 선점·PG 결과 영속화·보상 취소 구현계획 (승인 대기)

## 1. 현재 코드 문제 교정

- `PaymentProcessingService.failSystem()`과 취소 처리의 동명 메서드에서 `markCompleted()`를 제거한다.
- `OutboxProcessor`가 `PgClientException.errorType`을 확인하여 TIMEOUT과 확정 실패를 분리한다.
- 최초 `findById()` 확인 후 PG를 호출하는 흐름을 제거하고 반드시 선점 서비스의 결과가 있을 때만 처리한다.
- PG 승인 후 내부 처리 실패를 바깥 catch의 일반 `recordFailure()`로만 환원하지 않고 현재 처리 단계를 유지한다.

## 2. 도메인·DDL

- `OutboxStatus`에 `PROCESSING`을 추가한다.
- `PaymentOutbox`에 다음 필드를 추가한다.
  - `processingStage`: `PG_REQUEST`, `PG_APPROVED`, `COMPENSATION_REQUIRED`
  - `processingStartedAt`: lease 시작 시각
  - `processingToken`: 현재 처리자 fencing token
  - `pgTransactionId`, `pgResponseCode`, `encryptedPgPayload`: 승인 후 내부 처리 재개에 필요한 PG 결과 스냅샷
- `claim(now, token)`, `reclaim(now, token)`, `recordPgApproved(...)`, `requireOwner(token)`, `recordFailure(...)`, `markCompleted(...)`, `markFailed(...)` 상태 전이를 추가하고 허용 상태를 명시한다.
- `PaymentFailureReason.COMPENSATION_FAILED`, `PgOperationType.PAYMENT_COMPENSATION`, `ReconciliationBreakType.PAYMENT_COMPENSATION_FAILED`를 추가한다.
- 기존 `wallet_transaction.idempotency_key` 고유 제약을 결제 차감에도 사용한다. 결제 차감 키는 결제 ID에서 결정적으로 생성하고, 충전 및 취소 환불의 기존 멱등 정책은 유지한다.
- `pg_response_log`에는 `(operation_type, pg_transaction_id)` 고유 제약을 추가하여 동일 승인·보상 결과의 로그 중복을 막는다.
- Flyway `V10`에서 Outbox 처리 단계·시각·토큰·PG 결과 컬럼을 추가하며 각 컬럼의 `COMMENT`를 컬럼 선언에 인라인으로 작성한다.

## 3. 저장소와 선점 서비스

- `PaymentOutboxRepository.findByIdWithLock()`을 선점 진입점으로 사용한다.
- 스케줄러 조회를 다음 두 쿼리로 분리한다.
  - 오래된 `PENDING/RETRY`
  - `processing_started_at`이 lease 임계값보다 오래된 `PROCESSING`
- `OutboxClaimService`를 추가하고 `REQUIRES_NEW` 트랜잭션에서 잠금 조회, 상태 검증, UUID 토큰 발급, `PROCESSING` 전이를 커밋한다.
- 상태 저장 메서드는 outbox ID와 processing token을 함께 받아 소유권을 검증한다. `RETRY/COMPLETED/FAILED` 전이 시 lease 시각과 token을 비우되 처리 단계와 저장된 PG 결과는 재개 정책에 따라 유지한다.

## 4. PG 계약과 오류 정책

- `PgErrorType.ERROR`를 의미가 명확한 `CONFIRMED_FAILURE`로 변경하고 `TIMEOUT`을 유지한다.
- PG 승인 재시도는 최초 요청과 동일한 결제 멱등 키를 반드시 사용한다. `FakePgClient`도 멱등 키별 최초 승인 결과와 거래 ID를 재사용하여 중복 승인을 생성하지 않는다.
- `PgClient` 계약 주석과 아키텍처 문서에 “PG가 승인·보상 멱등 키별 최초 결과를 재반환한다”는 외부 시스템 가정을 명시한다. 실제 PG 어댑터는 이 가정을 검증하지 못하면 활성화하지 않는다.
- `PgClient`에 승인 직후 보상 전용 `compensate(PgCompensationCommand)`를 추가한다. 고객 취소 API와 모델을 재사용하지 않는다.
- 보상 멱등 키는 결제 멱등 키에서 결정적으로 생성한다(예: `<payment-key>:compensation`).
- `FakePgClient`는 승인, 거절, TIMEOUT, 확정 실패와 보상 성공·거절·TIMEOUT을 결정적으로 재현한다.

## 5. OutboxProcessor 단계 실행

1. `claim(outboxId)`가 반환한 토큰과 현재 단계를 확보한다. 선점 실패 시 조용히 종료한다.
2. `PG_REQUEST`이면 PG 승인 호출:
   - 거절: 결제 실패·PG 로그·Outbox 완료를 한 트랜잭션으로 반영한다.
   - 확정 기술 실패: 결제 `FAILED/SYSTEM_ERROR`, Outbox `FAILED`로 반영한다.
   - TIMEOUT: 결제는 변경하지 않고 `RETRY`로 전환하며 동일 멱등 키로 승인 단계를 재시도한다.
   - 승인: 승인 결과와 암호화 원문을 저장하고 단계를 `PG_APPROVED`로 전환한다.
3. `PG_APPROVED`이면 지갑 잠금 후 차감과 결제 완료를 시도한다.
   - 성공: Outbox를 완료한다.
   - 잔액 부족: 결제를 아직 완료/실패로 확정하지 않고 단계를 `COMPENSATION_REQUIRED`로 저장한다.
   - 일시적 내부 실패: 승인 결과를 유지한 채 `PG_APPROVED` 단계로 재시도한다.
4. `COMPENSATION_REQUIRED`이면 승인 거래에 대한 보상 취소만 호출한다.
   - 성공: 보상 PG 로그 저장, 결제 `FAILED/INSUFFICIENT_BALANCE`, Outbox 완료.
   - 확정 실패: 결제 `FAILED/COMPENSATION_FAILED`, Outbox 실패, 대사 Break 생성.
   - TIMEOUT: 같은 보상 멱등 키로 보상 단계만 재시도.
   - 동일 결과 재수신: PG 보상 거래 ID와 내부 반영 멱등 키를 확인하고 보상 로그·결제 상태·필요한 지갑 반영을 한 번만 커밋.

## 5.1 내부 이중 결제 방지

- 내부 승인 반영은 결제 행을 잠그고 `PENDING` 또는 아직 차감되지 않은 상태인지 확인한다.
- `wallet_transaction`에서 같은 결제 차감 멱등 키의 `PAYMENT` 거래가 이미 존재하면 지갑을 다시 차감하지 않는다.
- 데이터베이스 고유 제약을 최종 방어선으로 사용한다. 제약 충돌 트랜잭션은 롤백하고 별도 트랜잭션에서 기존 거래와 결제 상태를 재조회하여 성공 결과로 수렴한다.
- 같은 Outbox가 TIMEOUT 후 재실행되어 동일 PG 승인 결과를 받더라도 PG 거래 로그는 동일 PG 거래 ID 기준으로 중복 저장하지 않는다.
- 보상 TIMEOUT 후 동일 결과를 다시 받아도 보상 PG 로그와 내부 상태 전이, 필요한 지갑 거래는 각각 한 번만 반영한다.
- PG 멱등 키가 달라지는 요청은 재시도가 아니라 별도 거래로 간주하므로 Outbox가 임의로 키를 재생성하지 못하게 한다.

## 6. Scheduler 및 설정

- 기존 `orphan-threshold`를 PROCESSING lease 만료 기준으로 사용하고 명칭을 문서화한다.
- Scheduler는 일반 후보와 stale PROCESSING ID만 조회하고 실제 선점 판단은 `OutboxClaimService`에 위임한다.
- 재시도 한도 도달 시 현재 단계에 따라 실패 사유를 결정한다. 보상 단계 한도 초과는 반드시 보상 실패 Break를 남긴다.

## 7. 테스트

- Listener와 Scheduler가 같은 ID를 동시에 호출해도 PG 호출이 한 번만 발생하는 동시성 통합 테스트.
- stale PROCESSING 회수 및 아직 lease가 유효한 PROCESSING 미회수 테스트.
- 이전 token 처리자가 회수 이후 완료 상태를 덮어쓰지 못하는 fencing 테스트.
- PG 확정 실패는 최종 실패, TIMEOUT은 재시도로 분기되는 단위 테스트.
- PG TIMEOUT 후 동일 멱등 키 재호출 시 PG 거래 ID가 같고 지갑 차감 및 `PAYMENT` 원장이 한 건만 생성되는 통합 테스트.
- 동일 결제 내부 반영을 동시에 호출해도 데이터베이스 고유 제약과 결제 잠금으로 한 번만 차감되는 동시성 테스트.
- PG 승인 결과 저장 후 내부 DB 예외가 발생해도 다음 실행에서 PG 승인 API를 호출하지 않는 테스트.
- 잔액 부족 시 보상 성공, 보상 거절, 보상 TIMEOUT, 보상 재시도 한도 초과 테스트.
- 보상 TIMEOUT 후 같은 멱등 키로 재호출하면 동일 보상 거래 ID가 반환되고 보상 로그와 내부 상태·지갑 반영이 중복되지 않는 통합 테스트.
- PG가 멱등 키별 동일 결과를 반환한다는 가상 PG 계약 테스트.
- 고객 취소 흐름에 회귀가 없는 테스트.
- 전체 `clean test jacocoTestReport bootJar`, Flyway 마이그레이션, `git diff --check` 검증.
