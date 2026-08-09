# Payment System

가상의 외부 결제 사업자 API와 연동해 결제 원장, 고객 지갑, 취소·환불 및 운영 모니터링 정보를 관리하는 Spring Boot 기반 백엔드 프로젝트입니다.

현재 구현 범위와 동작은 [요구사항 정의서](docs/pre-initiation-deliverables/001-결제-시스템-요구사항-정의서.md)를 기준으로 합니다.

## 핵심 범위

- 결제 요청 접수 및 `PENDING` 원장 생성
- 외부 결제 승인 결과 반영
- 고객 지갑 생성, 충전, 잔액 조회 및 결제 차감
- 잔액 부족과 외부·내부 시스템 오류 이력 관리
- 전액·부분 취소와 고객 지갑 환불
- 결제·충전·취소 요청의 멱등성 보장
- 고객용 결제 이력과 운영자용 모니터링 조회
- 외부 API 응답 원문의 암호화 저장
- 결제·취소 원장과 함께 저장되는 Transactional Outbox
- 커밋 후 즉시 처리와 모놀리스 Scheduler의 `PENDING/RETRY` 복구
- API 요청·응답 감사 원문의 AES-256-GCM 암호화 저장

## 결제 처리 흐름

1. 결제 요청을 `PENDING` 상태로 저장합니다.
2. 외부 결제 API에 승인을 요청합니다.
3. 승인 성공 후 고객 지갑 잔액을 확인합니다.
4. 잔액이 충분하면 지갑 차감과 결제의 `COMPLETED` 전환을 하나의 트랜잭션으로 처리합니다.
5. 잔액이 부족하면 `FAILED/INSUFFICIENT_BALANCE`로 기록합니다.
6. 외부 API 오류·타임아웃 또는 내부 장애는 `FAILED/SYSTEM_ERROR`로 기록합니다.

## 기술 기준선

- Java 17
- Spring Boot 3.5.16
- Gradle 8.14.3 Wrapper
- Spring Web MVC, Spring Data JPA, Bean Validation, Actuator
- H2 Database, Flyway
- JUnit 5

## 실행 및 테스트

Windows:

```powershell
.\gradlew.bat bootRun
.\gradlew.bat test
```

macOS/Linux:

```bash
./gradlew bootRun
./gradlew test
```

H2는 인메모리 모드로 사용하므로 애플리케이션을 재시작하면 데이터가 초기화됩니다.

### Docker Compose로 동일 환경 실행

Docker Engine과 Docker Compose v2가 설치된 PC에서는 다음 명령으로 Java 17, 애플리케이션 설정 및 파일 기반 H2 환경을 동일하게 실행할 수 있습니다.

```bash
docker compose up --build -d
docker compose logs -f payment-system
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 상태 확인: `http://localhost:8080/actuator/health`
- 데이터는 `payment-data` named volume에 유지됩니다.

컨테이너를 중지하려면 `docker compose down`을 사용합니다. 데이터까지 초기화하는 `docker compose down -v`는 저장된 개발 데이터를 삭제하므로 명시적으로 초기화할 때만 사용합니다. Compose에 포함된 암호화 키는 로컬 재현 전용이며 운영 환경에서는 secret manager로 교체해야 합니다.

## 문서

- [요구사항 정의서](docs/pre-initiation-deliverables/001-결제-시스템-요구사항-정의서.md): 제품 범위, 기능·비기능 요구사항, 인수 기준 및 확정 정책
- [아키텍처 정의서](docs/pre-initiation-deliverables/002-아키텍처-정의서.md): 레이어드 모놀리스와 정합성 전략
- [현재 구현계획](docs/in-progress-deliverables/009-MVP-단순화-및-레이어드-아키텍처-전환-구현계획.md): 기능 축소와 패키지 전환 계획

운영 코드는 `domain`, `repository`, `service`, `controller`, `dto`, `integration`, `scheduler`, `common` 레이어로 구성합니다.

## 1페이즈 제외 범위

- 실제 PG사 및 금융망 연동
- 인증·인가 및 사용자 개인정보 관리
- 운영 환경 배포, 고가용성 및 대규모 트래픽 대응
- 외부 알림, 관리자 화면 및 대시보드 UI

제외된 기능성 요구사항은 2페이즈부터 검토합니다.
