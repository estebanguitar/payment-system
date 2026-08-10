# 코드 리뷰 지적사항 수정계획

## 1. 문서 정보

| 항목 | 내용 |
| --- | --- |
| 목적 | PR #9 병합(`0939909`) 이후 수행한 코드 리뷰의 전체 지적사항에 대한 코드 수준 수정계획을 확정한다. |
| 상태 | 승인 대기 |
| 대상 기준 | `main` @ `0939909`, 테스트 116개 통과 / LINE 88.3% |
| 검증 방식 | 정적 판독 + 실제 기동·HTTP 재현. 재현한 항목은 근거를 함께 기록한다. |
| 선행 문서 | [구현계획](implementation-plan.md), [요구사항 정의서](requirements.md) |

## 2. 지적사항 목록과 판정

심각도는 과제 제출·시연 관점의 영향도를 기준으로 한다.

| ID | 지적사항 | 위치 | 검증 | 심각도 |
| --- | --- | --- | --- | --- |
| R-01 | 암호화 키 미설정 시 애플리케이션 기동 실패 | `common/audit/AuditLogFilter.java:49` | 기동 재현 | 치명 |
| R-02 | 결제 생성 응답이 항상 `PENDING` (OSIV 1차 캐시) | `service/payment/PaymentFacade.java:34` | HTTP 재현 | 치명 |
| R-03 | 테스트가 `main`의 `application.yml`을 로드하지 않음 | `src/test/resources/application.yml` | 테스트 결과 재현 | 높음 |
| R-04 | 경량 대사가 정상 DB에서 오탐만 생성 | `service/reconciliation/ReconciliationService.java:81,73` | 스케줄러 실행 재현 | 높음 |
| R-05 | 대사 스케줄러가 어떤 프로파일에서도 비활성 | `application.yml:42`, `application-docker.yml` | 실행 재현 | 높음 |
| R-06 | 취소 생성 시 원 결제 취소 가능 상태 미검증 | `service/payment/PaymentCancelCreationService.java:47` | 코드 판독 | 높음 |
| R-07 | Outbox를 선점하지 않고 PG 호출 | `service/outbox/OutboxProcessor.java:42` | 코드 판독 | 높음 |
| R-08 | `PaymentCancelFacade`의 읽기 트랜잭션 경계 유실 | `service/payment/PaymentCancelFacade.java:37,39` | 코드 판독 | 중간 |
| R-09 | 감사 payload에 `queryString` 미마스킹 | `common/audit/AuditLogFilter.java:92` | 코드 판독 | 중간 |
| R-10 | 마스킹 정규식이 잘린 본문·숫자값·이스케이프를 놓침 | `common/audit/AuditLogFilter.java:38` | 코드 판독 | 중간 |
| R-11 | 암호문이 `VARCHAR(16384)`를 초과할 수 있음 | `common/audit/AuditLogFilter.java:94` | 코드 판독 | 중간 |
| R-12 | 대사 중복키 예외를 잡아도 트랜잭션이 rollback-only | `service/reconciliation/ReconciliationService.java:104` | 코드 판독 | 중간 |
| R-13 | 대사가 전체 테이블을 매 주기 스캔 | `service/reconciliation/ReconciliationService.java:39` | 코드 판독 | 중간 |
| R-14 | 테스트 34/42개의 `package` 선언이 디렉터리와 불일치 | `src/test/**` | 스크립트 집계 | 중간 |
| R-15 | ArchUnit이 테스트 클래스까지 스캔 | `PackageArchitectureTest.java:17` | 코드 판독 | 낮음 |
| R-16 | V8 백필이 과거 취소 로그를 승인으로 오라벨 | `db/migration/V8__add_pg_operation_type.sql:2` | 코드 판독 | 낮음(조건부) |
| R-17 | 메서드 본문 FQN 사용(규약 위반) | `scheduler/outbox/OutboxScheduler.java:33` 외 2건 | 코드 판독 | 낮음 |

### 2.1 재현 근거

```text
R-01  java -jar build/libs/payment-system-0.0.1-SNAPSHOT.jar
      → APPLICATION FAILED TO START
        Parameter 3 of constructor in ...AuditLogFilter required a bean of type
        '...PayloadEncryptor' that could not be found.

R-02  POST /api/v1/payments        → "status":"PENDING"
      GET  /api/v1/wallets/CUST-001 → balance 70000 (이미 차감됨)
      GET  /api/v1/customers/CUST-001/payments/1 → "status":"COMPLETED"
      --spring.jpa.open-in-view=false 로 기동 시 POST 응답이 "COMPLETED"

R-03  build/test-results 의 모든 SpringBootTest 가 jdbc:h2:mem:<random-uuid> 사용
      paymentdb 를 사용한 테스트 클래스 0건

R-04  --payment.reconciliation.enabled=true 로 기동, 정상 결제 1건 + 잔액부족 1건 후
      Break 4건 발생 / 실제 불일치 0건
        WALLET_BALANCE  wallet=1 기대=0       실제=100000
        WALLET_BALANCE  wallet=2 기대=0       실제=50000
        WALLET_BALANCE  wallet=1 기대=-30000  실제=70000
        PAYMENT_PG_STATUS payment=2 기대=완료 계열 실제=FAILED
```

## 3. 조치 계획

### G1. 실행 가능성 확보 (R-01)

코드 동작은 바꾸지 않고 **소스 배포 시 실행 가능하도록 문서와 주석을 실제와 일치시킨다.** 암호화 키 없이 기동을 허용하면 감사 원문이 평문으로 남을 수 있으므로 현재의 fail-fast 동작을 유지한다.

**G1-1. `README.md` 「실행과 검증」 절 교체**

- 필수 환경변수 `PAYMENT_ENCRYPTION_SECRET`(Base64 32바이트)를 선행 조건으로 명시한다.
- 키 미설정 시 나타나는 실제 오류 메시지와 원인을 함께 적어 수령자가 자가 진단할 수 있게 한다.
- macOS/Linux와 Windows PowerShell 실행 예시를 모두 제공한다. 현재 README는 PowerShell 명령만 있다.
- 로컬 확인용 키 생성 명령을 제공한다.
- 대사 조회 API가 기본 비활성임을 R-05 조치 전까지 명시한다. R-05를 함께 반영하면 이 문장은 제거한다.

추가할 내용의 요지는 다음과 같다.

```bash
# 로컬 확인용 키 생성
export PAYMENT_ENCRYPTION_SECRET=$(openssl rand -base64 32)

./gradlew clean test jacocoTestReport bootJar
PAYMENT_ENCRYPTION_SECRET=$PAYMENT_ENCRYPTION_SECRET java -jar build/libs/payment-system-0.0.1-SNAPSHOT.jar
```

```powershell
$env:PAYMENT_ENCRYPTION_SECRET = [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))
.\gradlew.bat clean test jacocoTestReport bootJar
java -jar build\libs\payment-system-0.0.1-SNAPSHOT.jar
```

`docker compose`는 `docker-compose.yml`이 키를 주입하므로 추가 설정이 필요 없다는 점도 함께 적는다.

**G1-2. 테스트 주석 정정**

`InfrastructureConfigurationTest.skipEncryptorWithoutKey`의 주석이 사실과 다르다.

```java
// 현재 (사실과 다름)
/** 키 설정이 없으면 암호화 Bean을 만들지 않아 기본 개발 컨텍스트가 기동되는지 확인한다. */

// 변경
/** 키 설정이 없으면 암호화 Bean을 만들지 않는다. 실제 애플리케이션은 AuditLogFilter가
 *  PayloadEncryptor를 필수로 요구하므로 키 없이 기동되지 않는다. */
```

**G1-3. 기동 실패를 원인이 드러나는 형태로 바꾸는 검증 추가**

`PaymentSecurityProperties`에 `@ConstructorBinding` 검증을 추가해 키 누락 시 "PAYMENT_ENCRYPTION_SECRET 환경변수가 필요합니다" 메시지로 실패하게 한다. `UnsatisfiedDependencyException` 스택트레이스보다 수령자가 즉시 원인을 알 수 있다. 통합 테스트 1건(`ApplicationContextRunner`로 키 없는 컨텍스트에서 `AuditLogFilter` 생성 실패와 메시지 확인)을 추가한다.

### G2. 테스트 설정 섀도잉 해결 (R-03)

**해결 방법: 테스트 전용 설정 파일 이름을 분리한다.** `src/test/resources/application.yml`이 존재하는 한 `main`의 동명 파일은 절대 로드되지 않으므로, 파일명을 바꾸는 것 외의 우회는 없다.

1. `src/test/resources/application.yml` → `src/test/resources/application-test.yml`로 이름 변경. 내용은 암호화 키 한 줄 유지.
2. Spring 컨텍스트를 띄우는 통합 테스트 12개에 `@ActiveProfiles("test")` 추가.
3. 이렇게 하면 `main`의 `application.yml`이 기본으로 로드되고 `application-test.yml`이 키만 덮어쓴다.

이 변경으로 되살아나는 것:

- `spring.jpa.hibernate.ddl-auto: validate` — 엔티티와 Flyway 스키마 불일치를 테스트가 잡는다. V6/V8/V9로 추가된 `encrypted_payload`, `payload_truncated`, `operation_type`, `reconciliation_break`가 처음으로 검증 대상이 된다.
- `payment.outbox.*`, `payment.pg.default-scenario`, springdoc, actuator 설정.

**주의**: `validate`가 켜지는 순간 기존에 감춰져 있던 매핑 불일치가 드러나 테스트가 깨질 수 있다. 이름 변경 커밋에서 즉시 `clean test`를 돌려 드러나는 불일치를 같은 커밋에서 해소한다. 이것이 이 조치의 실질적 가치다.

또한 `OutboxConfigurationTest.disableSchedulerByDefault`는 지금 "프로퍼티 부재"로 통과하고 있으므로, 프로파일 적용 후 실제 기본값 `false`로 통과하는지 재확인한다.

### G3. 대사 스케줄러 활성화 (R-05)

G4의 오탐을 먼저 고친 뒤 활성화한다. 순서를 뒤집으면 시연 중 오탐 Break가 노출된다.

1. `src/main/resources/application.yml`
   ```yaml
   payment:
     reconciliation:
       enabled: true          # false → true
       fixed-delay: 300000
   ```
2. `src/main/resources/application-docker.yml`에도 동일 키를 명시해 프로파일 간 동작을 일치시킨다.
3. `ReconciliationSchedulerTest`(신규, 통합)에서 `enabled=true`일 때 빈이 등록되고 `false`일 때 등록되지 않음을 검증한다. `OutboxEnabledConfigurationTest`와 동일한 패턴을 따른다.
4. README의 대사 Break 안내가 실제로 동작하는 상태가 되므로 G1-1에서 넣은 "기본 비활성" 문장을 제거한다.

### G4. 대사 오탐 제거 — 수정 방향 (R-04)

세 가지 오탐 원인이 서로 다르므로 각각 다른 방식으로 처리한다.

**G4-1. 지갑 기대 잔고 계산 (`reconcileWallet`)**

원인: 기대 잔고를 `wallet_transaction` 합계로만 산출하는데, V2 시드 지갑은 거래 이력 없이 잔액을 가진다.

선택지와 판단:

| 방안 | 내용 | 판정 |
| --- | --- | --- |
| A | 지갑에 `opening_balance` 컬럼 추가, 기대값 = 기초잔액 + 거래합계 | 정공법이나 DDL·도메인 변경 필요 |
| B | V2 시드를 `TOP_UP` 거래와 함께 삽입하도록 신규 migration 추가 | **채택** |
| C | 거래가 0건인 지갑을 검사 제외 | 실제 불일치를 놓침 |

방안 B를 채택한다. 요구사항상 지갑은 초기 잔액 0원으로 생성되고 모든 잔액 변경은 거래 이력으로 남아야 하므로(WAL-002, WAL-008), **거래 없이 잔액만 있는 시드 데이터 자체가 요구사항 위반**이다. 대사를 고치는 게 아니라 데이터를 규칙에 맞추는 것이 옳다.

```sql
-- V10__align_sample_wallet_transactions.sql
INSERT INTO wallet_transaction (wallet_id, transaction_type, amount, balance_after, idempotency_key, created_at)
SELECT w.id, 'TOP_UP', w.balance, w.balance, 'SEED-TOPUP-' || w.customer_id, CURRENT_TIMESTAMP
  FROM wallet w
 WHERE w.balance > 0
   AND NOT EXISTS (SELECT 1 FROM wallet_transaction t WHERE t.wallet_id = w.id);
```

V2는 checksum 보호를 위해 수정하지 않는다.

**G4-2. 동일 지갑 Break 중복 누적**

원인: `breakKey`에 기대·실제 값이 포함되어(`:97`) 값이 바뀔 때마다 새 Break가 생긴다.

```java
// 현재
String breakKey = type.name() + ":" + target + ":" + expected + ":" + actual;
// 변경
String breakKey = type.name() + ":" + target;
```

대상 단위로 1건만 유지하고, 재탐지 시 `expectedValue`/`actualValue`/`detectedAt`을 갱신하는 `ReconciliationBreak.redetect(...)` 도메인 메서드를 추가한다. 불일치가 해소되면 다음 주기에 더 이상 갱신되지 않으므로 `detectedAt`으로 최신성을 판단할 수 있다.

**G4-3. 잔액 부족 결제의 `PAYMENT_PG_STATUS` 오탐**

원인: PG는 승인(`0000`)했는데 잔액 부족으로 내부는 `FAILED`인 상태가 **설계된 정상 동작**인데 불일치로 잡힌다.

다만 이 상태는 "PG에서는 돈이 빠졌는데 내부에는 반영이 없는" 실제 금전 리스크이기도 하다. 승인 대기 중인 보상 취소 계획이 반영되기 전까지는 **정상이 아니라 미해결 리스크**다. 따라서 무조건 제외하지 않고 구분한다.

```java
boolean internallyCompleted = isCompleted(payment.getStatus());
boolean compensationPending =
        payment.getStatus() == PaymentStatus.FAILED
        && payment.getFailureReason() == PaymentFailureReason.INSUFFICIENT_BALANCE;

if (pgApproved && compensationPending) {
    // 보상 취소 미구현 구간: 별도 유형으로 분류해 PG 상태 불일치와 섞지 않는다
    record(ReconciliationBreakType.PAYMENT_COMPENSATION_REQUIRED, ...);
} else if (pgApproved != internallyCompleted) {
    record(ReconciliationBreakType.PAYMENT_PG_STATUS, ...);
}
```

`ReconciliationBreakType.PAYMENT_COMPENSATION_REQUIRED`를 추가한다. 보상 취소가 구현되면 이 유형은 보상 실패 시에만 남는다. 승인 대기 계획의 `PAYMENT_COMPENSATION_FAILED`와 연결되는 지점이므로 §5에서 함께 다룬다.

**G4-4. 검증**

시드 데이터만 있는 상태와 정상 결제·잔액부족 결제를 만든 상태 각각에서 Break가 기대대로만 생성되는지 통합 테스트로 고정한다. 현재 재현한 4건이 0건 또는 `PAYMENT_COMPENSATION_REQUIRED` 1건으로 줄어드는 것이 완료 조건이다.

### G5. 결제 응답 신선도 (R-02)

`spring.jpa.open-in-view`를 명시적으로 끈다.

```yaml
spring:
  jpa:
    open-in-view: false
```

근거: 이 프로젝트의 모든 조회 Service가 `@Transactional(readOnly = true)` 경계를 명시하고 Controller는 DTO만 다루므로 OSIV가 필요 없다. 오히려 요청 스레드에 EntityManager를 묶어 `REQUIRES_NEW`로 갱신된 결과를 가리는 원인이 된다. Spring Boot도 기동 시 이 설정을 명시하도록 경고한다.

부수 효과 점검: OSIV를 끄면 트랜잭션 밖 지연 로딩이 `LazyInitializationException`을 낸다. 이 프로젝트는 연관관계를 식별자 참조로만 관리하고 `@OneToMany`/`@ManyToOne`이 한 곳도 없으므로 영향이 없다. 이 사실을 계획 근거로 명시한다.

방어 테스트: MockMvc로 `POST /api/v1/payments`를 호출해 응답 `status`가 `COMPLETED`인지 검증하는 통합 테스트를 추가한다. 현재 `PaymentApplicationIntegrationTest`는 Service를 직접 호출해 OSIV를 타지 않으므로 이 회귀를 잡지 못한다.

### G6. 취소 정합성 (R-06, R-08)

**G6-1. 생성 시점 상태 검증**

```java
Payment payment = paymentRepository.findByIdWithLock(command.getPaymentId())
        .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND));
if (!payment.isCancelable()) {
    throw new InvalidPaymentStateException("완료 또는 부분 취소된 결제만 취소할 수 있습니다.");
}
```

`InvalidPaymentStateException`은 `GlobalExceptionHandler`에서 409로 매핑되므로 별도 처리가 필요 없다. 검증이 트랜잭션 롤백을 유발하므로 취소행·아웃박스·멱등키가 남지 않는다.

**G6-2. 읽기 트랜잭션 경계 복원**

`PaymentCancelFacade.cancel`에 `@Transactional(readOnly = true)`를 붙이면 쓰기 서비스 호출과 충돌하므로, 결과 조회 부분만 별도 메서드로 분리해 경계를 준다.

```java
public PaymentCancelResult cancel(PaymentCancelCommand command) { /* 생성 또는 멱등 해소 */ }

@Transactional(readOnly = true)
public PaymentCancelResult result(Long cancelId) {   // 취소와 원 결제를 한 스냅샷으로 읽는다
    PaymentCancel cancel = ...;
    return PaymentCancelResult.from(cancel, paymentRepository.findById(cancel.getPaymentId())...);
}
```

`this.result(...)` 자기호출은 프록시를 우회하므로 별도 빈(`PaymentCancelResultReader`)으로 분리하거나 Controller에서 두 번째 호출로 나눈다. **별도 빈 분리를 채택한다.** PR #9에서 `PaymentCancelResultService`를 지우며 유실된 경계를 최소 형태로 되돌리는 셈이다.

**G6-3. 예외 은닉 완화**

`PaymentEventListener`/`PaymentCancelEventListener`가 모든 `RuntimeException`을 로그로 삼킨다(`:21`). Outbox 복구가 있으므로 삼키는 것 자체는 타당하나, 로그 레벨을 `warn`에서 `error`로 올리고 `outboxId`와 함께 `paymentId`를 남긴다. 상태 전이 실패(`InvalidPaymentStateException`)는 재시도해도 성공하지 않으므로 별도로 구분해 로깅한다.

### G7. 감사 로그 안전성 (R-09, R-10, R-11)

**G7-1. `queryString` 마스킹 (R-09)**

```java
AuditPayload payload = new AuditPayload(..., redact(request.getQueryString()), ...);
```

쿼리 스트링은 JSON이 아니므로 별도 패턴이 필요하다.

```java
private static final Pattern QUERY_SECRET =
        Pattern.compile("(?i)([?&](?:password|token|authorization|cookie|cvv|cvc|secret|apikey)=)[^&]*");
```

**G7-2. 마스킹 누락 보완 (R-10)**

- 숫자·null 값: 패턴을 `\"(key)\"\s*:\s*(\"[^\"]*\"|[^,}\s]+)` 형태로 확장해 값 타입과 무관하게 치환한다.
- 이스케이프 따옴표: 값 부분을 `(?:\\\\.|[^\"\\\\])*`로 바꿔 `\"`를 값의 일부로 인식하게 한다.
- 잘린 본문: `ContentCachingRequestWrapper`의 4096바이트 제한 때문에 값 중간에서 끊기면 종료 따옴표가 없다. 마스킹 후에도 `"password"` 같은 키가 payload에 남아 있으면 **본문 전체를 `"[REDACTED-TRUNCATED]"`로 대체**하는 안전장치를 둔다. 감사 가치보다 비밀 유출 방지를 우선한다.

단위 테스트로 (a) 잘린 본문, (b) 숫자 CVV, (c) 이스케이프 따옴표, (d) 쿼리 토큰 4가지를 고정한다.

**G7-3. 암호문 길이 상한 (R-11)**

현재 본문만 4096자로 제한하고 URL·쿼리·content type은 제한이 없으며, UTF-8 인코딩과 Base64 확장으로 `VARCHAR(16384)`를 넘을 수 있다.

```java
private static final int MAX_PAYLOAD_CHARS = 8192;   // 암호화·Base64 확장 후 16384 이내

String json = toJson(payload);
boolean payloadTruncated = truncated || json.length() > MAX_PAYLOAD_CHARS;
String encryptedPayload = encryptor.encrypt(
        json.length() > MAX_PAYLOAD_CHARS ? json.substring(0, MAX_PAYLOAD_CHARS) : json);
```

직렬화 **결과**에 상한을 적용하고 `payload_truncated` 플래그로 잘림을 남긴다. 상한값은 AES-GCM + Base64 팽창률(약 1.37배 + 태그·IV)을 근거로 8192자로 잡는다. 최대 입력에서 암호문 길이가 컬럼 한계 이내인지 확인하는 단위 테스트를 추가한다.

### G8. 구조·규약 정리 (R-14, R-15, R-17)

R-14와 R-15는 반드시 같은 커밋에서 처리한다. 패키지 선언을 고치면 현재 우연히 통과 중인 ArchUnit 규칙이 깨지기 때문이다.

1. 테스트 34개의 `package` 선언을 물리 경로에 맞춘다. 테스트가 운영 코드의 package-private 요소에 접근하지 않는지 먼저 확인하고, 접근하는 파일은 반대로 디렉터리를 옮긴다.
2. `PackageArchitectureTest`에 스코프 제한을 적용한다.
   ```java
   classes = new ClassFileImporter()
           .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
           .importPackages("com.example.paymentsystem");
   ```
3. FQN 3건을 import로 교체한다.
   - `scheduler/outbox/OutboxScheduler.java:33` — `java.time.Duration`
   - `unit/.../AuditLogFilterTest.java` — `jakarta.servlet.http.HttpServletResponse`
   - `unit/.../OutboxServiceTest.java` — `org.mockito.ArgumentMatchers`

### G9. 대사 실행 안전성·성능 (R-12, R-13)

**G9-1. rollback-only 회피 (R-12)**

`reconcile()`의 `@Transactional`을 제거하고, Break 저장만 별도 빈의 `REQUIRES_NEW`로 수행한다. 대사는 읽기 위주 작업이고 Break는 서로 독립적이므로 한 건의 중복키가 전체 회차를 무효화해서는 안 된다.

```java
@Service
class ReconciliationBreakRecorder {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(ReconciliationBreak candidate) { ... }   // 중복키는 이 트랜잭션만 롤백
}
```

**G9-2. 스캔 범위 축소 (R-13)**

현 규모에서는 정상 동작하나 `findAll()` 두 번 + 행당 추가 쿼리 구조는 원장이 커지면 유지할 수 없다. 다음 두 단계로 나눈다.

- 이번 조치: 검사 대상을 시간 창으로 제한한다. `payment.reconciliation.lookback-minutes`(기본 1440)를 추가하고 `createdAt >= now - lookback`인 결제, 해당 기간에 거래가 발생한 지갑만 조회하는 Repository 메서드를 추가한다.
- 후속 과제: 행별 조회를 `GROUP BY` 집계 쿼리로 대체한다. 이번 범위에서는 수행하지 않고 문서에 남긴다.

### G10. V8 백필 보정 (R-16)

인메모리 H2는 매 기동마다 스키마를 새로 만들므로 영향이 없고, `docker` 프로파일의 파일 DB에 PR #9 이전 데이터가 남아 있을 때만 문제가 된다. V8은 이미 적용된 이력이므로 수정하지 않고 전진 보정 migration을 추가한다.

```sql
-- V11__fix_pg_operation_type_backfill.sql
UPDATE pg_response_log
   SET operation_type = 'PAYMENT_CANCEL'
 WHERE operation_type = 'PAYMENT_APPROVAL'
   AND pg_transaction_id LIKE 'CANCEL-%';
```

`FakePgClient`가 취소 거래 ID에 `CANCEL-` 접두사를 붙이므로(`deterministicId("CANCEL", ...)`) 사후 식별이 가능하다. 이 식별 규칙이 Fake PG 구현에 의존한다는 한계를 migration 주석과 위험 표에 명시한다.

## 4. 커밋 순서

각 커밋은 컴파일과 전체 테스트를 통과해야 한다. 기능 수정과 대규모 이동을 섞지 않는다.

| # | 커밋 | 포함 |
| --- | --- | --- |
| 1 | `docs: 코드 리뷰 지적사항 수정계획 수립` | 본 문서, `docs/current/README.md` 링크 |
| 2 | `fix: 테스트 설정 프로파일 분리와 스키마 검증 복원` | G2 |
| 3 | `fix: OSIV 비활성화로 결제 응답 상태 정합성 확보` | G5 |
| 4 | `docs: 소스 배포용 실행 안내와 암호화 키 선행 조건 명시` | G1-1, G1-2 |
| 5 | `feat: 암호화 키 누락을 원인이 드러나는 기동 실패로 전환` | G1-3 |
| 6 | `fix: 취소 생성 시 원 결제 취소 가능 상태 검증` | G6-1, G6-3 |
| 7 | `refactor: 취소 결과 조회 트랜잭션 경계 복원` | G6-2 |
| 8 | `fix: 감사 payload 마스킹 범위와 저장 크기 상한 보정` | G7 |
| 9 | `fix: 경량 대사 오탐 제거와 Break 중복 누적 방지` | G4 |
| 10 | `fix: 대사 Break 저장 트랜잭션 분리와 검사 범위 제한` | G9 |
| 11 | `feat: 경량 대사 스케줄러 활성화` | G3 |
| 12 | `refactor: 테스트 패키지 선언 정합과 아키텍처 규칙 스코프 제한` | G8 |
| 13 | `fix: PG 응답 로그 유형 백필 보정` | G10 |
| 14 | `docs: 수정 결과와 의사결정 기록 현행화` | `002-대화-및-의사결정-기록.md`, 본 문서 결과 절 |

2·3번을 먼저 두는 이유는 이후 모든 변경의 검증 신뢰도가 여기에 달려 있기 때문이다. 특히 2번에서 `ddl-auto: validate`가 되살아나면 숨어 있던 매핑 불일치가 드러날 수 있으므로 같은 커밋에서 해소한다.

## 5. 승인 대기 Outbox 계획의 미비점

`implementation-plan.md` §223~310 「Outbox 선점·PG 결과 영속화·보상 취소 구현계획(승인 대기)」을 검토한 결과 다음이 누락되었다. 승인 전에 계획을 보완할 것을 제안한다.

| # | 미비점 | 영향 | 보완 제안 |
| --- | --- | --- | --- |
| P-1 | **재시도 backoff와 다음 실행 시각이 없다.** §2 필드 목록에 `nextAttemptAt`이 없고 Scheduler는 `createdAt < threshold`만 본다. 이전 계획(§109)에는 "backoff가 계산된 RETRY"가 있었는데 이번 계획에서 사라졌다. | RETRY 전환 즉시 다시 후보가 되어 PG 확정 실패에 대해 촘촘한 재시도가 발생한다. 재시도 한도 5회가 수 초 만에 소진될 수 있다. | `next_attempt_at` 컬럼과 지수 backoff를 §2·§3·§6에 추가하고, Scheduler 조회 조건을 `next_attempt_at <= now`로 바꾼다. |
| P-2 | **`WalletTransaction.payment()` 팩터리 변경이 명시되지 않았다.** §2가 "`wallet_transaction.idempotency_key` 고유 제약을 결제 차감에도 사용한다"고 하지만 현재 팩터리는 이 값을 `null`로 넣는다. | 코드 수준 계획으로서 불완전하고, 구현 시 도메인 시그니처·기존 테스트 다수가 함께 바뀐다. | 팩터리 시그니처 변경, 결제 차감 키 생성 규칙(`payment:<paymentId>`), 기존 NULL 행과의 공존을 §2에 명시한다. |
| P-3 | **`pg_response_log`에 `(operation_type, pg_transaction_id)` 고유 제약 추가가 기존 데이터와 충돌할 수 있다.** 특히 R-16으로 과거 취소 로그가 `PAYMENT_APPROVAL`로 오라벨된 상태에서는 중복이 발생할 수 있다. | Flyway V10이 실패해 기동 불가. | 제약 추가 전에 R-16 보정(G10)을 선행 조건으로 명시하고, 중복 행 정리 단계를 migration에 포함한다. |
| P-4 | **`PgErrorType.ERROR` → `CONFIRMED_FAILURE` 개명의 파급 범위가 없다.** | `PgScenario`, `application.yml`의 `default-scenario`, `FakePgClient`, 관련 테스트가 함께 바뀐다. | 영향 파일 목록을 §4에 나열한다. |
| P-5 | **결제 응답 시점 정의가 없다.** 보상 취소가 도입되면 결제 최종 상태 확정이 더 늦어진다. | 클라이언트가 어떤 상태를 언제 받는지 계약이 불명확하다. R-02와 직결된다. | "생성 응답은 PG 처리 후의 최신 상태를 반환하되, 재시도 구간에서는 `PENDING`을 반환할 수 있다"를 API 계약으로 명문화하고 `api-spec.md`에 반영한다. |
| P-6 | **`OutboxStatus.PROCESSING` 추가 시 V7 주석과의 정합이 없다.** V7이 상태 목록을 `(PENDING, RETRY, COMPLETED, FAILED)`로 기록해 두었다. | 스키마 주석이 실제와 어긋난다. | V10에서 `COMMENT ON COLUMN`으로 갱신한다. |
| P-7 | **완료 기준·승인 요청·위험 대응 절이 없다.** 앞선 계획들은 모두 갖추고 있다. | AGENTS.md의 계획 관행과 어긋나고 완료 판정 기준이 불명확하다. | §8 완료 기준, §9 위험과 대응, §10 승인 요청을 추가한다. |
| P-8 | **테스트 설정 섀도잉(R-03)을 전제하지 않는다.** §7의 통합 테스트들이 실제 설정 없이 실행된다. | lease 임계값 등 설정 기반 동작을 검증하지 못한다. | 본 계획 G2를 선행 조건으로 명시한다. |

R-07(선점 없는 PG 호출)은 이 승인 대기 계획의 §5가 정면으로 다루므로 본 수정계획에서 중복 조치하지 않는다. 다만 승인이 지연되면 리스크가 남으므로, 승인 전 임시 조치로 `OutboxProcessor.process()` 진입 시 `findByIdWithLock`으로 행을 잠그는 최소 변경을 별도 검토할 수 있다.

## 6. 검증

- 각 커밋마다 `./gradlew clean test jacocoTestReport`를 실행하고 116건 이상 통과를 유지한다.
- G2 적용 후 `ddl-auto: validate`가 실제로 동작하는지 로그로 확인한다.
- G5 적용 후 `POST /api/v1/payments` 응답이 `COMPLETED`인지 실제 HTTP로 확인한다.
- G3·G4 적용 후 시드 데이터만 있는 상태에서 Break가 0건인지 실제 기동으로 확인한다.
- 전체 완료 후 `bootJar` 산출물을 키와 함께 기동해 §2.1의 재현 절차를 다시 수행하고 모두 해소되었는지 기록한다.
- Docker CLI가 있는 환경에서 `docker compose up --build` 기동을 확인한다. 불가하면 미검증 사실을 결과에 명시한다.

## 7. 위험과 대응

| 위험 | 대응 |
| --- | --- |
| G2로 `validate`가 켜지며 숨은 매핑 불일치가 드러나 테스트가 대량 실패 | 이름 변경 커밋 안에서 즉시 해소한다. 해소 규모가 크면 커밋을 분리하되 `validate` 활성화 커밋을 마지막에 둔다 |
| G5로 OSIV를 끄며 지연 로딩 예외 발생 | 연관관계 매핑이 없음을 사전 확인했다. 전체 통합 테스트와 HTTP 스모크로 재확인한다 |
| G4-1 신규 migration이 이미 거래가 있는 지갑을 중복 충전 | `NOT EXISTS` 조건으로 거래가 없는 지갑만 대상으로 한다 |
| G8 패키지 정리로 테스트 접근성 문제 발생 | package-private 접근 여부를 먼저 확인하고, 필요한 파일은 선언 대신 디렉터리를 옮긴다 |
| G10의 `CANCEL-` 접두사 식별이 Fake PG 구현에 의존 | 한계를 migration 주석에 남기고 실제 PG 도입 시 재검토 대상으로 문서화한다 |
| 커밋 14개로 이력이 길어짐 | 각 커밋을 독립 검증 가능하게 유지하고 리뷰 단위를 작게 가져간다 |

## 8. 완료 기준

- 소스만 전달받은 사람이 README만 보고 빌드·기동·검증까지 수행할 수 있다.
- `POST /api/v1/payments` 응답 상태가 실제 처리 결과와 일치한다.
- 모든 Spring 통합 테스트가 `main`의 `application.yml`을 기준으로 실행되고 `ddl-auto: validate`가 적용된다.
- 대사 스케줄러가 기본 활성이며 정상 데이터에서 Break가 생성되지 않는다.
- 취소 불가 결제에 대한 취소 요청이 4xx로 거절되고 고아 레코드를 남기지 않는다.
- 감사 payload에 마스킹되지 않은 비밀값이 남지 않고 저장 실패가 발생하지 않는다.
- 테스트 `package` 선언과 디렉터리가 일치하며 ArchUnit이 운영 코드만 검사한다.
- 승인 대기 Outbox 계획에 P-1~P-8 보완이 반영되어 있다.

## 9. 승인 요청 사항

1. R-01은 코드로 기동을 허용하지 않고 문서·주석·오류 메시지 개선으로 대응한다. (G1)
2. 테스트 설정을 `application-test.yml` + `@ActiveProfiles("test")`로 분리하고, 그 결과 드러나는 스키마 불일치를 같은 커밋에서 해소한다. (G2)
3. `spring.jpa.open-in-view: false`를 명시한다. (G5)
4. 시드 지갑에 대응하는 `TOP_UP` 거래를 신규 migration으로 추가해 대사 기준을 요구사항에 맞춘다. (G4-1)
5. 잔액 부족 후 PG 승인 상태는 `PAYMENT_COMPENSATION_REQUIRED`로 분류하고 보상 취소 구현 시 연결한다. (G4-3)
6. 대사 스케줄러를 기본·docker 프로파일 모두에서 활성화한다. (G3)
7. 승인 대기 Outbox 계획에 P-1~P-8을 반영한 뒤 별도로 승인 절차를 진행한다. (§5)
8. 위 순서대로 `main`에서 분기한 작업 브랜치에서 구현하고 PR을 생성한다.
