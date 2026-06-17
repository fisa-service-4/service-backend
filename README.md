# service-backend

프리랜서 특화 AI 자산관리 플랫폼의 메인 백엔드 서버입니다.
사용자 인증, 계좌/증권 조회, 가상월급 관리, AI 채팅 연동 등 서비스 전반의 API Gateway 및 BFF 역할을 수행합니다.

---

## 목차

- [서비스 개요](#서비스-개요)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [주요 기능](#주요-기능)
- [프로젝트 구조](#프로젝트-구조)
- [스케줄러](#스케줄러)
- [API 명세](#api-명세)
- [환경 변수](#환경-변수)
- [실행 방법](#실행-방법)

---

## 서비스 개요

불규칙한 수입을 가진 프리랜서를 위한 통합 금융 관리 플랫폼의 핵심 서버입니다.

| 기능 | 설명 |
|------|------|
| 인증 | 회원가입, 로그인, JWT 발급, PIN 인증, 휴대폰 본인확인 |
| 계좌 관리 | 계좌 역할 설정, 거래내역 조회, 잔액 조회 |
| 이체 | 계좌 이체 요청 및 승인 (Saga 기반) |
| 증권 | 주문 생성/취소, 보유 종목, 체결 내역, 관심종목 |
| 가상월급 | 계약 등록, 입금 매칭, 자동 분배, 대시보드 |
| AI 채팅 | 세션/메시지 관리, AI 서버 연동, 분석 파이프라인 트리거 |
| 마이데이터 | 금융기관 연동 및 동기화 |
| 관리자 | 사용자 관리, 로그 조회, 모니터링 대시보드 |

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| ORM | Spring Data JPA |
| Database | PostgreSQL 16 (운영/분석/로그 멀티 DataSource) |
| Cache | Redis 7.2 |
| Auth | Firebase Admin SDK (휴대폰 본인확인) |
| Docs | Springdoc OpenAPI (Swagger UI) |
| Code Quality | Spotless (Google Java Format), SonarCloud, JaCoCo |
| Container | Docker, Docker Compose |

---

## 아키텍처

```
Frontend (Next.js)
      ↕ HTTP (JWT)
service-backend (Spring Boot :8080)
      ├─ 운영 DB  (PostgreSQL :5432/finance)
      ├─ 로그 DB  (PostgreSQL :5432/finance_log)
      ├─ 분석 DB  (PostgreSQL :5432/finance_analytics)
      ├─ Redis    (:6379)
      ├─ transaction-server (:8083)  ← 계좌/이체/주식/포트폴리오
      ├─ mydata-server      (:8084)  ← 마이데이터 연동
      └─ service-ai-server  (:8000)  ← AI 채팅 / 분석 파이프라인
```

### 통신 원칙

- 프론트엔드는 **service-backend만** 직접 호출합니다.
- 은행/증권 원장 접근은 **transaction-server를 경유**합니다.
- AI 채팅 실행은 **service-ai-server에 위임**하고, 세션/메시지 저장은 service-backend가 담당합니다.
- 마이데이터 수집은 **mydata-server를 경유**합니다.
- 모든 요청에 `traceId`를 부여하여 분산 추적을 지원합니다.

### 멀티 DataSource 구성

| DataSource | 용도 | 설정 클래스 |
|---|---|---|
| 운영 DB | 사용자, 계약, AI 채팅, 알림 등 핵심 도메인 | `OperationalJpaConfig` |
| 로그 DB | 로그인 이력, API 호출 로그, AI 사용 로그 | `LogJpaConfig` |
| 분석 DB | 거래 원천 데이터, 자산 스냅샷 | `AnalyticsJpaConfig` |

---

## 주요 기능

### 1. JWT 기반 인증 / 인가

- Access Token (1시간) + Refresh Token (7일) 이중 토큰 구조
- `JwtFilter`에서 모든 요청의 토큰을 검증하고 `SecurityContext`에 userId를 저장
- ADMIN 권한은 별도 Role 클레임으로 관리

### 2. Firebase 휴대폰 본인확인

- 회원가입 시 Firebase Admin SDK를 통해 SMS 인증번호 발송 및 검증
- 인증 완료 시 Redis에 인증 상태를 캐싱하여 가입 흐름을 유지

### 3. PIN 인증

- 6자리 PIN을 bcrypt로 해시하여 `pin_auth` 테이블에 저장
- 연속/반복 숫자 패턴 검사로 취약한 PIN 차단
- 5회 실패 시 잠금 처리 (`locked_yn = true`)

### 4. 가상월급 & 자동 분배

계약 등록 → 입금 매칭 → 자동 분배의 전체 흐름을 처리합니다.

```
계약 등록 (Contract)
    ↓
입금 감지 (PaymentMatching 폴링)
    ↓
자동 매칭 또는 수동 매칭
    ↓
자동 분배 (월급통장 / 비상금통장 / 투자계좌)
    ↓
가상월급 대시보드 갱신
```

- 세율 자동 계산 (BUSINESS/ETC/ARTIST 구분, 3.3%)
- TBC(To Be Confirmed) 상태 관리 및 만료 처리

### 5. AI 채팅 연동

- 세션/메시지 CRUD는 service-backend가 운영 DB에 직접 저장
- AI 응답 생성은 `AiServerClient`를 통해 service-ai-server에 위임
- PIN 인증이 필요한 금융 액션(주문/이체)은 `requirePin` 플래그로 프론트엔드에 전달

### 6. 분석 데이터 동기화

- 5분마다 신규 거래내역을 분석 DB(`analysis_raw_transaction`)로 동기화
- 매월 1일 자정 전체 사용자 AI 파이프라인 비동기 트리거
- 매일 새벽 1시 사용자별 자산 스냅샷 생성

### 7. 전 요청 API 로그 수집

- `ApiCallLogFilter`가 모든 HTTP 요청에 `traceId`를 부여
- 응답 완료 후 비동기로 API 호출 로그를 로그 DB에 저장
- Swagger UI 경로는 로깅 대상에서 제외

---

## 프로젝트 구조

```
src/main/java/com/service/
├── domain/
│   ├── auth/               # 회원가입, 로그인, PIN, 토큰 재발급
│   ├── user/               # 사용자 정보 조회, 알림 설정
│   ├── account/            # 계좌 역할 설정, 거래내역 조회
│   ├── transfer/           # 이체 요청 / 승인 / 결과 조회
│   ├── stock/              # 주문, 체결, 보유종목, 관심종목
│   ├── mydata/             # 마이데이터 연동 및 동기화
│   ├── aichat/             # AI 채팅 세션 / 메시지 관리
│   ├── analytics/          # 분석 데이터 동기화, 자산 스냅샷 스케줄러
│   ├── virtualsalary/      # 계약, 매칭, 가상월급 설정, 자동 분배
│   └── admin/              # 사용자 관리, 로그 조회, 모니터링
└── global/
    ├── client/             # 외부 서버 HTTP 클라이언트
    │   ├── AiServerClient.java
    │   ├── TransactionServerClient.java
    │   ├── BankServerClient.java
    │   └── ServiceHealthClient.java
    ├── config/             # JPA, Security, Redis, Swagger, Async 설정
    ├── exception/          # BusinessException, ErrorCode, GlobalExceptionHandler
    ├── filter/             # ApiCallLogFilter (traceId / API 로그)
    ├── response/           # ApiResponse 공통 응답 포맷
    └── security/           # JwtProvider, JwtFilter
```

---

## 스케줄러

### VirtualSalaryScheduler

| 실행 시간 (KST) | 작업 |
|---|---|
| 매일 00:10 | 가상월급 지급일 처리 (payday 기준 자동 이체) |
| 매일 00:20 | 만료된 TBC 매칭 → FAILED 전환 |
| 매일 00:30 / 12:30 | TBC 사용자 입금 폴링 및 자동 매칭 |
| 매일 00:50 | 분배 미완료 건 재시도 |

### AnalyticsSyncScheduler

| 실행 주기 | 작업 |
|---|---|
| 5분마다 | 신규 거래내역 분석 DB 동기화 |
| 매월 1일 00:00 | 전체 사용자 AI 파이프라인 비동기 트리거 |
| 매일 01:00 | 사용자별 자산 스냅샷 생성 |

---

## API 명세

**Base URL:** `http://localhost:8080/api/v1`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

| 도메인 | 경로 | 주요 기능 |
|---|---|---|
| 인증 | `/auth` | 회원가입, 로그인, PIN, 토큰 재발급 |
| 사용자 | `/users/me` | 내 정보 조회, 알림 설정 |
| 계좌 | `/accounts` | 계좌 조회, 역할 설정, 거래내역 |
| 이체 | `/transfers` | 이체 요청, 승인, 결과 조회 |
| 주문 | `/orders` | 주식 주문 생성/취소/조회 |
| 체결 | `/executions` | 체결 내역 조회 |
| 보유종목 | `/holdings` | 보유 종목, 수익률 조회 |
| 포트폴리오 | `/portfolio` | 통합 자산 조회 |
| 관심종목 | `/favorite-stocks` | 관심종목 등록/삭제/목록 |
| 계약 | `/contracts` | 계약 등록, 목록, 상세 |
| 매칭 | `/payment-matchings` | 매칭 목록, 수동 매칭 |
| 가상월급 | `/virtual-salary` | 설정, 대시보드, 요약, AI 추천 |
| AI 채팅 | `/ai/chat` | 세션 생성/종료, 메시지 전송, 목록 조회 |
| 마이데이터 | `/mydata` | 연동, 조회, 동기화 |
| 관리자 | `/admin` | 사용자 관리, 로그 조회, 대시보드 |
| 알림 | `/notifications` | 알림 목록, 읽음 처리 |

---

## 환경 변수

`application.yaml`을 참고하여 환경에 맞게 설정하세요.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://{HOST}:5432/finance
    username: {DB_USER}
    password: {DB_PASSWORD}

  data:
    redis:
      host: {REDIS_HOST}
      port: 6379

log:
  datasource:
    url: jdbc:postgresql://{HOST}:5432/finance_log

analytics:
  datasource:
    url: jdbc:postgresql://{HOST}:5432/finance_analytics

jwt:
  secret: {JWT_SECRET_256BIT_MINIMUM}
  access-token-expiration: 3600000    # 1시간 (ms)
  refresh-token-expiration: 604800000 # 7일 (ms)

mydata:
  server:
    url: http://{MYDATA_HOST}:8084/mydata/v1

transaction-server:
  url: http://{TRANSACTION_HOST}:8083

ai:
  server:
    url: http://{AI_HOST}:8000/api/v1/ai
```

---

## 실행 방법

### 사전 준비

```bash
# PostgreSQL 및 Redis 실행
docker-compose up -d
```

### 로컬 실행

```bash
# Spotless 포맷 적용 후 빌드
./gradlew spotlessApply build

# 서버 실행
./gradlew bootRun
```

### Docker 빌드

```bash
./gradlew build
docker build -t service-backend .
docker run -p 8080:8080 service-backend
```

### 테스트

```bash
# 전체 테스트 실행 + 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

커버리지 리포트: `build/reports/jacoco/test/html/index.html`
