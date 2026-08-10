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
| R-02 | 결제 생성·취소 응답이 처리 이전 상태로 반환됨 (OSIV 1차 캐시) | `service/payment/PaymentFacade.java:34`, `PaymentCancelFacade.java:37` | HTTP 재현 | 치명 |
| R-03 | 테스트가 `main`의 `application.yml`을 로드하지 않음 | `src/test/resources/application.yml` | 테스트 결과 재현 | 높음 |
| R-04 | 경량 대사가 정상 DB에서 오탐만 생성 | `service/reconciliation/ReconciliationService.java:81,73` | 스케줄러 실행 재현 | 높음 |
| R-05 | 대사 스케줄러가 어떤 프로파일에서도 비활성 | `application.yml:42`, `application-docker.yml` | 실행 재현 | 높음 |
| R-06 | 취소 생성 시 원 결제 취소 가능 상태 미검증 | `service/payment/PaymentCancelCreationService.java:47` | HTTP 재현 | 높음 |
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

      취소 API도 동일하다.
      POST /api/v1/payments/1/cancel (PARTIAL 10000)
        → 응답 cancelStatus=PENDING, paymentStatus=COMPLETED
        → 직후 조회 시 결제는 이미 PARTIALLY_CANCELED

R-03  build/test-results 의 모든 SpringBootTest 가 jdbc:h2:mem:<random-uuid> 사용
      paymentdb 를 사용한 테스트 클래스 0건

R-04  --payment.reconciliation.enabled=true 로 기동, 정상 결제 1건 + 잔액부족 1건 후
      Break 4건 발생 / 실제 불일치 0건
        WALLET_BALANCE  wallet=1 기대=0       실제=100000
        WALLET_BALANCE  wallet=2 기대=0       실제=50000
        WALLET_BALANCE  wallet=1 기대=-30000  실제=70000
        PAYMENT_PG_STATUS payment=2 기대=완료 계열 실제=FAILED

R-06  원 결제 상태별 취소 요청 결과
        COMPLETED           → 200, 결제 PARTIALLY_CANCELED 전환        (정상)
        PARTIALLY_CANCELED  → 200, 결제 CANCELED 전환                  (정상)
        CANCELED            → 400 INVALID_CANCEL_AMOUNT                (금액 검증에 우연히 걸림)
        FAILED              → 200, 결제 FAILED 유지 + 취소행 PENDING 잔존 (결함)
      FAILED 결제 취소 후 남는 흔적: 취소이력 [(3, 'PENDING', 999999)]
```

## 3. 조치 계획

### G0. Flyway 버전 배정

본 계획과 승인 대기 중인 Outbox 계획이 모두 `V10`을 사용하고 있어 그대로 구현하면 같은 버전의 migration이 두 개 생겨 Flyway가 기동을 거부한다. 다음과 같이 배정을 고정한다.

| 버전 | 내용 | 소속 계획 | 커밋(§4) |
| --- | --- | --- | --- |
| `V10` | 시드 지갑 대응 거래 보정 (G4-1) | 본 계획 | 10 |
| `V11` | 대사 Break 해소 상태 컬럼과 기존 `break_key` 정규화 (G4-2) | 본 계획 | 10 |
| `V12` | `reconciliation_checkpoint` 테이블 (G9-2) | 본 계획 | 11 |
| `V13` | PG 응답 로그 유형 백필 보정 (G10) | 본 계획 | 14 |
| `V14` 이상 | Outbox 처리 단계·lease·PG 결과 컬럼, `pg_response_log` 고유 제약 | Outbox 계획 | 별도 |

배정에는 두 가지 제약이 있다.

1. **버전 번호는 커밋 순서와 오름차순으로 일치해야 한다.** Flyway는 기본적으로 out-of-order 적용을 허용하지 않으므로, 앞 커밋에서 `V12`가 적용된 환경에 뒤 커밋이 `V11`을 들고 오면 `Detected resolved migration not applied to database: 11`로 기동이 막힌다. 위 표의 커밋 열이 오름차순인지 항상 확인한다.
2. **`V13`(G10)이 Outbox migration보다 앞서야 한다.** 그래야 P-3의 선행 조건(오라벨된 취소 로그 보정 후 고유 제약 추가)이 파일 버전만으로 강제된다. Outbox 계획 승인 시 `implementation-plan.md`의 `V10` 표기를 `V14`로 함께 정정한다.

G4-2와 G9-2를 한 파일로 묶지 않고 `V11`·`V12`로 나눈 이유는 두 조치가 서로 다른 커밋에 속하기 때문이다. 한 파일로 묶으면 앞 커밋에서 이미 적용된 migration을 뒤 커밋이 수정하게 되어, 그 사이에 기동한 환경에서 checksum 오류가 난다. **이미 적용 이력이 있는 migration 파일은 어떤 경우에도 되돌아가 수정하지 않는다.**

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

`docker compose`도 예외가 아니다. 현재 `docker-compose.yml`은 리터럴 키를 커밋해 두고 있으므로 이를 제거하는 G1-4를 함께 반영하고, README에는 Compose 실행 전에도 `PAYMENT_ENCRYPTION_SECRET`을 셸이나 `.env`로 주입해야 한다고 적는다.

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

`UnsatisfiedDependencyException` 스택트레이스 대신 원인이 바로 드러나는 메시지로 실패하게 한다. **`@ConstructorBinding`으로는 이 목적을 달성할 수 없다.** `PaymentSecurityProperties`는 record라 이미 생성자 바인딩 대상이고(Spring Boot 3.x에서 `@ConstructorBinding`은 생성자에만 붙일 수 있으며 타입 선언에 붙이면 오류다), 이 어노테이션은 바인딩 방식만 정할 뿐 값을 검증하지 않는다. 현재 기본값이 `${PAYMENT_ENCRYPTION_SECRET:}`이므로 키가 없으면 빈 문자열이 정상 바인딩되고, `EncryptionKeyConfiguredCondition`만 불충족되어 지금과 똑같은 예외가 난다.

Bean Validation으로 바꾼다. `spring-boot-starter-validation`은 이미 의존성에 있다.

```java
@Validated
@ConfigurationProperties(prefix = "payment.security")
public record PaymentSecurityProperties(
        @NotBlank(message = "PAYMENT_ENCRYPTION_SECRET 환경변수가 필요합니다.") String encryptionKey) {
}
```

`@NotBlank`는 빈 문자열도 거부하므로 `application.yml`의 `${PAYMENT_ENCRYPTION_SECRET:}` 기본값을 그대로 두어도 키 미설정이 걸러진다. 기동은 바인딩 검증 실패 시점에 위 메시지를 그대로 노출하며 멈춘다.

부수 효과: 이 검증이 들어가면 키 없는 Spring 컨텍스트는 더 이상 뜨지 않는다. 따라서 `InfrastructureConfigurationTest.skipEncryptorWithoutKey`는 "키가 없어도 컨텍스트가 뜬다"를 더는 검증할 수 없으므로, `PaymentSecurityProperties`를 제외한 채 `payloadEncryptor` Bean 조건만 확인하도록 축소하거나 폐기한다. G1-2의 주석 정정도 이 결정에 맞춘다.

테스트는 `AuditLogFilter` 의존성 실패가 아니라 **ConfigurationProperties 바인딩 검증 실패와 메시지**를 단언한다. `ApplicationContextRunner`로 키 없는 컨텍스트를 띄워 실패 원인에 "PAYMENT_ENCRYPTION_SECRET 환경변수가 필요합니다."가 포함되는지 확인한다.

**G1-4. `docker-compose.yml`의 리터럴 암호화 키 제거**

`docker-compose.yml`이 `PAYMENT_ENCRYPTION_SECRET: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=`를 소스에 그대로 담고 있다. 이 값은 32바이트 0으로 만든 공개 상수이므로, 기본 Compose로 만들어진 파일 DB의 감사·PG 암호문은 저장소를 열람한 누구나 복호화할 수 있다. 「키를 소스코드나 DB에 저장하지 않고 환경 변수로 주입한다」는 `architecture.md`의 키 관리 원칙과 AUD-002의 인수 조건에 정면으로 어긋난다. 감사 payload 마스킹(G7)을 강화하면서 키를 공개해 두면 그 조치의 의미가 사라지므로 함께 처리한다.

```yaml
environment:
  PAYMENT_ENCRYPTION_SECRET: ${PAYMENT_ENCRYPTION_SECRET:?PAYMENT_ENCRYPTION_SECRET 환경변수가 필요합니다}
```

외부 변수가 없으면 Compose가 컨테이너를 만들기 전에 오류로 멈추므로, 키 없이 기동해 평문 위험을 남기는 경로가 사라진다. README의 Compose 절에는 실행 전 키 생성·주입 예시를 넣고, `.env` 파일을 쓰는 경우 `.gitignore`에 포함되어 있는지 확인한다. §6 검증 항목의 `docker compose up --build`도 키를 주입한 상태와 주입하지 않은 상태를 모두 확인하도록 한다.

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
-- V2 가 거래 이력 없이 삽입한 샘플 지갑 2건에만 최초 충전 거래를 채운다.
-- 고객 식별자와 V2 의 시드 기초금액을 상수로 고정해 운영 데이터에는 적용되지 않게 한다.
INSERT INTO wallet_transaction (wallet_id, transaction_type, amount, balance_after, idempotency_key, created_at)
SELECT w.id, 'TOP_UP', s.seed_amount, s.seed_amount, 'SEED-TOPUP-' || w.customer_id, w.created_at
  FROM wallet w
  JOIN (SELECT 'CUST-001' AS customer_id, 100000 AS seed_amount
        UNION ALL
        SELECT 'CUST-002' AS customer_id,  50000 AS seed_amount) s
    ON s.customer_id = w.customer_id
 WHERE NOT EXISTS (SELECT 1
                     FROM wallet_transaction t
                    WHERE t.wallet_id = w.id
                      AND t.idempotency_key = 'SEED-TOPUP-' || w.customer_id);
```

**조건을 고정 시드 금액과 시드 거래 키로 잡는 이유**: 대상을 고객 식별자로 한정하는 것만으로는 부족하고, **현재 잔액이나 전체 거래 부재를 조건에 넣으면 안 된다.** 이 migration이 실제로 필요한 곳은 이미 사용 이력이 쌓인 파일 DB인데, 그런 DB에서는 두 조건이 모두 깨지기 때문이다. `CUST-001`로 30,000원 결제가 처리된 DB는 잔액이 70,000원이고 `PAYMENT` 거래도 존재하므로 `balance = 100000`과 `거래 없음`을 함께 요구하면 보정 대상에서 빠진다. 그 결과 §2.1이 재현한 `WALLET_BALANCE wallet=1 기대=-30000 실제=70000` 오탐이 마이그레이션 후에도 그대로 남는다. 신규 인메모리 DB에서는 V10이 사용 이전에 적용되므로 통과해 결함이 드러나지 않는 것도 함정이다.

따라서 삽입 금액은 현재 잔액이 아니라 V2가 넣은 **기초금액 상수**(100,000 / 50,000)로 쓰고, 중복 방지는 전체 거래 부재가 아니라 **`SEED-TOPUP-<고객ID>` 멱등키를 가진 거래의 부재**로 판정한다. `created_at`도 `CURRENT_TIMESTAMP`가 아니라 지갑 생성 시각을 써서 기초 충전이 이후 거래보다 앞서도록 한다. 이렇게 하면 사용 이력이 있든 없든 기대 잔고가 `기초금액 + 이후 거래 순합계`로 맞아떨어지고, 재실행해도 중복 삽입되지 않는다.

시드 2건으로 한정하는 것 자체는 유지한다. `balance > 0 AND 거래 없음`처럼 넓게 잡으면 장애나 수동 변경으로 거래 이력만 유실된 실제 지갑에도 가짜 `TOP_UP`이 삽입되어 대사가 탐지해야 할 불일치를 영구히 지워버린다. 이는 "대사는 금전 원장을 자동 보정하지 않는다"는 현행 정책과 정면으로 충돌한다. 조건에 맞지 않는 무거래·양수 잔액 지갑은 보정 대상이 아니라 **`WALLET_BALANCE` Break로 탐지되어야 하는 정상적인 탐지 대상**이다.

테스트는 두 가지를 모두 고정한다.

- 신규 DB: migration 적용 후 시드 지갑 2건에만 거래가 생기고 다른 지갑은 변경되지 않는다.
- **업그레이드 DB**: V9까지 적용된 DB에 `CUST-001`의 `PAYMENT` 30,000원 거래를 넣어 잔액을 70,000원으로 만든 뒤 V10을 실행하고, 대사가 계산한 기대 잔고가 실제 잔고와 일치해 Break가 생기지 않는지 확인한다.

V2는 checksum 보호를 위해 수정하지 않는다.

**G4-2. 동일 지갑 Break 중복 누적**

원인: `breakKey`에 기대·실제 값이 포함되어(`:97`) 값이 바뀔 때마다 새 Break가 생긴다.

```java
// 현재
String breakKey = type.name() + ":" + target + ":" + expected + ":" + actual;
// 변경
String breakKey = type.name() + ":" + target;
```

대상 단위로 1건만 유지하고, 재탐지 시 `expectedValue`/`actualValue`를 갱신하는 `ReconciliationBreak.redetect(...)` 도메인 메서드를 추가한다.

**해소 상태가 없으면 이 조치가 완결되지 않는다.** 현재 `ReconciliationBreak`에는 `detectedAt` 하나뿐이라(`domain/reconciliation/ReconciliationBreak.java`) 원장을 복구한 뒤 다시 대사해도 기존 행이 `findAll()` 결과에 영구히 남는다. 조회하는 쪽은 해소된 건과 아직 검사되지 않은 건을 구분할 수 없고, G9-2가 요구하는 "아직 해소되지 않은 기존 Break"를 저장소에서 판별할 방법도 없다. 다음을 함께 추가한다.

| 항목 | 내용 |
| --- | --- |
| `status` | `OPEN` / `RESOLVED`. 신규 탐지와 재탐지는 `OPEN`, 재검사에서 불일치가 사라지면 `RESOLVED` |
| `lastCheckedAt` | 이 대상이 마지막으로 검사된 시각. 미검사와 해소를 구분한다 |
| `resolvedAt` | `RESOLVED` 전환 시각. `OPEN`이면 `null` |

`detectedAt`은 최초 탐지 시각으로 의미를 고정하고 재탐지 때 갱신하지 않는다. 도메인 메서드는 `redetect(expected, actual, checkedAt)`와 `resolve(checkedAt)` 두 개로 나눈다. 조회 API는 기본적으로 `OPEN`만 반환하고 `status` 질의 파라미터로 전체를 볼 수 있게 하며, 계약 변경이므로 `api-spec.md`의 「경량 대사 Break 조회」 절과 `ReconciliationBreakResponse`에 반영한다.

DDL과 기존 데이터 정규화는 `V11`(G0)에서 함께 처리한다.

```sql
-- V11__add_reconciliation_break_state.sql (일부)
ALTER TABLE reconciliation_break ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'OPEN';
ALTER TABLE reconciliation_break ADD COLUMN last_checked_at TIMESTAMP(6);
ALTER TABLE reconciliation_break ADD COLUMN resolved_at TIMESTAMP(6);

-- 기존 `유형:대상:기대:실제` 키를 `유형:대상`으로 정규화하고 대상당 1건만 남긴다.
DELETE FROM reconciliation_break
 WHERE id NOT IN (SELECT MAX(id) FROM reconciliation_break
                   GROUP BY break_type, COALESCE(payment_id, -1), COALESCE(wallet_id, -1));
UPDATE reconciliation_break
   SET break_key = break_type || ':'
                   || CASE WHEN payment_id IS NOT NULL THEN 'P' || payment_id ELSE 'W' || wallet_id END;
```

**키 정규화를 빠뜨리면 안 되는 이유**: 배포 전에 이미 `WALLET_BALANCE:W1:0:100000` 형식의 행이 저장된 DB에서 새 코드가 같은 불일치를 검사하면 `WALLET_BALANCE:W1`을 별도 행으로 추가한다. 옛 행은 새 키와 충돌하지 않으므로 계속 조회되고, "대상 단위로 1건만 유지"라는 목표가 깨진다. §2.1의 R-04 재현처럼 `--payment.reconciliation.enabled=true`로 한 번이라도 기동한 파일 DB에는 실제로 이 행들이 남아 있다. 삭제를 먼저 하고 `UPDATE`를 뒤에 두어야 고유 제약 위반 없이 병합된다.

기존 형식 행이 있는 DB를 업그레이드한 뒤 재대사해 대상당 1건만 남는 통합 테스트, 그리고 탐지 → 원장 복구 → 재대사에서 `RESOLVED`로 전환되는 시나리오 테스트를 추가한다.

**G4-3. 잔액 부족 결제의 `PAYMENT_PG_STATUS` 오탐**

원인: PG는 승인(`0000`)했는데 잔액 부족으로 내부는 `FAILED`인 상태가 **설계된 정상 동작**인데 불일치로 잡힌다.

다만 이 상태는 "PG에서는 돈이 빠졌는데 내부에는 반영이 없는" 실제 금전 리스크이기도 하다. 승인 대기 중인 보상 취소 계획이 반영되기 전까지는 **정상이 아니라 미해결 리스크**다. 따라서 무조건 제외하지 않고 구분한다.

```java
boolean internallyCompleted = isCompleted(payment.getStatus());
boolean insufficientBalance =
        payment.getStatus() == PaymentStatus.FAILED
        && payment.getFailureReason() == PaymentFailureReason.INSUFFICIENT_BALANCE;
// 승인 금액이 보상 취소로 이미 회수됐는지 PG 로그로 확인한다
boolean compensated = pgLogRepository.existsSuccessfulCompensation(payment.getId());

if (pgApproved && insufficientBalance && !compensated) {
    // 보상 취소가 아직 이뤄지지 않은 구간: 별도 유형으로 분류해 PG 상태 불일치와 섞지 않는다
    record(ReconciliationBreakType.PAYMENT_COMPENSATION_REQUIRED, ...);
} else if (pgApproved != internallyCompleted && !insufficientBalance) {
    record(ReconciliationBreakType.PAYMENT_PG_STATUS, ...);
}
```

`ReconciliationBreakType.PAYMENT_COMPENSATION_REQUIRED`를 추가한다.

**보상 성공 여부를 함께 보지 않으면 Break가 영구히 재생성된다.** 승인 대기 Outbox 계획 §5의 4단계는 보상 성공 시 결제를 `FAILED/INSUFFICIENT_BALANCE`로 남기고 승인 PG 로그도 `0000`인 채 보상 로그만 추가한다(`implementation-plan.md:277`). 즉 보상이 성공해도 결제 상태와 승인 로그는 보상 이전과 구분되지 않는다. 승인 로그와 결제 상태만 보는 판정은 매 주기 `PAYMENT_COMPENSATION_REQUIRED`를 다시 만들거나 갱신하고, 보상이 확정 실패한 경우에는 Outbox가 만드는 `PAYMENT_COMPENSATION_FAILED`와 잘못된 `REQUIRED`가 함께 남는다. 따라서 판정에 반드시 `PgOperationType.PAYMENT_COMPENSATION` 로그의 성공 여부를 포함한다.

- 보상 성공(성공 응답 코드의 보상 로그 존재) → `REQUIRED`를 만들지 않고, 이미 있으면 G4-2의 `resolve(...)`로 `RESOLVED` 전환한다.
- 보상 확정 실패·재시도 한도 초과 → Outbox 계획이 `PAYMENT_COMPENSATION_FAILED`를 남기므로 대사는 `REQUIRED`를 중복 생성하지 않는다.
- 보상 미수행(로그 없음) → `REQUIRED` 유지. 보상 취소 구현 전에는 모든 잔액 부족 승인 건이 여기에 해당한다.

`existsSuccessfulCompensation`은 `PgResponseLogRepository`에 추가하며, 보상 로그 유형(`PgOperationType.PAYMENT_COMPENSATION`)은 Outbox 계획에서 도입되므로 **본 조치는 Outbox 계획과 연동해야 완결된다.** 연동 요구를 §5의 P-9로 명시한다. 보상이 구현되기 전에는 해당 유형과 Repository 메서드가 없으므로, 그때까지는 `compensated`를 항상 `false`로 두는 최소 구현으로 시작하고 Outbox 계획 반영 시 실제 조회로 교체한다.

**G4-4. 검증**

시드 데이터만 있는 상태와 정상 결제·잔액부족 결제를 만든 상태 각각에서 Break가 기대대로만 생성되는지 통합 테스트로 고정한다. 현재 재현한 4건이 0건 또는 `PAYMENT_COMPENSATION_REQUIRED` 1건으로 줄어드는 것이 완료 조건이다.

### G5. 결제·취소 응답 신선도 (R-02)

`spring.jpa.open-in-view`를 명시적으로 끈다.

```yaml
spring:
  jpa:
    open-in-view: false
```

근거: 이 프로젝트의 모든 조회 Service가 `@Transactional(readOnly = true)` 경계를 명시하고 Controller는 DTO만 다루므로 OSIV가 필요 없다. 오히려 요청 스레드에 EntityManager를 묶어 `REQUIRES_NEW`로 갱신된 결과를 가리는 원인이 된다. Spring Boot도 기동 시 이 설정을 명시하도록 경고한다.

부수 효과 점검: OSIV를 끄면 트랜잭션 밖 지연 로딩이 `LazyInitializationException`을 낸다. 이 프로젝트는 연관관계를 식별자 참조로만 관리하고 `@OneToMany`/`@ManyToOne`이 한 곳도 없으므로 영향이 없다. 이 사실을 계획 근거로 명시한다.

영향 범위는 결제 생성 API에 그치지 않는다. `POST /api/v1/payments/{id}/cancel`도 같은 원인으로 `cancelStatus=PENDING`, `paymentStatus`는 취소 반영 이전 값을 반환한다. 두 API 모두 이 조치 하나로 해소된다.

방어 테스트: MockMvc로 두 API를 호출해 응답이 처리 결과와 일치하는지 검증하는 통합 테스트를 추가한다.

- `POST /api/v1/payments` → `status`가 `COMPLETED`
- `POST /api/v1/payments/{id}/cancel` → `cancelStatus`가 `COMPLETED`이고 `paymentStatus`가 `PARTIALLY_CANCELED`

현재 `PaymentApplicationIntegrationTest`는 Service를 직접 호출해 OSIV를 타지 않으므로 이 회귀를 잡지 못한다. 반드시 MockMvc 경로로 검증한다.

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

현재 동작과 조치 후 동작을 상태별로 비교하면 다음과 같다. **완료·부분취소 결제의 정상 취소 동작은 바뀌지 않는다.**

| 원 결제 상태 | 현재 | 조치 후 | 비고 |
| --- | --- | --- | --- |
| `COMPLETED` | 200, 정상 취소 | 동일 | 변화 없음 |
| `PARTIALLY_CANCELED` | 200, 정상 취소 | 동일 | 변화 없음 |
| `CANCELED` | 400 `INVALID_CANCEL_AMOUNT` | 409 `INVALID_PAYMENT_STATE` | 상태 검증이 금액 검증보다 먼저 걸린다 |
| `FAILED` | **200, 취소행 PENDING 잔존** | 409 `INVALID_PAYMENT_STATE` | 결함 해소 |
| `PENDING` | **200, 취소행 PENDING 잔존** | 409 `INVALID_PAYMENT_STATE` | 결함 해소 |

`CANCELED`가 현재 막히는 것은 상태 검증 때문이 아니라 누적 취소액이 원금과 같아져 잔여 취소 가능 금액이 0이 되고 금액 검증에 먼저 걸리기 때문이다. 반면 `FAILED`와 `PENDING`은 누적 취소액이 0이라 잔여액이 원금 전체로 계산되어 금액 검증을 통과한다. 이것이 두 상태만 결함으로 남은 이유다.

`CANCELED`의 응답 코드가 400에서 409로 바뀌는 것은 계약 변경이므로 `api-spec.md`에 반영하고, 기존 400을 유지해야 한다면 상태 검증을 금액 검증 뒤로 옮기는 대안을 선택한다. 본 계획은 의미가 더 정확한 409를 기본안으로 제안한다.

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
private static final Pattern QUERY_SECRET = Pattern.compile(
        "(?i)(^|&)((?:password|token|authorization|cookie|cvv|cvc|secret|apikey)=)[^&]*");
// 치환: "$1$2***"
```

`HttpServletRequest.getQueryString()`은 선행 `?` 없이 `token=secret&x=1` 형태를 반환한다. 경계를 `[?&]`로 잡으면 **첫 번째 파라미터가 마스킹되지 않는다.** `(^|&)`로 문자열 시작을 포함하고 구분자를 캡처해 그대로 복원한다.

테스트는 민감 파라미터가 **첫 번째·중간·마지막**에 오는 세 경우를 모두 포함한다. 토큰을 두 번째 파라미터로만 두면 이 결함을 놓친다.

**G7-2. 마스킹 누락 보완 (R-10)**

- 숫자·null 값: 패턴을 `\"(key)\"\s*:\s*(\"[^\"]*\"|[^,}\s]+)` 형태로 확장해 값 타입과 무관하게 치환한다.
- 이스케이프 따옴표: 값 부분을 `(?:\\\\.|[^\"\\\\])*`로 바꿔 `\"`를 값의 일부로 인식하게 한다.
- **폼 인코딩 본문**: `AuditLogFilter.readable()`은 `form-urlencoded`를 저장 대상으로 취급하는데 본문 마스킹은 `JSON_SECRET`만 적용한다. 그래서 `password=clear&x=1` 형태의 POST 본문은 암호화 전 평문 그대로 남는다. 요청 `Content-Type`이 `application/x-www-form-urlencoded`이면 본문에도 G7-1의 `QUERY_SECRET`을 적용한다. JSON과 폼을 content type으로 분기하고, 판단할 수 없는 경우에는 두 패턴을 모두 적용한다.
- 잘린 본문: `ContentCachingRequestWrapper`의 4096바이트 제한 때문에 값 중간에서 끊기면 종료 따옴표가 없어 정규식이 값을 잡지 못한다. 이때만 **본문 전체를 `"[REDACTED-TRUNCATED]"`로 대체**하는 안전장치를 둔다.

**안전장치의 발동 조건을 좁혀야 한다.** "마스킹 후에도 민감 키가 남아 있으면 전체 대체"로 잡으면 안 된다. 제안한 치환은 값만 `***`로 바꾸고 키는 그대로 두므로, 잘리지 않고 정상 마스킹된 `{"user":"a","password":"***"}`도 항상 조건에 걸려 통째로 버려진다. 그러면 AUD-001이 요구하는 비민감 요청 정보까지 사라진다. 발동 조건은 다음 두 가지를 모두 만족할 때로 한정한다.

1. 실제로 truncation이 발생했다.
2. 그 본문에서 민감 값의 완전한 마스킹을 확인할 수 없다. 즉 민감 키가 있는데 뒤따르는 값이 종료 구분자(`"` 또는 `&`) 없이 끝난다.

**잘림 판정을 `Content-Length`에 의존하면 안 된다.** 현재 코드는 `request.getContentLengthLong() > MAX_BODY_CHARS`로 판정하는데, chunked 전송처럼 `Content-Length` 헤더가 없는 요청에서는 이 값이 `-1`이다. `ContentCachingRequestWrapper`는 그런 요청도 4096바이트에서 캐시를 자르므로, `{"password":"AAAA…`의 종료 따옴표가 경계 밖으로 밀려난 본문은 **실제로 잘렸는데도 1번 조건이 거짓**이 된다. 그러면 JSON 파싱과 정규식 마스킹이 모두 실패한 채 부분 비밀번호가 암호화 payload에 그대로 보존되어 §8의 완료 기준을 위반한다.

캐시 오버플로를 래퍼가 직접 기록하게 바꾼다.

```java
/** 캐시 한도를 넘겨 본문이 잘렸는지 스스로 기록하는 요청 래퍼다. */
static class OverflowAwareRequestWrapper extends ContentCachingRequestWrapper {
    @Getter
    private boolean overflowed;

    OverflowAwareRequestWrapper(HttpServletRequest request) {
        super(request, MAX_BODY_CHARS);
    }

    /** 캐시 한도 초과를 잘림으로 표시한다. */
    @Override
    protected void handleContentOverflow(int contentCacheLimit) {
        this.overflowed = true;
    }
}
```

`truncated` 판정과 안전장치 1번 조건 모두 이 플래그를 쓴다. 응답은 `ContentCachingResponseWrapper`가 전량을 보관하므로 기존의 길이 비교를 유지한다. 테스트에 **`Content-Length`가 없는 4096바이트 초과 요청**을 추가해 잘림이 실제로 감지되는지 고정한다.

가능하면 정규식보다 **JSON 파싱 기반 마스킹을 우선**한다. 파싱에 성공하면 민감 필드만 정확히 치환하고 나머지 구조를 보존할 수 있다. 잘려서 파싱이 불가능한 본문만 위 규칙으로 보수적으로 전체 대체한다.

단위 테스트로 (a) 잘린 본문의 전체 대체, (b) **정상 마스킹된 본문에서 비민감 필드가 보존되는지**, (c) 숫자 CVV, (d) 이스케이프 따옴표, (e) 쿼리 토큰, (f) 폼 인코딩 본문에서 민감 필드가 첫·중간·마지막에 오는 세 경우를 고정한다.

**G7-3. 암호문 길이 상한 (R-11)**

현재 본문만 4096자로 제한하고 URL·쿼리·content type은 제한이 없으며, UTF-8 인코딩과 Base64 확장으로 `VARCHAR(16384)`를 넘을 수 있다.

**상한은 문자 수가 아니라 UTF-8 바이트 수로 잡아야 한다.** 암호화기는 평문을 UTF-8로 변환한 뒤 AES-GCM과 Base64를 적용하므로, 한글은 문자당 3바이트로 팽창한다. 문자 수 8192로 제한하면 한글 본문에서 평문이 24KB가 되어 암호문이 컬럼 한계를 넘는다.

컬럼 한계에서 역산한 값은 다음과 같다.

```
VARCHAR(16384)
  - "v1:" + base64(12바이트 IV) + ":"  = 20자
  → base64 가용 16364자 → 암호문 12273바이트 → GCM 태그 16바이트 제외
  → 암호화 전 평문 최대 약 12257바이트
```

```java
private static final int MAX_PAYLOAD_BYTES = 10240;   // 12257 대비 약 16% 여유

String json = toJson(payload);
byte[] raw = json.getBytes(StandardCharsets.UTF_8);
boolean payloadTruncated = truncated || raw.length > MAX_PAYLOAD_BYTES;
String encryptedPayload = encryptor.encrypt(payloadTruncated ? toJson(shrink(payload)) : json);
```

**JSON을 임의 substring하지 않는다.** 문자열을 자르면 복호화 후 파싱 불가능한 조각이 남는다. 대신 `shrink(payload)`가 본문 필드부터 순서대로 축약해 **유효한 JSON을 유지한 채** 바이트 상한을 맞춘다. 축약 순서는 응답 본문 → 요청 본문 → 쿼리 스트링으로 두고, URL·메서드·상태 코드 같은 식별 정보는 마지막까지 보존한다.

검증 테스트는 ASCII·한글·이모지 각각의 최대 입력에 대해 **실제 암호문 길이가 16384 이하**인지 확인하고, 복호화 결과가 유효한 JSON으로 파싱되는지도 함께 단언한다.

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

**`REQUIRES_NEW`만으로는 부족하다.** 별도 트랜잭션은 rollback-only 범위를 격리할 뿐 예외를 소비하지 않는다. `save()`의 중복키 위반은 보통 메서드 본문이 끝난 뒤 커밋·flush 시점에 발생하므로 Recorder 내부의 try/catch로는 잡히지 않고, 프록시 호출자에게 `DataIntegrityViolationException`이 그대로 전파되어 남은 대상 검사와 체크포인트 전진이 중단된다. 두 실행자가 같은 신규 Break를 동시에 삽입하면 재현된다. 따라서 **호출자가 Recorder 프록시 호출 전체를 감싸 중복키 예외만 소비**한다.

```java
private void record(...) {
    try {
        breakRecorder.record(candidate);            // 프록시 경계 밖에서 커밋 예외까지 받는다
    } catch (DataIntegrityViolationException duplicated) {
        log.debug("동시 탐지로 이미 기록된 Break입니다. key={}", candidate.getBreakKey());
    }
}
```

DB가 지원한다면 원자적 upsert(`MERGE INTO`)로 대체해도 된다. 두 실행자가 같은 Break를 동시에 기록해도 양쪽 모두 나머지 대사를 끝까지 수행하는 동시성 테스트를 추가한다.

**G9-2. 스캔 범위 축소 (R-13)**

현 규모에서는 정상 동작하나 `findAll()` 두 번 + 행당 추가 쿼리 구조는 원장이 커지면 유지할 수 없다. 다음 두 단계로 나눈다.

**단순 lookback 시간 창은 채택하지 않는다.** `createdAt >= now - 24h`로 제한하면 배포 이전에 생성된 결제나 스케줄러가 하루 이상 중단된 구간의 데이터가 **첫 실행부터 영구히 검사 대상에서 빠진다.** 이틀 전에 완료됐지만 PG 로그가 없는 결제는 이후 상태가 바뀌지 않으므로 어떤 주기에서도 다시 선택되지 않는다. 성능을 얻는 대신 대사의 완전성을 잃는 교환이라 목적에 반한다.

체크포인트 방식을 채택한다.

1. `reconciliation_checkpoint` 테이블(단일 행)에 마지막으로 **성공한** 검사의 상한 시각을 기록한다. DDL은 `V12`(G0)에 포함하고, 엔티티·Repository와 함께 최초 조회 시 단일 행이 없으면 생성하는 정책을 명시한다. 행 식별자는 상수로 고정해 두 실행자가 서로 다른 행을 만들지 않게 한다.
2. 각 주기는 `[checkpoint - overlap, now]` 구간을 검사한다. `overlap`(기본 10분)은 경계에서 커밋 타이밍 때문에 누락되는 행을 막는다.
3. 체크포인트가 비어 있는 최초 실행은 **전체 백필 대사**를 수행한 뒤 증분으로 전환한다.
4. 주기가 예외로 끝나면 체크포인트를 전진시키지 않아 다음 주기가 같은 구간을 다시 검사한다.
5. 시간 창과 무관하게 **아직 해소되지 않은 기존 Break(`status = OPEN`)의 대상**은 매 주기 재검사 대상에 포함한다. 그래야 불일치가 해소됐는지 판정하고 G4-2의 `RESOLVED`로 전환할 수 있다.
6. **증분만으로는 과거 원장 훼손을 잡지 못하므로 주기적 전체 대사를 병행한다.** `payment.reconciliation.full-scan-cron`(기본 하루 1회, 트래픽이 적은 시각)을 추가해 시간 창과 무관하게 전체를 검사한다.

6번이 필요한 이유는 증분 후보 선정이 "변경된 행"에 의존하기 때문이다. 최초 대사에서 정상이던 지갑의 오래된 `wallet_transaction` 한 행이 이후 삭제되면 지갑의 `updated_at`은 바뀌지 않고, 삭제된 행 자체도 시간 창 조회에 나타나지 않으며, 기존 Break도 없으므로 그 지갑은 어떤 주기에서도 다시 검사되지 않는다. 이는 G4-1에서 **탐지 대상이라고 명시한 "거래 이력 유실"을 그대로 놓치는 것**이라 단순 lookback을 배제한 근거와도 모순된다. 삭제·수정까지 남기는 변경 저널이나 DB 트리거를 두는 것이 정공법이지만 현재 규모에는 과하므로, 증분 + 주기적 전체 대사 조합을 채택한다. 전체 대사가 부담이 되는 규모에 도달하면 그때 변경 저널을 도입한다.

회귀 테스트로 (a) 최초 실행이 과거 데이터를 검사하는지, (b) 스케줄러 중단 후 재개 시 중단 구간이 검사되는지, (c) 실패한 주기가 체크포인트를 전진시키지 않는지, (d) **오래된 지갑 거래나 PG 로그를 삭제·변경한 뒤 전체 대사에서 Break가 생성되는지**를 고정한다.

후속 과제로 행별 조회를 `GROUP BY` 집계 쿼리로 대체하는 작업을 남긴다. 이번 범위에서는 수행하지 않는다.

### G10. V8 백필 보정 (R-16)

인메모리 H2는 매 기동마다 스키마를 새로 만들므로 영향이 없고, `docker` 프로파일의 파일 DB에 PR #9 이전 데이터가 남아 있을 때만 문제가 된다. V8은 이미 적용된 이력이므로 수정하지 않고 전진 보정 migration을 추가한다.

```sql
-- V13__fix_pg_operation_type_backfill.sql
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
| 6 | `fix: Compose 암호화 키를 필수 외부 변수로 전환` | G1-4 (`docker-compose.yml`, README) |
| 7 | `fix: 취소 생성 시 원 결제 취소 가능 상태 검증` | G6-1, G6-3 |
| 8 | `refactor: 취소 결과 조회 트랜잭션 경계 복원` | G6-2 |
| 9 | `fix: 감사 payload 마스킹 범위와 저장 크기 상한 보정` | G7 |
| 10 | `fix: 경량 대사 오탐 제거와 Break 중복 누적 방지` | G4 (`V10` 시드 보정, `V11` Break 상태·키 정규화) |
| 11 | `fix: 대사 Break 저장 트랜잭션 분리와 증분·전체 대사 도입` | G9 (`V12` `reconciliation_checkpoint`) |
| 12 | `feat: 경량 대사 스케줄러 활성화` | G3 |
| 13 | `refactor: 테스트 패키지 선언 정합과 아키텍처 규칙 스코프 제한` | G8 |
| 14 | `fix: PG 응답 로그 유형 백필 보정` | G10 (`V13`) |
| 15 | `docs: 수정 결과와 의사결정 기록 현행화` | `002-대화-및-의사결정-기록.md`, 본 문서 결과 절 |

2·3번을 먼저 두는 이유는 이후 모든 변경의 검증 신뢰도가 여기에 달려 있기 때문이다. 특히 2번에서 `ddl-auto: validate`가 되살아나면 숨어 있던 매핑 불일치가 드러날 수 있으므로 같은 커밋에서 해소한다.

## 5. 승인 대기 Outbox 계획의 미비점

`implementation-plan.md` §223~310 「Outbox 선점·PG 결과 영속화·보상 취소 구현계획(승인 대기)」을 검토한 결과 다음이 누락되었다. 승인 전에 계획을 보완할 것을 제안한다.

| # | 미비점 | 영향 | 보완 제안 |
| --- | --- | --- | --- |
| P-1 | **재시도 backoff와 다음 실행 시각이 없다.** §2 필드 목록에 `nextAttemptAt`이 없고 Scheduler는 `createdAt < threshold`만 본다. 이전 계획(§109)에는 "backoff가 계산된 RETRY"가 있었는데 이번 계획에서 사라졌다. | RETRY 전환 즉시 다시 후보가 되어 PG 확정 실패에 대해 촘촘한 재시도가 발생한다. 재시도 한도 5회가 수 초 만에 소진될 수 있다. | `next_attempt_at` 컬럼과 지수 backoff를 §2·§3·§6에 추가하고, Scheduler 조회 조건을 `next_attempt_at <= now`로 바꾼다. |
| P-2 | **`WalletTransaction.payment()` 팩터리 변경이 명시되지 않았다.** §2가 "`wallet_transaction.idempotency_key` 고유 제약을 결제 차감에도 사용한다"고 하지만 현재 팩터리는 이 값을 `null`로 넣는다. | 코드 수준 계획으로서 불완전하고, 구현 시 도메인 시그니처·기존 테스트 다수가 함께 바뀐다. | 팩터리 시그니처 변경, 결제 차감 키 생성 규칙(`payment:<paymentId>`), 기존 NULL 행과의 공존을 §2에 명시한다. |
| P-3 | **`pg_response_log`에 `(operation_type, pg_transaction_id)` 고유 제약 추가가 기존 데이터와 충돌할 수 있다.** 특히 R-16으로 과거 취소 로그가 `PAYMENT_APPROVAL`로 오라벨된 상태에서는 중복이 발생할 수 있다. | Flyway migration 실패로 기동 불가. | 제약 추가를 `V14` 이상으로 재배정해 본 계획의 `V13`(G10) 뒤에 적용되도록 파일 버전으로 순서를 강제하고, 그래도 남을 수 있는 중복 행 정리 단계를 migration에 포함한다. |
| P-4 | **`PgErrorType.ERROR` → `CONFIRMED_FAILURE` 개명의 파급 범위가 없다.** | `PgScenario`, `application.yml`의 `default-scenario`, `FakePgClient`, 관련 테스트가 함께 바뀐다. | 영향 파일 목록을 §4에 나열한다. |
| P-5 | **결제 응답 시점 정의가 없다.** 보상 취소가 도입되면 결제 최종 상태 확정이 더 늦어진다. | 클라이언트가 어떤 상태를 언제 받는지 계약이 불명확하다. R-02와 직결된다. | "생성 응답은 PG 처리 후의 최신 상태를 반환하되, 재시도 구간에서는 `PENDING`을 반환할 수 있다"를 API 계약으로 명문화하고 `api-spec.md`에 반영한다. |
| P-6 | **`OutboxStatus.PROCESSING` 추가 시 V7 주석과의 정합이 없다.** V7이 상태 목록을 `(PENDING, RETRY, COMPLETED, FAILED)`로 기록해 두었다. | 스키마 주석이 실제와 어긋난다. | Outbox migration(재배정 후 `V14` 이상)에서 `COMMENT ON COLUMN`으로 갱신한다. 계획서의 `V10` 표기도 함께 정정한다(§3 G0). |
| P-7 | **완료 기준·승인 요청·위험 대응 절이 없다.** 앞선 계획들은 모두 갖추고 있다. | AGENTS.md의 계획 관행과 어긋나고 완료 판정 기준이 불명확하다. | §8 완료 기준, §9 위험과 대응, §10 승인 요청을 추가한다. |
| P-8 | **테스트 설정 섀도잉(R-03)을 전제하지 않는다.** §7의 통합 테스트들이 실제 설정 없이 실행된다. | lease 임계값 등 설정 기반 동작을 검증하지 못한다. | 본 계획 G2를 선행 조건으로 명시한다. |
| P-9 | **보상 취소 결과를 대사 판정에 연결하는 코드 변경이 없다.** §5의 4단계는 보상 성공 시 결제를 `FAILED/INSUFFICIENT_BALANCE`로 남기고 승인 로그도 `0000`인 채로 두므로, 보상 전후가 결제 상태와 승인 로그만으로는 구분되지 않는다. P-1~P-9 어디에도 이를 처리할 변경이 없다. | 본 계획 G4-3의 `PAYMENT_COMPENSATION_REQUIRED`가 보상 성공 후에도 매 주기 재생성된다. 보상 확정 실패 시에는 `PAYMENT_COMPENSATION_FAILED`와 잘못된 `REQUIRED`가 함께 남아 운영자가 실제 미해결 건을 가려낼 수 없다. | 대사 판정이 `PgOperationType.PAYMENT_COMPENSATION` 로그의 성공 여부와 Outbox 처리 단계·상태를 함께 확인하도록 Outbox 계획의 필수 연계 항목으로 추가한다. 보상 성공 시 `REQUIRED`를 `RESOLVED`로 해소하고 확정 실패·한도 초과일 때만 `FAILED`를 유지하는 통합 테스트를 §7에 명시한다. |

R-07(선점 없는 PG 호출)은 이 승인 대기 계획의 §5가 정면으로 다루므로 본 수정계획에서 중복 조치하지 않는다. 다만 승인이 지연되면 리스크가 남으므로, 승인 전 임시 조치로 `OutboxProcessor.process()` 진입 시 `findByIdWithLock`으로 행을 잠그는 최소 변경을 별도 검토할 수 있다.

## 6. 검증

- 각 커밋마다 `./gradlew clean test jacocoTestReport`를 실행하고 116건 이상 통과를 유지한다.
- G2 적용 후 `ddl-auto: validate`가 실제로 동작하는지 로그로 확인한다.
- G5 적용 후 `POST /api/v1/payments` 응답이 `COMPLETED`인지 실제 HTTP로 확인한다.
- G3·G4 적용 후 시드 데이터만 있는 상태에서 Break가 0건인지 실제 기동으로 확인한다.
- 전체 완료 후 `bootJar` 산출물을 키와 함께 기동해 §2.1의 재현 절차를 다시 수행하고 모두 해소되었는지 기록한다.
- Docker CLI가 있는 환경에서 `docker compose up --build` 기동을 확인한다. 키를 주입한 경우 정상 기동하고, 주입하지 않은 경우 Compose가 필수 변수 누락으로 멈추는지 함께 확인한다. 불가하면 미검증 사실을 결과에 명시한다.

## 7. 위험과 대응

| 위험 | 대응 |
| --- | --- |
| G2로 `validate`가 켜지며 숨은 매핑 불일치가 드러나 테스트가 대량 실패 | 이름 변경 커밋 안에서 즉시 해소한다. 해소 규모가 크면 커밋을 분리하되 `validate` 활성화 커밋을 마지막에 둔다 |
| G5로 OSIV를 끄며 지연 로딩 예외 발생 | 연관관계 매핑이 없음을 사전 확인했다. 전체 통합 테스트와 HTTP 스모크로 재확인한다 |
| G4-1 신규 migration이 시드 지갑을 중복 충전하거나, 반대로 사용 이력이 있는 시드 지갑을 건너뛴다 | 중복 방지는 전체 거래 부재가 아니라 `SEED-TOPUP-<고객ID>` 멱등키 부재로 판정하고, 삽입 금액은 현재 잔액이 아닌 V2 기초금액 상수를 쓴다. 신규 DB와 사용 이력이 있는 업그레이드 DB 양쪽으로 테스트한다 |
| G1-4로 Compose 필수 변수를 도입해 기존 `docker compose up` 절차가 실패 | README와 §6 검증 절차에 키 생성·주입 단계를 함께 넣고, 키 없이 실행하면 어떤 메시지로 멈추는지도 안내한다 |
| G4-2 Break 상태 도입으로 조회 API 응답이 바뀜 | `api-spec.md`에 `status`·`resolvedAt` 추가와 기본 `OPEN` 필터를 명시하고, 기존 필드는 제거하지 않는다 |
| G8 패키지 정리로 테스트 접근성 문제 발생 | package-private 접근 여부를 먼저 확인하고, 필요한 파일은 선언 대신 디렉터리를 옮긴다 |
| G10의 `CANCEL-` 접두사 식별이 Fake PG 구현에 의존 | 한계를 migration 주석에 남기고 실제 PG 도입 시 재검토 대상으로 문서화한다 |
| 커밋 15개로 이력이 길어짐 | 각 커밋을 독립 검증 가능하게 유지하고 리뷰 단위를 작게 가져간다 |

## 8. 완료 기준

- 소스만 전달받은 사람이 README만 보고 빌드·기동·검증까지 수행할 수 있다.
- `POST /api/v1/payments` 응답 상태가 실제 처리 결과와 일치한다.
- 모든 Spring 통합 테스트가 `main`의 `application.yml`을 기준으로 실행되고 `ddl-auto: validate`가 적용된다.
- 대사 스케줄러가 기본 활성이며 정상 데이터에서 Break가 생성되지 않는다.
- 취소 불가 결제에 대한 취소 요청이 4xx로 거절되고 고아 레코드를 남기지 않는다.
- 감사 payload에 마스킹되지 않은 비밀값이 남지 않고 저장 실패가 발생하지 않는다. JSON·폼 인코딩 본문과 쿼리 스트링을 모두 포함한다.
- 암호화 키가 소스에 남아 있지 않다. 키 없이 실행하면 원인을 알 수 있는 메시지로 기동이 멈춘다.
- 사용 이력이 있는 기존 파일 DB를 업그레이드해도 시드 지갑 오탐이 사라진다.
- 불일치가 해소된 Break가 `RESOLVED`로 구분되고, 미검사 대상과 혼동되지 않는다.
- 테스트 `package` 선언과 디렉터리가 일치하며 ArchUnit이 운영 코드만 검사한다.
- 승인 대기 Outbox 계획에 P-1~P-9 보완이 반영되어 있다.

## 9. 승인 요청 사항

1. R-01은 코드로 기동을 허용하지 않고 문서·주석·오류 메시지 개선으로 대응한다. (G1)
2. 테스트 설정을 `application-test.yml` + `@ActiveProfiles("test")`로 분리하고, 그 결과 드러나는 스키마 불일치를 같은 커밋에서 해소한다. (G2)
3. `spring.jpa.open-in-view: false`를 명시한다. (G5)
4. 시드 지갑에 대응하는 `TOP_UP` 거래를 신규 migration으로 추가해 대사 기준을 요구사항에 맞춘다. (G4-1)
5. 잔액 부족 후 PG 승인 상태는 `PAYMENT_COMPENSATION_REQUIRED`로 분류하고 보상 취소 구현 시 연결한다. (G4-3)
6. 대사 스케줄러를 기본·docker 프로파일 모두에서 활성화한다. (G3)
7. `docker-compose.yml`의 리터럴 암호화 키를 필수 외부 변수로 바꾼다. 키를 주입하지 않으면 Compose 실행이 실패하는 동작 변경을 포함한다. (G1-4)
8. 대사 Break에 `OPEN`/`RESOLVED` 상태를 추가하고 조회 API 기본값을 `OPEN`으로 바꾼다. 응답 계약 변경이므로 `api-spec.md`를 함께 갱신한다. (G4-2)
9. 증분 대사에 더해 하루 1회 전체 대사를 병행한다. 과거 원장 훼손 탐지를 위한 것으로 스케줄 부하가 늘어난다. (G9-2)
10. 승인 대기 Outbox 계획에 P-1~P-9을 반영한 뒤 별도로 승인 절차를 진행한다. (§5)
11. 위 순서대로 `main`에서 분기한 작업 브랜치에서 구현하고 PR을 생성한다.
