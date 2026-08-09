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
