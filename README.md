# Payment System

결제·취소, 고객 지갑, 외부 PG 연동을 다루는 Spring Boot 기반 레이어드 모놀리스입니다. 현재 기준은 [요구사항 정의서](docs/current/requirements.md)와 [아키텍처 정의서](docs/current/architecture.md)입니다.

## 구현 범위

- 결제 생성과 전액·부분 취소
- 고객 지갑 생성·충전·잔액 조회 및 결제 차감·취소 환불
- 결제·충전·취소 요청 멱등성
- 결제와 함께 저장되는 Transactional Outbox
- 커밋 후 즉시 처리와 `PENDING/RETRY` 복구 스케줄러가 공유하는 `OutboxProcessor`
- 호출 주체·URL·시각·요청·응답 감사 로그와 요청·응답 원문 AES-256-GCM 암호화
- 결제–지갑 금액, 결제–PG 상태, 지갑 거래–잔고 경량 대사 및 Break 조회
- Swagger UI와 JaCoCo HTML 테스트 리포트

## 구조

운영 코드는 역할이 바로 드러나는 `controller`, `service`, `repository`, `domain`, `dto`, `integration`, `scheduler`, `common` 패키지로 구성합니다. 결제와 결제 취소는 하나의 `payment` 도메인으로 관리합니다. 테스트는 `src/test/unit/java`와 `src/test/integration/java` 아래에서 다시 도메인별로 구분합니다.

## 실행과 검증

Java 17이 필요합니다.

```powershell
.\gradlew.bat bootRun
.\gradlew.bat clean test jacocoTestReport bootJar
```

테스트 커버리지 HTML은 `build/reports/jacoco/test/html/index.html`에서 확인합니다.

Docker Compose로 동일한 로컬 환경을 실행할 수 있습니다.

```bash
docker compose up --build -d
docker compose logs -f payment-system
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 상태 확인: `http://localhost:8080/actuator/health`
- 경량 대사 Break: `GET /api/v1/ops/reconciliation-breaks`

로컬 데이터는 `payment-data` 볼륨에 유지됩니다. 데이터까지 초기화할 때만 `docker compose down -v`를 사용합니다. 운영 환경에서는 저장소의 기본 암호화 키를 Secret Manager 등 외부 비밀 저장소로 교체해야 합니다.

## 문서

- [현재 기준 문서 목록](docs/current/README.md)
- [요구사항 정의서](docs/current/requirements.md)
- [아키텍처 정의서](docs/current/architecture.md)
- [API 명세서](docs/current/api-spec.md)
- [구현계획](docs/current/implementation-plan.md)
- [과거 문서](docs/archive)

실제 PG·금융망 연동, 인증·인가, 운영 배포 및 대규모 부하 검증은 현재 범위에 포함하지 않습니다.
