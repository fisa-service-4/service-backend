# 테스트용 시드 데이터 삽입 가이드

## 개요

`GET /api/v1/accounts` 및 `PATCH /api/v1/accounts/{accountId}/role` API를 테스트하려면 아래 두 DB에 데이터를 삽입해야 합니다.

| DB | 역할 | 접속 정보 (로컬) |
|---|---|---|
| PostgreSQL `finance_operational` | service-backend 운영 DB | `localhost:5432` |
| Oracle `BANK` schema | bank-server 원장 DB | `localhost:1524` |

> **mydata-server**는 별도 DB가 없고 bank-server에 프록시합니다.  
> **stock-server**는 계좌 역할 설정(STOCK)을 쓸 경우에만 필요합니다.

---

## 1. 사전 조건

```bash
# infra 디렉터리에서 컨테이너 실행
cd infra
docker compose up -d postgres-operational oracle-bank
```

---

## 2. PostgreSQL — service-backend 운영 DB

접속: `postgresql://admin:1234@localhost:5432/finance_operational`

### 2-1. 사용자 (users)

```sql
INSERT INTO users (
    firebase_uid, email, password_hash, user_name, phone_number,
    role, status, notification_consent_yn, terms_consent_yn, mydata_consent_yn, created_at
) VALUES (
    'test-firebase-uid-001',
    'test@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Password123!
    '홍길동',
    '01012345678',
    'USER',
    'ACTIVE',
    true, true, true,
    NOW()
);
-- 생성된 user_id 확인: SELECT user_id FROM users WHERE email = 'test@test.com';
```

### 2-2. 사용자 프로필 (user_profile)

```sql
INSERT INTO user_profile (user_id, freelancer_yn, job_type)
VALUES (1, true, 'DEVELOPER');
```

### 2-3. PIN 등록 (pin_auth)

```sql
-- PIN: 123456
INSERT INTO pin_auth (user_id, pin_hash, fail_count, locked_yn)
VALUES (
    1,
    '$2a$10$8K1p/a0dR1xqM8K3Qe5Vt.VkRmNuFEZt7GZ6g3pBJXi2p2qKvVm6', -- 123456
    0,
    false
);
```

> **PIN 없이 테스트할 경우** pin_auth는 생략 가능합니다.  
> 계좌 역할 설정 API는 PIN Token이 필요 없습니다.

### 2-4. 연동 금융 계좌 (linked_financial_account)

`external_account_id`는 Oracle bank-server의 `account.id`와 일치해야 합니다.

```sql
-- 은행 계좌 2개 연동
INSERT INTO linked_financial_account (
    user_id, institution_type, institution_code, external_account_id, account_masking, synced_at
) VALUES
    (1, 'BANK', '088', 1001, '110-****-456789', NOW()),  -- 신한은행 계좌
    (1, 'BANK', '020', 1002, '301-****-001234', NOW());  -- 우리은행 계좌
```

---

## 3. Oracle — bank-server (BANK schema)

접속: `jdbc:oracle:thin:@localhost:1524/XEPDB1` / 계정: `BANK` / 비밀번호: `bank123`

> DBeaver 등 DB 툴 사용 시: Host `localhost`, Port `1524`, Service Name `XEPDB1`

### 3-1. 계좌 (account)

`id` 값은 위의 `external_account_id`와 동일해야 합니다.

```sql
INSERT INTO account (id, user_id, account_number, account_type, bank_code, currency, status, opened_at, trace_id, created_at, created_by)
VALUES (1001, 1, '110-123-456789', 'CHECKING', '088', 'KRW', 'ACTIVE', TO_DATE('2024-01-15', 'YYYY-MM-DD'), 'trace-seed-001', SYSTIMESTAMP, 'system');

INSERT INTO account (id, user_id, account_number, account_type, bank_code, currency, status, opened_at, trace_id, created_at, created_by)
VALUES (1002, 1, '301-098-001234', 'CHECKING', '020', 'KRW', 'ACTIVE', TO_DATE('2024-03-01', 'YYYY-MM-DD'), 'trace-seed-002', SYSTIMESTAMP, 'system');

COMMIT;
```

### 3-2. 잔액 (account_transaction - 초기 잔액 세팅)

`balance_after`가 현재 잔액으로 조회됩니다.

```sql
INSERT INTO account_transaction (
    account_id, transaction_type, direction, amount, balance_after,
    currency, description, transaction_at, trace_id, created_at, created_by
) VALUES
    (1001, 'DEPOSIT', 'CREDIT', 3500000, 3500000, 'KRW', '초기 잔액', SYSTIMESTAMP, 'trace-seed-003', SYSTIMESTAMP, 'system'),
    (1002, 'DEPOSIT', 'CREDIT', 800000,  800000,  'KRW', '초기 잔액', SYSTIMESTAMP, 'trace-seed-004', SYSTIMESTAMP, 'system');

COMMIT;
```

---

## 4. 테스트 흐름

### Step 1. 로그인 → JWT 토큰 발급

```bash
POST /api/v1/auth/login
{
  "email": "test@test.com",
  "password": "Password123!"
}
# → accessToken 복사
```

### Step 2. 내 계좌 조회

```bash
GET /api/v1/accounts
Authorization: Bearer {accessToken}

# 기대 응답
[
  { "accountId": 1001, "bankCode": "088", "balance": 3500000, "accountRole": null },
  { "accountId": 1002, "bankCode": "020", "balance": 800000,  "accountRole": null }
]
```

### Step 3. 계좌 역할 설정

```bash
PATCH /api/v1/accounts/1001/role
Authorization: Bearer {accessToken}
{
  "accountRole": "SALARY"
}

# 재조회 시 accountRole: "SALARY" 로 변경 확인
```

---

## 5. account_mapping 확인 쿼리 (PostgreSQL)

```sql
SELECT
    am.mapping_id,
    am.user_id,
    am.mapping_type,
    lfa.external_account_id,
    lfa.account_masking
FROM account_mapping am
JOIN linked_financial_account lfa ON am.linked_account_id = lfa.linked_account_id
WHERE am.user_id = 1;
```

---

## 참고: 데이터 연결 구조

```
Oracle bank-server
  account.id (1001, 1002)
        ↕ external_account_id
PostgreSQL service-backend
  linked_financial_account
        ↕ linked_account_id FK
  account_mapping (mapping_type: SALARY 등)
```
