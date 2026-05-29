# Virtual Salary / Contracts API 테스트 가이드

> Base URL: `http://localhost:8080` / Swagger: `http://localhost:8080/swagger-ui/index.html`
> 사전 데이터 삽입은 `test-data.md` 참조 (oracle-bank STEP 1~2 → postgres-operational STEP 3~4 순서 필수)

---

## 서버 간 통신 구조 전체 그림

```
[Client]
   │
   ▼
service-backend:8080  (Spring Boot — 이 서버의 API를 테스트)
   │
   ├─ BankServerClient.getAccountBalance(accountId)
   │       │
   │       ▼
   │  mydata-server:8084  GET /mydata/v1/bank/accounts/{id}/balance
   │       │
   │       ▼
   │  transaction-server:8083  GET /baas/v1/bank/accounts/{id}/balance
   │       │
   │       ▼
   │  oracle-bank:1524  SELECT balance FROM bank_account WHERE account_id = ?
   │
   └─ AiServerClient.getVirtualSalaryRecommendation(request)
           │
           ▼
      service-ai-server:8000  POST /api/v1/ai/virtual-salary/recommend
      (현재 미구현 엔드포인트 → AI_002 반환)
```

**BankServerClient를 호출하는 API**

| API | 호출 계좌 | 목적 |
|---|---|---|
| `GET /virtual-salary/dashboard` | SALARY (account_id=1005) | currentBalance 계산 |
| `GET /virtual-salary/summary` | SALARY (account_id=1005) | dashboard 포함이므로 동일 |
| `PATCH /payment-matchings/{id}/manual` | INCOME (account_id=1004) | 자동분배 계산 |
| Scheduler (`0 0 9 * * *`) | INCOME (account_id=1004) | paidAmount 계산 |

**외부 서버 호출 없는 API**

`POST/GET/PATCH /virtual-salary`, `POST/GET /contracts`, `GET /contracts/{id}`, `GET /payment-matchings`
→ service-backend PostgreSQL만 사용

---

## 테스트 계정 및 연결 계좌

| 항목 | 값 |
|---|---|
| userId | `5` |
| email | `vstest@test.com` |
| password | `Test1234!` |
| INCOME 계좌 | oracle-bank `account_id=1004`, 잔액 5,000,000원 |
| SALARY 계좌 | oracle-bank `account_id=1005`, 잔액 1,200,000원 |

> 계좌가 없으면 `test-data.md` STEP 1~4를 먼저 실행하세요.

---

## 로그인 (공통 — 토큰 발급)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"vstest@test.com","password":"Test1234!"}'
```

**응답** (`200 OK`)
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "user": { "userId": 5, "name": "가상월급테스터", "email": "vstest@test.com" }
  },
  "meta": { "traceId": "uuid" }
}
```

```powershell
# PowerShell — 토큰 변수 저장
$resp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"email":"vstest@test.com","password":"Test1234!"}'
$TOKEN = $resp.data.accessToken
```

---

## 1. 가상월급 설정 저장 (POST /virtual-salary)

**통신 범위**: service-backend PostgreSQL만 사용

**사전 조건**: 없음 (최초 설정 or 덮어쓰기)

```bash
curl -X POST http://localhost:8080/api/v1/virtual-salary \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetSalary": 3000000,
    "payday": 25,
    "emergencyTargetAmount": 5000000,
    "investmentRatio": 20,
    "emergencyRatio": 30,
    "priorityOrder": ["SALARY", "EMERGENCY", "INVESTMENT"]
  }'
```

**응답** (`201 Created`)
```json
{
  "success": true,
  "data": { "saved": true },
  "meta": { "traceId": "uuid" }
}
```

**DB 결과** (`VIRTUAL_SALARY_SETTING`)
```
user_id=5, target_salary=3000000, payday=25,
emergency_target_amount=5000000, investment_ratio=20, emergency_ratio=30,
priority_order=["SALARY","EMERGENCY","INVESTMENT"]
```

**에러 케이스**

| 상황 | 요청 | 에러 코드 |
|---|---|---|
| investmentRatio + emergencyRatio > 100 | `"investmentRatio":60,"emergencyRatio":50` | `VIRTUAL_SALARY_002` |
| targetSalary 누락 | targetSalary 필드 제외 | `VALID_001` |
| payday 범위 초과 | `"payday":32` | `VALID_001` |

```json
{ "success": false, "error": { "code": "VIRTUAL_SALARY_002", "message": "투자 비율과 비상금 비율의 합은 100을 초과할 수 없습니다." } }
{ "success": false, "error": { "code": "VALID_001", "message": "입력값이 올바르지 않습니다." } }
```

---

## 2. 가상월급 설정 조회 (GET /virtual-salary)

**통신 범위**: service-backend PostgreSQL만 사용

```bash
curl http://localhost:8080/api/v1/virtual-salary \
  -H "Authorization: Bearer $TOKEN"
```

**응답** (`200 OK`)
```json
{
  "success": true,
  "data": {
    "targetSalary": 3000000.00,
    "payday": 25,
    "emergencyTargetAmount": 5000000.00,
    "investmentRatio": 20.00,
    "emergencyRatio": 30.00,
    "priorityOrder": ["SALARY", "EMERGENCY", "INVESTMENT"],
    "updatedAt": "2026-05-29T06:26:00.471063"
  },
  "meta": { "traceId": "uuid" }
}
```

**에러 케이스**
```json
// VIRTUAL_SALARY_SETTING 행 없을 때
{ "success": false, "error": { "code": "VIRTUAL_SALARY_001", "message": "가상월급 설정이 없습니다." } }
```

---

## 3. 가상월급 설정 수정 (PATCH /virtual-salary)

**통신 범위**: service-backend PostgreSQL만 사용

> POST와 동일하게 upsert 동작. 설정이 없으면 새로 생성합니다.

```bash
curl -X PATCH http://localhost:8080/api/v1/virtual-salary \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetSalary": 3500000,
    "payday": 15,
    "emergencyTargetAmount": 6000000,
    "investmentRatio": 25,
    "emergencyRatio": 35,
    "priorityOrder": ["EMERGENCY", "SALARY", "INVESTMENT"]
  }'
```

**응답** (`200 OK`)
```json
{
  "success": true,
  "data": { "saved": true },
  "meta": { "traceId": "uuid" }
}
```

---

## 4. 계약 생성 (POST /contracts)

**통신 범위**: service-backend PostgreSQL만 사용

> `CONTRACT` + `CONTRACT_SETTLEMENT`가 함께 생성됩니다 (Cascade).
> 모든 TaxType의 세율이 현재 3.3%로 구현되어 있습니다.

**세율 계산 공식**: `deductedAmount = contractAmount × 0.033` (소수점 버림)
`actualIncome = contractAmount - deductedAmount`

### 4-1. BUSINESS 계약 (6월 — 목록/상세 조회 테스트용)

```bash
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "(주)카카오",
    "contractAmount": 5000000,
    "expectedPaymentDate": "2026-06-10",
    "taxType": "BUSINESS",
    "memo": "카카오 프론트 개발 계약"
  }'
```

**응답** (`201 Created`)
```json
{
  "success": true,
  "data": {
    "contractId": 1,
    "contractAmount": 5000000,
    "deductedAmount": 165000,
    "actualIncome": 4835000
  },
  "meta": { "traceId": "uuid" }
}
```

> 5,000,000 × 3.3% = 165,000 공제 → 실수령 4,835,000원

### 4-2. ETC 계약 (6월)

```bash
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "네이버클라우드",
    "contractAmount": 3000000,
    "expectedPaymentDate": "2026-06-20",
    "taxType": "ETC"
  }'
```

**응답** (`201 Created`)
```json
{
  "success": true,
  "data": {
    "contractId": 2,
    "contractAmount": 3000000,
    "deductedAmount": 99000,
    "actualIncome": 2901000
  },
  "meta": { "traceId": "uuid" }
}
```

### 4-3. BUSINESS 계약 (5월 — summary BFF 및 수동 매칭 테스트용)

> `expectedPaymentDate`를 **현재 달**로 설정해야 `GET /virtual-salary/summary`에서 계약 목록에 포함됩니다.

```bash
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "삼성SDS",
    "contractAmount": 4000000,
    "expectedPaymentDate": "2026-05-30",
    "taxType": "BUSINESS"
  }'
```

**응답** (`201 Created`)
```json
{
  "success": true,
  "data": {
    "contractId": 3,
    "contractAmount": 4000000,
    "deductedAmount": 132000,
    "actualIncome": 3868000
  },
  "meta": { "traceId": "uuid" }
}
```

**에러 케이스**
```json
// clientName 누락
{ "success": false, "error": { "code": "VALID_001", "message": "입력값이 올바르지 않습니다." } }

// 과거 날짜 (@FutureOrPresent 검증)
{ "success": false, "error": { "code": "VALID_001", "message": "입력값이 올바르지 않습니다." } }
```

---

## 5. 계약 목록 조회 (GET /contracts)

**통신 범위**: service-backend PostgreSQL만 사용

> `date` 미입력 시 현재 월 기준. `expectedPaymentDate`가 해당 월에 속하는 계약만 반환합니다.

```bash
# 현재 월 (5월) 조회 — contract_id=3 (삼성SDS)만 반환
curl http://localhost:8080/api/v1/contracts \
  -H "Authorization: Bearer $TOKEN"

# 6월 조회 — contract_id=1,2 반환
curl "http://localhost:8080/api/v1/contracts?date=2026-06-01" \
  -H "Authorization: Bearer $TOKEN"
```

**응답** (`200 OK`) — `?date=2026-06-01`
```json
{
  "success": true,
  "data": [
    {
      "contractId": 1,
      "clientName": "(주)카카오",
      "contractAmount": 5000000.00,
      "actualIncome": 4835000.00,
      "expectedPaymentDate": "2026-06-10",
      "contractStatus": "PENDING"
    },
    {
      "contractId": 2,
      "clientName": "네이버클라우드",
      "contractAmount": 3000000.00,
      "actualIncome": 2901000.00,
      "expectedPaymentDate": "2026-06-20",
      "contractStatus": "PENDING"
    }
  ],
  "meta": { "traceId": "uuid" }
}
```

---

## 6. 계약 상세 조회 (GET /contracts/{contractId})

**통신 범위**: service-backend PostgreSQL만 사용

```bash
curl http://localhost:8080/api/v1/contracts/1 \
  -H "Authorization: Bearer $TOKEN"
```

**응답** (`200 OK`)
```json
{
  "success": true,
  "data": {
    "contractId": 1,
    "clientName": "(주)카카오",
    "contractAmount": 5000000.00,
    "taxRate": 0.033,
    "deductedAmount": 165000.00,
    "actualIncome": 4835000.00,
    "taxType": "BUSINESS",
    "expectedPaymentDate": "2026-06-10",
    "actualPaymentDate": null,
    "contractStatus": "PENDING",
    "memo": "카카오 프론트 개발 계약"
  },
  "meta": { "traceId": "uuid" }
}
```

**에러 케이스**
```json
// 존재하지 않는 계약
{ "success": false, "error": { "code": "CONTRACT_001", "message": "존재하지 않는 계약입니다." } }

// 타인 계약 접근
{ "success": false, "error": { "code": "CONTRACT_002", "message": "본인 계약이 아닙니다." } }
```

---

## 7. 가상월급 대시보드 조회 (GET /virtual-salary/dashboard)

**통신 범위**: ⚡ 외부 서버 호출 포함

```
service-backend
  └─ BankServerClient.getAccountBalance(1005)   ← SALARY 계좌 account_id
       └─ GET mydata-server:8084/mydata/v1/bank/accounts/1005/balance
            └─ GET transaction-server:8083/baas/v1/bank/accounts/1005/balance
                 └─ oracle-bank: SELECT balance FROM bank_account WHERE account_id=1005
                      → 1,200,000원
```

**사전 조건**
- `VIRTUAL_SALARY_SETTING` 존재 (POST로 먼저 저장)
- `ACCOUNT_MAPPING` 에 `mapping_type=SALARY` 행 존재
- oracle-bank `bank_account.account_id=1005`, `balance=1200000`, `account_status=ACTIVE`

**중간 서버 응답 확인** (디버깅용)
```bash
# mydata-server 직접 조회
curl http://localhost:8084/mydata/v1/bank/accounts/1005/balance
# → {"success":true,"data":{"accountId":1005,"balance":1200000,"availableBalance":null},...}

# transaction-server 직접 조회
curl http://localhost:8083/baas/v1/bank/accounts/1005/balance
# → {"success":true,"data":{"accountId":1005,"balance":1200000,"updatedAt":"..."},...}
```

```bash
curl http://localhost:8080/api/v1/virtual-salary/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

**응답** (`200 OK`)
```json
{
  "success": true,
  "data": {
    "targetSalary": 3000000.00,
    "currentBalance": 1200000,
    "remainAmount": 1200000,
    "usedAmount": 1800000.00,
    "progressRate": 60.00,
    "payday": 25,
    "dday": 27
  },
  "meta": { "traceId": "uuid" }
}
```

**계산 근거**
| 필드 | 계산식 | 값 |
|---|---|---|
| currentBalance | oracle-bank `balance` | 1,200,000 |
| usedAmount | targetSalary - currentBalance | 3,000,000 - 1,200,000 = 1,800,000 |
| progressRate | usedAmount / targetSalary × 100 | 60% |
| dday | 다음 payday(25일)까지 남은 일수 | 27 (테스트일 기준) |

**에러 케이스**

| 상황 | 에러 코드 |
|---|---|
| VIRTUAL_SALARY_SETTING 없음 | `VIRTUAL_SALARY_001` |
| ACCOUNT_MAPPING에 SALARY 타입 없음 | `VIRTUAL_SALARY_003` |
| mydata-server/transaction-server/oracle-bank 중 하나라도 장애 | 내부 서버 오류 (500) |

```json
{ "success": false, "error": { "code": "VIRTUAL_SALARY_003", "message": "SALARY 계좌가 연결되어 있지 않습니다." } }
```

---

## 8. 가상월급 홈 통합 조회 — BFF (GET /virtual-salary/summary)

**통신 범위**: ⚡ 외부 서버 호출 포함 (dashboard와 동일 체인)

```
service-backend
  ├─ VirtualSalaryDashboardService.getDashboard()
  │    └─ BankServerClient.getAccountBalance(1005)  [dashboard와 동일 체인]
  │
  └─ ContractService.getContracts(이번달)
       └─ PostgreSQL: CONTRACT WHERE expectedPaymentDate 이번달
```

**사전 조건**
- STEP 7(dashboard)의 사전 조건 동일
- 이번달 기준 계약이 있어야 `contracts`, `calendarData` 필드가 채워집니다
  (4-3에서 생성한 5월 삼성SDS 계약, `expectedPaymentDate=2026-05-30`)

```bash
curl http://localhost:8080/api/v1/virtual-salary/summary \
  -H "Authorization: Bearer $TOKEN"
```

**응답** (`200 OK`) — 이번달 계약 있을 때
```json
{
  "success": true,
  "data": {
    "dashboard": {
      "targetSalary": 3000000.00,
      "currentBalance": 1200000,
      "progressRate": 60.00,
      "dday": 27
    },
    "monthlyExpectedIncome": 3868000.00,
    "contracts": [
      {
        "contractId": 3,
        "clientName": "삼성SDS",
        "expectedPaymentDate": "2026-05-30",
        "actualIncome": 3868000.00,
        "contractStatus": "PENDING"
      }
    ],
    "calendarData": [
      { "date": "2026-05-30", "amount": 3868000.00 }
    ]
  },
  "meta": { "traceId": "uuid" }
}
```

**응답** — 이번달 계약 없을 때 (`contracts: [], calendarData: [], monthlyExpectedIncome: 0`)

> `monthlyExpectedIncome`은 이번달 계약들의 `actualIncome` 합산입니다.
> `calendarData`는 `expectedPaymentDate`로 그룹핑, 날짜 오름차순 정렬입니다.

---

## 9. 매칭 목록 조회 (GET /payment-matchings)

**통신 범위**: service-backend PostgreSQL만 사용

**사전 조건**: `PAYMENT_MATCHING` 행 존재 (test-data.md STEP 8)

```sql
-- 사전 삽입 (직접 SQL)
INSERT INTO payment_matching (contract_id, bank_transaction_id, matching_status, matched_by, matched_at)
VALUES (3, 9001, 'TBC', 'SYSTEM', NULL);
-- → matching_id = 1
```

```bash
# 전체 조회
curl http://localhost:8080/api/v1/payment-matchings \
  -H "Authorization: Bearer $TOKEN"

# contractId 필터
curl "http://localhost:8080/api/v1/payment-matchings?contractId=3" \
  -H "Authorization: Bearer $TOKEN"

# matchingStatus 필터
curl "http://localhost:8080/api/v1/payment-matchings?matchingStatus=TBC" \
  -H "Authorization: Bearer $TOKEN"

# 날짜 필터 (matchedAt 기준 — TBC는 matchedAt=NULL이므로 날짜 필터에서 제외됨)
curl "http://localhost:8080/api/v1/payment-matchings?from=2026-05-01&to=2026-05-31" \
  -H "Authorization: Bearer $TOKEN"
```

**응답** (`200 OK`)
```json
{
  "success": true,
  "data": [
    {
      "matchingId": 1,
      "contractId": 3,
      "bankTransactionId": 9001,
      "matchingStatus": "TBC",
      "matchedBy": "SYSTEM",
      "matchedAt": null
    }
  ],
  "meta": { "traceId": "uuid" }
}
```

> **필터 동작 주의**
> - `matchingStatus` 필터: Java Stream에서 처리 (PostgreSQL enum null 파라미터 이슈로 수정됨)
> - `from`/`to` 날짜 필터: `matchedAt` 기준이므로 TBC(matchedAt=null) 건은 날짜 필터 결과에서 제외됨

---

## 10. 수동 매칭 처리 (PATCH /payment-matchings/{matchingId}/manual)

**통신 범위**: ⚡ 외부 서버 호출 포함

```
service-backend
  ├─ PaymentMatchingServiceImpl.manualMatch()
  │    ├─ matching.applyManualMatch()    → PAYMENT_MATCHING.matching_status = MANUAL_MATCHED
  │    └─ contract.updateStatus(PAID)   → CONTRACT.contract_status = PAID
  │
  └─ AutoDistributionService.distribute(userId=5, matchingId=1)
       ├─ VirtualSalarySetting 조회     → PostgreSQL
       ├─ ContractSettlement 조회       → actualIncome = 3,868,000
       └─ BankServerClient.getAccountBalance(1004)   ← INCOME 계좌 account_id
            └─ GET mydata-server:8084/mydata/v1/bank/accounts/1004/balance
                 └─ GET transaction-server:8083/baas/v1/bank/accounts/1004/balance
                      └─ oracle-bank: SELECT balance FROM bank_account WHERE account_id=1004
                           → 5,000,000원
```

**사전 조건**
- matching_id=1, matching_status=TBC 행 존재 (STEP 8 사전 삽입)
- oracle-bank `bank_account.account_id=1004`, `balance=5000000`
- `VIRTUAL_SALARY_SETTING` 존재 (자동분배 계산에 사용)

**중간 서버 응답 확인** (디버깅용)
```bash
# INCOME 계좌 잔액 확인
curl http://localhost:8084/mydata/v1/bank/accounts/1004/balance
# → {"success":true,"data":{"accountId":1004,"balance":5000000,...}}
```

```bash
curl -X PATCH http://localhost:8080/api/v1/payment-matchings/1/manual \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"bankTransactionId": 9001, "matchedBy": "USER"}'
```

**응답** (`200 OK`)
```json
{
  "success": true,
  "data": {
    "matchingId": 1,
    "matchingStatus": "MANUAL_MATCHED",
    "matchedBy": "USER",
    "matchedAt": "2026-05-29T06:33:10.243625182"
  },
  "meta": { "traceId": "uuid" }
}
```

**자동분배 계산 결과** (응답에는 포함되지 않음, 로그로 확인)

> 가상월급 설정 기준 (STEP 3 저장값 — test-data.md STEP 5 수정 전 기준):
> `targetSalary=3,000,000`, `emergencyRatio=30%`, `investmentRatio=20%`,
> `priorityOrder=["SALARY","EMERGENCY","INVESTMENT"]`
> `incomeBalance=5,000,000 (oracle-bank account_id=1004)`
> `actualIncome=3,868,000 (삼성SDS 계약 실수령액)`

| 항목 | 계산식 | 결과 |
|---|---|---|
| salaryReserved | min(targetSalary=3,000,000, actualIncome=3,868,000) | 3,000,000 |
| distributable | 3,868,000 - 3,000,000 | 868,000 |
| emergencyAmount | min(3,868,000×30%=1,160,400, 868,000, emergencyCap=5,000,000) | 868,000 |
| investmentAmount | min(3,868,000×20%=773,600, 0) | 0 |
| livingAmount | 0 | 0 |
| effectiveBalance | 5,000,000 - 3,000,000 = 2,000,000 | — |
| totalToTransfer | 868,000 + 0 = 868,000 ≤ 2,000,000 | 잔액 충분 |

> 실제 이체는 현재 미구현입니다. 계산 결과만 내부적으로 산출하며 이체 요청은 하지 않습니다.

**에러 케이스**

| 상황 | 요청 | 에러 코드 |
|---|---|---|
| 존재하지 않는 matchingId | `/9999/manual` | `MATCHING_001` |
| 이미 MANUAL_MATCHED 상태 재시도 | 동일 요청 반복 | `MATCHING_002` |
| matchedBy = "SYSTEM" | `"matchedBy":"SYSTEM"` | `MATCHING_003` |

```json
{ "success": false, "error": { "code": "MATCHING_001", "message": "매칭 정보를 찾을 수 없습니다." } }
{ "success": false, "error": { "code": "MATCHING_002", "message": "이미 매칭 처리된 건입니다." } }
{ "success": false, "error": { "code": "MATCHING_003", "message": "matchedBy는 USER만 허용됩니다." } }
```

---

## 11. AI 분배 비율 추천 (GET /virtual-salary/recommendation)

**통신 범위**: ⚡ 외부 서버 호출 포함 (현재 환경 미지원)

```
service-backend
  ├─ VirtualSalarySettingRepository 조회   → PostgreSQL
  ├─ BankServerClient.getAccountBalance(1005)  [SALARY 잔액, 실패 시 0으로 fallback]
  │    └─ mydata-server → transaction-server → oracle-bank
  ├─ ContractRepository (이번달 actualIncome 합산)  → PostgreSQL
  └─ AiServerClient.getVirtualSalaryRecommendation()
       └─ POST service-ai-server:8000/api/v1/ai/virtual-salary/recommend
            ⚠️  해당 엔드포인트 미구현 → AI_002 (timeout) 반환
```

**AI 서버에 전달하는 요청 바디** (참고용)
```json
{
  "targetSalary": 3000000,
  "currentBalance": 1200000,
  "monthlyExpectedIncome": 3868000,
  "emergencyTargetAmount": 5000000,
  "emergencyRatio": 30,
  "investmentRatio": 20
}
```

**사전 조건**
- `VIRTUAL_SALARY_SETTING` 존재 (없으면 `VIRTUAL_SALARY_001`)
- SALARY 계좌 잔액 조회 실패 시 `currentBalance=0` 으로 AI에 전달 (non-fatal)

```bash
curl http://localhost:8080/api/v1/virtual-salary/recommendation \
  -H "Authorization: Bearer $TOKEN"
```

**AI 서버 정상 응답 시** (`200 OK`)
```json
{
  "success": true,
  "data": {
    "recommendedEmergencyRatio": 35.00,
    "recommendedInvestmentRatio": 20.00,
    "summary": "최근 수입 안정성이 높아 투자 비중 확대를 추천합니다."
  },
  "meta": { "traceId": "uuid" }
}
```

**현재 환경 응답** (AI 서버 엔드포인트 미구현)
```json
{ "success": false, "error": { "code": "AI_002", "message": "AI 서버 응답 시간이 초과되었습니다." } }
```

**에러 케이스**

| 상황 | 에러 코드 | 원인 |
|---|---|---|
| VIRTUAL_SALARY_SETTING 없음 | `VIRTUAL_SALARY_001` | PostgreSQL 조회 실패 |
| AI 서버 timeout | `AI_002` | `ResourceAccessException` |
| AI 서버 오류 응답 | `AI_001` | 비정상 응답 / 파싱 실패 |
| AI 실행 중 예외 | `AI_003` | 기타 예외 |

> 추천 결과는 자동 저장되지 않습니다. 적용하려면 `POST /virtual-salary`를 별도 호출하세요.

---

## 12. 스케줄러 동작 확인 (매일 오전 9시)

**통신 범위**: ⚡ 외부 서버 호출 포함

```
VirtualSalaryScheduler (@Scheduled "0 0 9 * * *")
  └─ VirtualSalaryPaymentService.processPayday(today, isLastDayOfMonth)
       └─ VIRTUAL_SALARY_SETTING WHERE payday = today   → PostgreSQL
            (isLastDayOfMonth=true 이면 payday > today 도 추가 조회)
       └─ 사용자별 processSingleUser(setting):
            ├─ ACCOUNT_MAPPING WHERE mapping_type IN (INCOME, SALARY)  → PostgreSQL
            └─ BankServerClient.getAccountBalance(INCOME account_id=1004)
                 └─ mydata-server → transaction-server → oracle-bank
                      → 5,000,000원
                 → paidAmount = min(targetSalary, incomeBalance)
                              = min(3,000,000, 5,000,000) = 3,000,000
```

**정상 동작 로그** (payday=15인 사용자가 15일에 실행되는 경우)
```
INFO  VirtualSalaryScheduler        : 가상월급 스케줄러 실행: date=2026-05-15, isLastDay=false
INFO  VirtualSalaryPaymentServiceImpl: 가상월급 지급 대상 확인: userId=5, targetSalary=3500000.00, incomeBalance=5000000, paidAmount=3500000.00
```

**월말 처리 로그** (2월 28일, payday=29/30/31인 사용자 포함)
```
INFO  VirtualSalaryScheduler        : 가상월급 스케줄러 실행: date=2026-02-28, isLastDay=true
INFO  VirtualSalaryPaymentServiceImpl: 가상월급 지급 대상 확인: userId=5, ...
```

**스킵 케이스 로그**

| 상황 | 로그 |
|---|---|
| INCOME 또는 SALARY 계좌 미연결 | `WARN ... 가상월급 지급 스킵 - 계좌 미연결: userId=5` |
| INCOME 잔액 조회 실패 (mydata-server 장애 등) | `WARN ... 가상월급 지급 스킵 - INCOME 잔액 조회 실패: userId=5` |
| INCOME 잔액 = 0 | `WARN ... 가상월급 지급 스킵 - INCOME 잔액 없음: userId=5` |

> 실제 INCOME → SALARY 이체는 미구현 (TransactionServer 연동 예정). 현재는 로그만 출력합니다.

---

## 추천 테스트 순서

```
[사전 준비]
  1. test-data.md STEP 1~2  oracle-bank 계좌 생성 및 잔액 설정
  2. test-data.md STEP 3~4  postgres-operational 계정 + 계좌 연동

[외부 통신 없는 API 검증]
  3.  POST /virtual-salary             → 설정 저장 (201)
  4.  GET  /virtual-salary             → 설정 조회 확인
  5.  PATCH /virtual-salary            → 설정 수정 (200)
  6.  POST /virtual-salary (비율 초과) → VIRTUAL_SALARY_002 에러
  7.  POST /contracts (BUSINESS, 6월)  → contract_id=1, 계산값 확인
  8.  POST /contracts (ETC, 6월)       → contract_id=2
  9.  POST /contracts (BUSINESS, 5월) → contract_id=3 (summary 테스트용)
  10. GET  /contracts?date=2026-06-01 → 6월 계약 2건
  11. GET  /contracts/1               → 상세 (taxRate=0.033 확인)

[외부 서버 통신 검증]
  12. curl mydata-server:8084/bank/accounts/1005/balance
      → {"balance":1200000} 직접 확인 (체인 사전 검증)
  13. GET  /virtual-salary/dashboard
      → currentBalance=1200000 (oracle-bank → transaction-server → mydata-server → 여기)
  14. GET  /virtual-salary/summary
      → dashboard 포함 + 5월 계약(삼성SDS) + calendarData

[매칭 플로우 검증]
  15. test-data.md STEP 8  payment_matching TBC 삽입
  16. GET  /payment-matchings                  → TBC 매칭 1건
  17. GET  /payment-matchings?matchingStatus=TBC  → 필터 동작 확인
  18. PATCH /payment-matchings/1/manual        → MANUAL_MATCHED, 자동분배 실행
      (서비스 로그에서 AutoDistributionService 계산 결과 확인)
  19. PATCH /payment-matchings/1/manual 재시도 → MATCHING_002 에러
  20. PATCH /payment-matchings/1/manual (matchedBy=SYSTEM) → MATCHING_003 에러

[AI 추천 — 현재 환경에서 실패 확인]
  21. GET  /virtual-salary/recommendation → AI_002 반환 (서버 미구현)
```

---

## 에러 코드 전체 목록

| 코드 | HTTP | 메시지 | 발생 조건 |
|---|---|---|---|
| `CONTRACT_001` | 404 | 존재하지 않는 계약입니다 | contractId 조회 실패 |
| `CONTRACT_002` | 403 | 본인 계약이 아닙니다 | 타인 계약 접근 |
| `VIRTUAL_SALARY_001` | 404 | 가상월급 설정이 없습니다 | VIRTUAL_SALARY_SETTING 미존재 |
| `VIRTUAL_SALARY_002` | 400 | 투자 비율과 비상금 비율의 합은 100을 초과할 수 없습니다 | investmentRatio + emergencyRatio > 100 |
| `VIRTUAL_SALARY_003` | 404 | SALARY 계좌가 연결되어 있지 않습니다 | ACCOUNT_MAPPING SALARY 타입 없음 |
| `MATCHING_001` | 404 | 매칭 정보를 찾을 수 없습니다 | matchingId 조회 실패 |
| `MATCHING_002` | 400 | 이미 매칭 처리된 건입니다 | MATCHED / MANUAL_MATCHED 재처리 |
| `MATCHING_003` | 400 | matchedBy는 USER만 허용됩니다 | matchedBy ≠ USER |
| `AI_001` | 500 | AI 응답 생성에 실패했습니다 | AI 서버 비정상 응답 |
| `AI_002` | 504 | AI 서버 응답 시간이 초과되었습니다 | ResourceAccessException |
| `AI_003` | 500 | AI 실행에 실패했습니다 | AI 실행 중 예외 |
| `VALID_001` | 400 | 입력값이 올바르지 않습니다 | Bean Validation 실패 |

---

## PowerShell 전체 예시

```powershell
# 0. 로그인
$resp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"email":"vstest@test.com","password":"Test1234!"}'
$TOKEN = $resp.data.accessToken

# 1. 가상월급 설정 저장
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/virtual-salary" -Method POST `
  -Headers @{"Authorization"="Bearer $TOKEN"; "Content-Type"="application/json"} `
  -Body '{"targetSalary":3000000,"payday":25,"emergencyTargetAmount":5000000,"investmentRatio":20,"emergencyRatio":30,"priorityOrder":["SALARY","EMERGENCY","INVESTMENT"]}' |
  ConvertTo-Json -Depth 5

# 2. 계약 생성
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/contracts" -Method POST `
  -Headers @{"Authorization"="Bearer $TOKEN"; "Content-Type"="application/json"} `
  -Body '{"clientName":"삼성SDS","contractAmount":4000000,"expectedPaymentDate":"2026-05-30","taxType":"BUSINESS"}' |
  ConvertTo-Json -Depth 5

# 3. 중간 서버 체인 직접 검증 (대시보드 호출 전)
Invoke-RestMethod -Uri "http://localhost:8084/mydata/v1/bank/accounts/1005/balance" |
  ConvertTo-Json

# 4. 대시보드 (외부 서버 통신 포함)
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/virtual-salary/dashboard" `
  -Headers @{"Authorization"="Bearer $TOKEN"} | ConvertTo-Json -Depth 5

# 5. 수동 매칭 (외부 서버 통신 포함)
$idempotencyKey = [guid]::NewGuid().ToString()
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payment-matchings/1/manual" -Method PATCH `
  -Headers @{"Authorization"="Bearer $TOKEN"; "Content-Type"="application/json"; "Idempotency-Key"=$idempotencyKey} `
  -Body '{"bankTransactionId":9001,"matchedBy":"USER"}' | ConvertTo-Json -Depth 5
```
