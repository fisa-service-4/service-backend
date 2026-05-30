# 테스트 데이터 삽입 가이드

> 가상월급 도메인 전체 API의 end-to-end 테스트에 필요한 데이터를 DB별, 순서별로 정리합니다.
> 테스트 기준일: 2026-05-29

---

## 전체 서버 통신 체인

잔액 조회가 포함된 API(대시보드, 자동분배, 스케줄러)의 실제 통신 경로입니다.

```
service-backend
  └─ BankServerClient.getAccountBalance(accountId)
       └─ GET mydata-server:8084/mydata/v1/bank/accounts/{id}/balance
            └─ GET transaction-server:8083/baas/v1/bank/accounts/{id}/balance
                 └─ oracle-bank (BANK_ACCOUNT.balance 조회)
```

> BANK_ACCOUNT.balance 컬럼이 실시간 잔액입니다.
> BANK_TRANSACTION의 마지막 balance_after와 별개로 직접 관리됩니다.

---

## DB 삽입 순서 요약

| 순서 | DB | 방법 | 내용 |
|---|---|---|---|
| 1 | oracle-bank | 직접 SQL | 테스트용 계좌 2개 생성 (INCOME / SALARY) |
| 2 | oracle-bank | 직접 SQL | 초기 거래 내역 삽입 (잔액 설정) |
| 3 | postgres-operational | 직접 SQL | 테스트 사용자 생성 |
| 4 | postgres-operational | 직접 SQL | 계좌 연동 및 목적 매핑 |
| 5 | postgres-operational | API | 가상월급 설정 저장 |
| 6 | postgres-operational | API | 계약 3건 생성 |
| 7 | postgres-operational | API | 가상월급 설정 수정 |
| 8 | postgres-operational | 직접 SQL | 매칭 시드 삽입 |
| 9 | postgres-operational | API | 수동 매칭 처리 |

---

## STEP 1 — oracle-bank: 테스트 계좌 생성

**연결**: `oracle-bank:1524` / Service: `XEPDB1` / User: `BANK` / Password: `bank123`

> 기존 seed 데이터 현황 (이미 존재):
> - account_id=1001, user_id=501, 급여통장, 잔액 5,000,000원
> - account_id=1002, user_id=501, 생활비통장, 잔액 1,200,000원
> - account_id=1003, user_id=502, 비상금통장, 잔액 850,000원
>
> **아래는 테스트 유저(user_id=5) 전용 계좌를 새로 만드는 데이터입니다.**

```sql
-- INCOME 계좌 (수입 입금 통장 — 가상월급 지급 재원)
INSERT INTO bank_account
  (account_id, user_id, bank_code, account_number, account_name,
   balance, account_status, opened_at, updated_at)
VALUES
  (1004, 5, '088', '110-111-000004', 'INCOME통장',
   5000000, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP);

-- SALARY 계좌 (생활비 지출 통장 — 대시보드 currentBalance 기준)
INSERT INTO bank_account
  (account_id, user_id, bank_code, account_number, account_name,
   balance, account_status, opened_at, updated_at)
VALUES
  (1005, 5, '088', '110-111-000005', 'SALARY통장',
   1200000, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;
```

**결과**

| account_id | user_id | account_number | account_name | balance | account_status |
|---|---|---|---|---|---|
| 1004 | 5 | 110-111-000004 | INCOME통장 | 5,000,000 | ACTIVE |
| 1005 | 5 | 110-111-000005 | SALARY통장 | 1,200,000 | ACTIVE |

---

## STEP 2 — oracle-bank: 초기 거래 내역 삽입 (잔액 근거 기록)

> BANK_ACCOUNT.balance가 실시간 잔액이지만,
> BANK_TRANSACTION에도 초기 입금 기록을 남겨야 거래 내역 조회 API가 정상 동작합니다.

```sql
-- INCOME 계좌 초기 입금 거래
INSERT INTO bank_transaction
  (transaction_id, account_id, transaction_type, transaction_category,
   amount, balance_after, transaction_channel, transaction_status, transaction_at)
VALUES
  (9004, 1004, 'DEPOSIT', '프리랜서수입',
   5000000, 5000000, 'APP', 'SUCCESS', SYSTIMESTAMP);

-- SALARY 계좌 초기 입금 거래 (가상월급 이체 시뮬레이션)
INSERT INTO bank_transaction
  (transaction_id, account_id, transaction_type, transaction_category,
   amount, balance_after, transaction_channel, transaction_status, transaction_at)
VALUES
  (9005, 1005, 'DEPOSIT', '가상월급',
   1200000, 1200000, 'APP', 'SUCCESS', SYSTIMESTAMP);

COMMIT;
```

**결과**

| transaction_id | account_id | transaction_type | amount | balance_after |
|---|---|---|---|---|
| 9004 | 1004 | DEPOSIT | 5,000,000 | 5,000,000 |
| 9005 | 1005 | DEPOSIT | 1,200,000 | 1,200,000 |

---

## STEP 3 — postgres-operational: 테스트 사용자 생성

**연결**: `postgres-operational:5432` / DB: `finance_operational` / User: `admin`

> **주의**: BCrypt 해시에 포함된 `$` 기호는 bash/PowerShell의 더블쿼트(`"`) 환경에서
> 쉘 변수로 치환되어 해시가 손상됩니다.
> 반드시 **psql 대화형 프롬프트에 직접 붙여넣기**하거나 `.sql` 파일로 실행하세요.
>
> ```bash
> # 올바른 실행 방법 — psql 대화형 프롬프트 진입 후 SQL 붙여넣기
> docker exec -it postgres-operational psql -U admin -d finance_operational
> ```
>
> 절대 `psql -c "... $2b$10$... "` 형태로 실행하지 마세요.

```sql
-- 사용자 삽입 (password: Test1234! 의 BCrypt hash)
INSERT INTO users
  (firebase_uid, email, password_hash, user_name, phone_number,
   role, status, notification_consent_yn, terms_consent_yn, mydata_consent_yn, created_at)
VALUES (
  'firebase-uid-vstest-001',
  'vstest@test.com',
  '$2b$10$GtYeMhO44tVhRHMulLRw2OcWUR1YFpRm3vVT1amLQmBCxzLcHDA8.',
  '가상월급테스터',
  '01011112222',
  'USER', 'ACTIVE', true, true, true, NOW()
);
-- → user_id = 5

-- 사용자 프로필
INSERT INTO user_profile (user_id, freelancer_yn, job_type)
VALUES (5, true, 'DEVELOPER');
```

---

## STEP 4 — postgres-operational: 계좌 연동 및 목적 매핑

> oracle-bank에서 생성한 account_id(1004, 1005)를 `external_account_id`로 연결합니다.

```sql
-- INCOME 계좌 연동 (oracle-bank account_id=1004)
INSERT INTO linked_financial_account
  (user_id, institution_type, institution_code, external_account_id, account_masking, synced_at)
VALUES (5, 'BANK', '088', 1004, '110-111-****04', NOW());
-- → linked_account_id = 3

-- SALARY 계좌 연동 (oracle-bank account_id=1005)
INSERT INTO linked_financial_account
  (user_id, institution_type, institution_code, external_account_id, account_masking, synced_at)
VALUES (5, 'BANK', '088', 1005, '110-111-****05', NOW());
-- → linked_account_id = 4

-- 계좌 목적 매핑
INSERT INTO account_mapping (user_id, linked_account_id, mapping_type)
VALUES (5, 3, 'INCOME');   -- → mapping_id = 3

INSERT INTO account_mapping (user_id, linked_account_id, mapping_type)
VALUES (5, 4, 'SALARY');   -- → mapping_id = 4
```

**이 시점에서 잔액 조회 체인 동작 확인 가능**

```bash
# service-backend → mydata-server → transaction-server → oracle-bank
# SALARY 계좌(1005) 잔액 1,200,000 반환 여부 확인
curl http://localhost:8084/mydata/v1/bank/accounts/1005/balance
```

```json
{
  "success": true,
  "data": { "accountId": 1005, "balance": 1200000, "availableBalance": null },
  "meta": null
}
```

---

## STEP 5 — API: 가상월급 설정 저장

> `POST /api/v1/virtual-salary` — 로그인 토큰 필요

```bash
# 로그인
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"vstest@test.com","password":"Test1234!"}' \
  | jq -r '.data.accessToken')

# 가상월급 설정 저장
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

**생성된 레코드 — VIRTUAL_SALARY_SETTING**

| 컬럼 | 값 |
|---|---|
| user_id | 5 |
| target_salary | 3000000.00 |
| payday | 25 |
| emergency_target_amount | 5000000.00 |
| investment_ratio | 20.00 |
| emergency_ratio | 30.00 |
| priority_order | ["SALARY","EMERGENCY","INVESTMENT"] |

---

## STEP 6 — API: 계약 3건 생성

> `POST /api/v1/contracts` — 로그인 토큰 필요

### 계약 1 — (주)카카오 / BUSINESS / 6월 (목록·상세 조회 테스트용)

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

| 테이블 | contract_id | contract_amount | deducted_amount | actual_income | contract_status |
|---|---|---|---|---|---|
| CONTRACT | 1 | 5,000,000 | 165,000 (3.3%) | 4,835,000 | PENDING |
| CONTRACT_SETTLEMENT | settlement_id=1 | tax_rate=0.033 | 165,000 | 4,835,000 | — |

### 계약 2 — 네이버클라우드 / ETC / 6월

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

| 테이블 | contract_id | contract_amount | deducted_amount | actual_income | contract_status |
|---|---|---|---|---|---|
| CONTRACT | 2 | 3,000,000 | 99,000 (3.3%) | 2,901,000 | PENDING |
| CONTRACT_SETTLEMENT | settlement_id=2 | tax_rate=0.033 | 99,000 | 2,901,000 | — |

### 계약 3 — 삼성SDS / BUSINESS / 5월 (summary BFF 및 매칭 테스트용)

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

| 테이블 | contract_id | contract_amount | deducted_amount | actual_income | contract_status |
|---|---|---|---|---|---|
| CONTRACT | 3 | 4,000,000 | 132,000 (3.3%) | 3,868,000 | PENDING → **PAID** (STEP 9에서 변경) |
| CONTRACT_SETTLEMENT | settlement_id=3 | tax_rate=0.033 | 132,000 | 3,868,000 | — |

---

## STEP 7 — API: 가상월급 설정 수정

> `PATCH /api/v1/virtual-salary`

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

**변경 결과 — VIRTUAL_SALARY_SETTING (user_id=5)**

| 컬럼 | STEP 5 값 | STEP 7 값 (최종) |
|---|---|---|
| target_salary | 3,000,000 | **3,500,000** |
| payday | 25 | **15** |
| emergency_target_amount | 5,000,000 | **6,000,000** |
| investment_ratio | 20.00 | **25.00** |
| emergency_ratio | 30.00 | **35.00** |
| priority_order | ["SALARY","EMERGENCY","INVESTMENT"] | **["EMERGENCY","SALARY","INVESTMENT"]** |

---

## STEP 8 — postgres-operational: 매칭 시드 삽입 (직접 SQL)

```sql
-- contract_id=3 (5월 삼성SDS 계약)에 대한 TBC 매칭 생성
INSERT INTO payment_matching
  (contract_id, bank_transaction_id, matching_status, matched_by, matched_at)
VALUES (3, 9001, 'TBC', 'SYSTEM', NULL);
-- → matching_id = 1
```

> `bank_transaction_id=9001`은 oracle-bank 기존 seed 거래 ID입니다.
> 수동 매칭 테스트에서 이 값으로 매칭합니다.

| matching_id | contract_id | bank_transaction_id | matching_status | matched_by | matched_at |
|---|---|---|---|---|---|
| 1 | 3 | 9001 | TBC | SYSTEM | NULL |

---

## STEP 9 — API: 수동 매칭 처리

> `PATCH /api/v1/payment-matchings/1/manual`
> 매칭 완료 시 `AutoDistributionService.distribute()` 자동 실행 → oracle-bank INCOME 계좌(1004) 잔액 조회

```bash
curl -X PATCH http://localhost:8080/api/v1/payment-matchings/1/manual \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"bankTransactionId": 9001, "matchedBy": "USER"}'
```

**변경된 레코드**

| 테이블 | 컬럼 | 변경 전 | 변경 후 |
|---|---|---|---|
| PAYMENT_MATCHING | matching_status | TBC | MANUAL_MATCHED |
| PAYMENT_MATCHING | matched_by | SYSTEM | USER |
| PAYMENT_MATCHING | matched_at | NULL | (현재시각) |
| CONTRACT | contract_status | PENDING | PAID |

---

## 최종 데이터 상태

### oracle-bank (BANK_ACCOUNT)

| account_id | user_id | account_number | account_name | balance | 용도 |
|---|---|---|---|---|---|
| 1001 | 501 | 110-123-456789 | 급여통장 | 5,000,000 | 기존 seed |
| 1002 | 501 | 110-987-654321 | 생활비통장 | 1,200,000 | 기존 seed |
| 1003 | 502 | 301-1234-5678 | 비상금통장 | 850,000 | 기존 seed |
| **1004** | **5** | **110-111-000004** | **INCOME통장** | **5,000,000** | 테스트 신규 |
| **1005** | **5** | **110-111-000005** | **SALARY통장** | **1,200,000** | 테스트 신규 |

### oracle-bank (BANK_TRANSACTION)

| transaction_id | account_id | transaction_type | amount | balance_after |
|---|---|---|---|---|
| 9001 | 1001 | DEPOSIT | 3,000,000 | 5,000,000 |
| 9002 | 1001 | WITHDRAW | 50,000 | 4,950,000 |
| 9003 | 1002 | TRANSFER_OUT | 200,000 | 1,000,000 |
| **9004** | **1004** | **DEPOSIT** | **5,000,000** | **5,000,000** |
| **9005** | **1005** | **DEPOSIT** | **1,200,000** | **1,200,000** |

### postgres-operational (요약)

| 테이블 | 신규 데이터 |
|---|---|
| USERS | user_id=5 (vstest@test.com) |
| USER_PROFILE | user_id=5 |
| LINKED_FINANCIAL_ACCOUNT | linked_id=3 (→ account_id=1004), linked_id=4 (→ account_id=1005) |
| ACCOUNT_MAPPING | INCOME(linked_id=3), SALARY(linked_id=4) |
| VIRTUAL_SALARY_SETTING | user_id=5, targetSalary=3,500,000, payday=15 (PATCH 최종값) |
| CONTRACT | contract_id=1(카카오/PENDING), 2(네이버/PENDING), 3(삼성SDS/**PAID**) |
| CONTRACT_SETTLEMENT | settlement_id=1,2,3 |
| PAYMENT_MATCHING | matching_id=1, **MANUAL_MATCHED** |

---

## 데이터 초기화 SQL

```sql
-- === oracle-bank (BANK_ACCOUNT / BANK_TRANSACTION) ===
DELETE FROM bank_transaction WHERE transaction_id IN (9004, 9005);
DELETE FROM bank_account WHERE account_id IN (1004, 1005);
COMMIT;

-- === postgres-operational ===
DELETE FROM payment_matching
  WHERE contract_id IN (SELECT contract_id FROM contract WHERE user_id = 5);
DELETE FROM contract_settlement
  WHERE contract_id IN (SELECT contract_id FROM contract WHERE user_id = 5);
DELETE FROM contract WHERE user_id = 5;
DELETE FROM virtual_salary_setting WHERE user_id = 5;
DELETE FROM account_mapping WHERE user_id = 5;
DELETE FROM linked_financial_account WHERE user_id = 5;
DELETE FROM user_profile WHERE user_id = 5;
DELETE FROM users WHERE user_id = 5;
```
