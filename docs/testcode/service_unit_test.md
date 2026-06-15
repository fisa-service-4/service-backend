# Service 단위 테스트 시나리오 명세

> 테스트 방식: `@ExtendWith(MockitoExtension.class)` — Spring 컨텍스트 없이 Mockito만 사용

---

## 목차

- [A. AuthService — 회원가입 `signup`](#a-authservice--회원가입-signup)
- [B. AuthService — 로그인 `login`](#b-authservice--로그인-login)
- [C. AuthService — 로그아웃 `logout`](#c-authservice--로그아웃-logout)
- [D. AuthService — 토큰 재발급 `reissue`](#d-authservice--토큰-재발급-reissue)
- [E. AuthService — `completeSignup` (Firebase 폴백)](#e-authservice--completesignup-firebase-폴백)
- [F. PinService — PIN 등록 `registerPin`](#f-pinservice--pin-등록-registerpin)
- [G. PinService — PIN 검증 `verifyPin`](#g-pinservice--pin-검증-verifypin)
- [H. PinService — PIN 변경 `changePin`](#h-pinservice--pin-변경-changepin)
- [I. UserService — 내 정보 조회 `getMe`](#i-userservice--내-정보-조회-getme)
- [J. UserService — 알림 동의 수정 `updateConsent`](#j-userservice--알림-동의-수정-updateconsent)
- [구현 시 주의사항](#구현-시-주의사항)

---

## A. AuthService — 회원가입 `signup`

### A-01. 정상 회원가입

| 항목 | 내용 |
|------|------|
| **Given** | email, phoneNumber 중복 없음 / 유효한 `SignupRequest` |
| **When** | `signup(request)` 호출 |
| **Then** | `User` 저장 (status=`INACTIVE`, role=`USER`), `UserProfile` 저장, 비밀번호 인코딩, `completeSignup()` 호출, `SignupResponse` 반환 |

#### 포인트

- `@Spy`로 `AuthService`를 감싸 `completeSignup()` 호출 여부 검증 및 차단
- `completeSignup()`은 `doNothing()`으로 실행 차단 (Firebase 호출은 E 시나리오에서 별도 검증)
- `ArgumentCaptor`로 `save()`에 전달된 `User`, `UserProfile` 필드값 직접 검증

```
User:        status=INACTIVE, role=USER, passwordHash="encoded-password"
UserProfile: freelancerYn=true, jobType="DEVELOPER"
Response:    userId, email, userName 일치 여부 확인
```

---

### A-02. 이메일 중복

| 항목 | 내용 |
|------|------|
| **Given** | `userRepository.existsByEmail(email)` = `true` |
| **When** | `signup(request)` 호출 |
| **Then** | `BusinessException(AUTH_001)` 발생, `save()` · `completeSignup()` 미호출 |

#### 포인트

```java
assertThatThrownBy(() -> authService.signup(request))
    .isInstanceOf(BusinessException.class)
    .extracting(ex -> ((BusinessException) ex).getErrorCode())
    .isEqualTo(ErrorCode.AUTH_001);

then(userRepository).should(never()).save(any(User.class));
then(authService).should(never()).completeSignup(anyLong());
```

> 전화번호 중복 체크는 이메일에서 이미 예외가 발생하므로 stub 불필요

---

### A-03. 전화번호 중복

| 항목 | 내용 |
|------|------|
| **Given** | `existsByEmail` = `false` / `existsByPhoneNumber(phoneNumber)` = `true` |
| **When** | `signup(request)` 호출 |
| **Then** | `BusinessException(AUTH_002)` 발생, `save()` 미호출 |

#### 포인트

이메일 체크 통과 → 전화번호 체크에서 예외 발생하는 흐름 재현. 두 검사가 **순서대로** 실행되는 것도 간접 검증.

```java
given(userRepository.existsByEmail(email)).willReturn(false);
given(userRepository.existsByPhoneNumber(phoneNumber)).willReturn(true);

assertThatThrownBy(() -> authService.signup(request))
    .isInstanceOf(BusinessException.class)
    .extracting(ex -> ((BusinessException) ex).getErrorCode())
    .isEqualTo(ErrorCode.AUTH_002);

then(userRepository).should(never()).save(any(User.class));
```

---

### A-04. 비밀번호 해시 저장 확인

| 항목 | 내용 |
|------|------|
| **Given** | 정상 요청, `rawPassword = "Password1!"` |
| **When** | `signup(request)` 호출 |
| **Then** | `encode("Password1!")` 호출됨, 저장된 `passwordHash ≠ "Password1!"` (평문 저장 금지) |

#### 포인트

```java
// 행위 검증
then(passwordEncoder).should().encode("Password1!");

// 평문 저장 금지 확인
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
then(userRepository).should().save(captor.capture());

assertThat(captor.getValue().getPasswordHash())
    .isNotEqualTo("Password1!")     // 평문 아님
    .isEqualTo("hashed-password");  // 해시값 일치
```

---

## B. AuthService — 로그인 `login`

### B-01. 정상 로그인

| 항목 | 내용 |
|------|------|
| **Given** | `findByEmail()` → User 존재 / `matches()` = `true` / 토큰 생성 stub |
| **When** | `login(request)` 호출 |
| **Then** | Redis에 Refresh 토큰 저장 (TTL 7일), `resolveLoginFailureLogs()` 호출, `LoginResponse` 반환 |

#### 포인트

`opsForValue()` 체이닝 때문에 `ValueOperations`를 별도 Mock으로 선언 후 연결

```java
given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
```

TTL까지 포함해 정확히 검증

```java
then(valueOperations).should()
    .set("refresh:1", "refresh-token", 7L, TimeUnit.DAYS);
```

---

### B-02. 존재하지 않는 이메일

| 항목 | 내용 |
|------|------|
| **Given** | `findByEmail(email)` = `Optional.empty()` |
| **When** | `login(request)` 호출 |
| **Then** | `BusinessException(AUTH_003)` 발생, `saveSystemErrorLog(null, ...)` 호출 |

#### 포인트

User 자체가 없으므로 `saveSystemErrorLog`의 `userId` 인자가 **`null`** 이어야 함

```java
then(adminLogSaveService).should()
    .saveSystemErrorLog(null, "WARN", "AUTH_003", any(), any(), any());
```

> 인자 6개를 모두 명시하여 실수로 `userId`가 채워지는 버그도 잡아냄

---

### B-03. 비밀번호 불일치

| 항목 | 내용 |
|------|------|
| **Given** | User 존재 / `matches(rawPw, hash)` = `false` |
| **When** | `login(request)` 호출 |
| **Then** | `BusinessException(AUTH_003)` 발생, `saveSystemErrorLog(..., userId)` 호출, Redis 저장 미호출 |

#### 포인트

**에러코드가 B-02와 동일한 이유** — 이메일 없음 / 비밀번호 틀림을 외부에 구분해 노출하면 보안 위험

B-02와의 차이점: User를 찾은 뒤 실패했으므로 `userId` 특정 가능 → `saveSystemErrorLog` 마지막 인자가 `1L`

```java
then(adminLogSaveService).should()
    .saveSystemErrorLog(any(), "WARN", "AUTH_003", any(), any(), 1L);

// Redis 비호출 검증
then(stringRedisTemplate).shouldHaveNoInteractions();
```

---

### B-04. 로그인 성공 시 실패 이력 해소

| 항목 | 내용 |
|------|------|
| **Given** | 정상 로그인 조건 (B-01과 동일) |
| **When** | `login(request)` 호출 |
| **Then** | `resolveLoginFailureLogs(userId)` 호출됨, `saveSystemErrorLog()` 미호출 |

#### 포인트

"이전 실패 이력"은 DB 데이터이므로 Given 코드에 반영 불필요 → **메서드 호출 여부만 검증**

```java
then(adminLogSaveService).should().resolveLoginFailureLogs(1L);

then(adminLogSaveService).should(never())
    .saveSystemErrorLog(any(), any(), any(), any(), any(), any());
```

> B-01과 Given이 같지만 관심사가 다름
> - **B-01** → 토큰 발급 · Redis 저장
> - **B-04** → 실패 이력 해소 · 에러 로그 미기록

---

## C. AuthService — 로그아웃 `logout`

### C-01. 정상 로그아웃

| 항목 | 내용 |
|------|------|
| **Given** | Redis에 `refresh:{userId}` 존재 |
| **When** | `logout(userId)` 호출 |
| **Then** | `redisTemplate.delete("refresh:1")` 호출됨 |

#### 포인트

`logout()`은 `redisTemplate.delete("refresh:" + userId)` 한 줄이 전부이므로 별도 Given stub 불필요

```java
authService.logout(1L);

// 키 이름이 정확히 조합됐는지 검증
then(stringRedisTemplate).should().delete("refresh:1");
```

> prefix 오타나 `userId` 누락 시 이 검증에서 바로 걸림

---

### C-02. Redis에 토큰 없는 경우

| 항목 | 내용 |
|------|------|
| **Given** | Redis 빈 상태 (stub 없음) |
| **When** | `logout(userId)` 호출 |
| **Then** | 예외 없이 정상 종료, `redisTemplate.delete()` 호출됨 |

#### 포인트

stub 없이도 동작하는 이유 — Mockito는 stub되지 않은 `void` 메서드를 호출하면 아무것도 하지 않음 (실제 Redis도 없는 키를 `delete()`하면 예외 없이 `0` 반환)

```java
assertThatCode(() -> authService.logout(1L))
    .doesNotThrowAnyException();

then(stringRedisTemplate).should().delete("refresh:1");
```

---

### C-01 vs C-02 비교

| | C-01 | C-02 |
|-|------|------|
| **관심사** | 토큰이 있을 때 정상 삭제되는가 | 토큰이 없을 때도 안전하게 동작하는가 |
| **Given stub** | 있음 | 없음 |
| **핵심 assertion** | `delete()` 호출 여부 | 예외 미발생 + `delete()` 호출 여부 |

> 로그아웃은 토큰 존재 여부와 관계없이 **항상 성공**해야 하므로 두 케이스 모두 검증 필요

---

## D. AuthService — 토큰 재발급 `reissue`

### D-01. 정상 재발급

| 항목 | 내용 |
|------|------|
| **Given** | `validateRefreshToken()` 정상 통과, `getUserId()` = `1L`, Redis `"refresh:1"` = 요청 토큰과 일치, `findById(1L)` = User 존재 |
| **When** | `reissue(refreshToken)` 호출 |
| **Then** | 새 토큰 생성, Redis `"refresh:1"` 갱신 (TTL 7일), `TokenResponse` 반환 |

#### 실행 흐름

```
토큰 유효성 검증 → Redis 토큰 비교 → User 조회 → 새 토큰 생성 → Redis 갱신
```

#### 포인트

**`validateRefreshToken()`은 `void` 메서드** — 기본적으로 통과되지만 의도 명시를 위해 명시적 stub 권장

```java
doNothing().when(jwtProvider).validateRefreshToken("old-refresh-token");
```

**`opsForValue()`는 한 번만 stub** — `get()`과 `set()` 두 호출 모두 같은 Mock 반환

```java
given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
given(valueOperations.get("refresh:1")).willReturn("old-refresh-token");
```

Then 검증 3가지

```java
// 1. 새 토큰 생성 호출
then(jwtProvider).should().generateAccessToken(any());
then(jwtProvider).should().generateRefreshToken(any());

// 2. Redis 갱신 (TTL 7일)
then(valueOperations).should()
    .set("refresh:1", "new-refresh-token", 7L, TimeUnit.DAYS);

// 3. 응답값 확인
assertThat(response.getAccessToken()).isEqualTo("new-access-token");
assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
```

---

### D-02. JWT 서명 / 형식 오류

| 항목 | 내용 |
|------|------|
| **Given** | `validateRefreshToken("invalid-token")` → 예외 발생 |
| **When** | `reissue("invalid-token")` 호출 |
| **Then** | 예외 그대로 전파, Redis 조회 미호출 |

#### 포인트

`void` 메서드 예외 stub — `willThrow().given()` 순서로 작성

```java
willThrow(new BusinessException(ErrorCode.AUTH_005))
    .given(jwtProvider).validateRefreshToken("invalid-token");

then(stringRedisTemplate).shouldHaveNoInteractions();
```

> "예외 발생"이 아닌 **"예외 전파"** — 예외를 처음 생성하는 곳은 `JwtProvider`, `AuthService`는 그대로 통과시킴

---

### D-03. Redis에 토큰 없음 (만료 또는 로그아웃 후 재시도)

| 항목 | 내용 |
|------|------|
| **Given** | JWT 유효, `Redis.get("refresh:1")` = `null` |
| **When** | `reissue(refreshToken)` 호출 |
| **Then** | `BusinessException(AUTH_005)` 발생 |

#### 실제 발생 케이스

- 사용자가 로그아웃하여 `delete("refresh:1")`로 삭제된 경우
- Redis TTL 7일 경과로 자동 만료된 경우

#### 포인트

`null` 반환을 명시적으로 stub — Mockito 기본값이 `null`이지만 "Redis에 값이 없다"는 의도를 코드에서 표현

```java
given(valueOperations.get("refresh:1")).willReturn(null);
```

실제 코드의 검증 조건

```java
if (storedToken == null || !storedToken.equals(refreshToken))
//      ↑ D-03 여기서 걸림           ↑ D-04 여기서 걸림
    throw new BusinessException(AUTH_005);
```

---

### D-04. Redis 토큰 불일치 (다른 기기에서 재발급 후 이전 토큰 재사용)

| 항목 | 내용 |
|------|------|
| **Given** | JWT 유효, `Redis.get("refresh:1")` = `"different-token"` |
| **When** | `reissue(refreshToken)` 호출 |
| **Then** | `BusinessException(AUTH_005)` 발생 |

#### 실제 발생 케이스

기기 A에서 재발급 → Redis `"refresh:1"`이 새 토큰으로 교체 → 기기 B가 이전 토큰으로 재시도 → JWT는 유효하나 Redis 값 불일치

#### D-03 vs D-04 비교

| | D-03 | D-04 |
|-|------|------|
| `get("refresh:1")` 반환값 | `null` | `"different-token"` |
| 걸리는 조건 | `storedToken == null` | `!storedToken.equals(refreshToken)` |
| 에러코드 | `AUTH_005` | `AUTH_005` |

> 에러코드는 같아도 **원인이 다른 상황** — 로직 분리 가능성을 고려해 케이스를 나눠 관리

---

### D-05. 사용자 없음 (회원 탈퇴 후 토큰 재사용)

| 항목 | 내용 |
|------|------|
| **Given** | JWT 유효, Redis 토큰 일치, `findById(1L)` = `Optional.empty()` |
| **When** | `reissue(refreshToken)` 호출 |
| **Then** | `BusinessException(USER_001)` 발생 |

#### 실제 발생 케이스

회원 탈퇴 후 Redis에 토큰이 아직 남아있을 때

#### 포인트

에러코드가 `AUTH_005`가 아닌 **`USER_001`** — 계층이 다름

| 단계 | 계층 | 에러코드 |
|------|------|----------|
| JWT 검증 실패 (D-02) | 인증 계층 | `AUTH_005` |
| Redis 토큰 없음 / 불일치 (D-03, D-04) | 인증 계층 | `AUTH_005` |
| **사용자 없음 (D-05)** | **도메인 계층** | **`USER_001`** |

```java
given(userRepository.findById(1L)).willReturn(Optional.empty());
```

---

## E. AuthService — `completeSignup` (Firebase 폴백)

> `@Transactional` 메서드 — Firebase 실패 여부와 관계없이 `user.activate()` **항상** 호출됨

### E-01. Firebase 정상 생성

| 항목 | 내용 |
|------|------|
| **Given** | `createUser()` → `UserRecord(uid="firebase-uid-123")` 반환, `linkUser()` 정상 |
| **When** | `completeSignup(userId)` 호출 |
| **Then** | `updateFirebaseUid("firebase-uid-123")` 호출, `linkUser()` 호출, `user.activate()` → `status = ACTIVE` |

#### 포인트

`FirebaseAuth`는 static 메서드이므로 `MockedStatic` 필요

```java
try (MockedStatic<FirebaseAuth> mockedStatic = mockStatic(FirebaseAuth.class)) {
    FirebaseAuth mockAuth = mock(FirebaseAuth.class);
    mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
    given(mockAuth.createUser(any())).willReturn(mockUserRecord);
    // ...
}
// 블록 종료 시 static mock 자동 원복 → 다른 테스트에 영향 없음
```

Then 검증 3가지

```java
// 1. uid 상태 변화로 간접 검증
assertThat(user.getFirebaseUid()).isEqualTo("firebase-uid-123");

// 2. linkUser 인자 4개 모두 명시
then(transactionServerClient).should()
    .linkUser(1L, "firebase-uid-123", "홍길동", "01012345678");

// 3. 활성화 확인
assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
```

---

### E-02. Firebase `createUser` 실패 → `getUserByEmail` 폴백 성공

| 항목 | 내용 |
|------|------|
| **Given** | `createUser()` → `FirebaseAuthException`, `getUserByEmail(email)` → `UserRecord(uid="existing-uid")` |
| **When** | `completeSignup(userId)` 호출 |
| **Then** | `updateFirebaseUid("existing-uid")` 호출 (폴백 uid), `linkUser()` 호출, `user.activate()` |

#### 실행 흐름

```
createUser() → FirebaseAuthException
  └─ catch → log.warn() → getUserByEmail() → UserRecord(uid="existing-uid")
                                                    └─ updateFirebaseUid("existing-uid")
                                                    └─ linkUser(1L, "existing-uid", ...)
user.activate()  ← try-catch 바깥, 무조건 실행
```

#### 포인트

`FirebaseAuth.getInstance()`가 두 번 호출되지만 **같은 mock 인스턴스**를 반환 → 첫 번째(createUser)는 예외, 두 번째(getUserByEmail)는 정상 반환 각각 stub 가능

```java
given(mockAuth.createUser(any())).willThrow(mock(FirebaseAuthException.class));
given(mockAuth.getUserByEmail("user@test.com")).willReturn(mockUserRecord); // uid="existing-uid"
```

Then — 폴백 uid 반영 확인

```java
assertThat(user.getFirebaseUid()).isEqualTo("existing-uid");
then(transactionServerClient).should()
    .linkUser(1L, "existing-uid", "홍길동", "01012345678");
assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
```

---

### E-03. Firebase 전체 실패 (`createUser` + `getUserByEmail` 모두 실패)

| 항목 | 내용 |
|------|------|
| **Given** | `createUser()` → `FirebaseAuthException`, `getUserByEmail()` → `FirebaseAuthException` |
| **When** | `completeSignup(userId)` 호출 |
| **Then** | `updateFirebaseUid()` 미호출 (임시값 유지), `linkUser()` 미호출, `user.activate()` 호출됨 |

#### 실행 흐름

```
createUser() → FirebaseAuthException
  └─ catch → log.warn() → getUserByEmail() → FirebaseAuthException
                                └─ 바깥 catch → log.error()
                                └─ updateFirebaseUid() 미호출
                                └─ linkUser() 미호출
user.activate()  ← try-catch 바깥, 무조건 실행
```

#### 포인트

```java
assertThat(user.getFirebaseUid()).isEqualTo("temp-uid");      // 임시값 그대로 유지
then(transactionServerClient).shouldHaveNoInteractions();      // linkUser 미호출
assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);    // activate()는 무조건 실행
```

> Firebase 장애가 회원가입 완료 자체를 막아서는 안 된다는 서비스 정책이 코드로 표현된 것

---

### E-04. `transactionServerClient.linkUser` 실패

| 항목 | 내용 |
|------|------|
| **Given** | Firebase 정상 (`uid="firebase-uid-123"`), `linkUser()` → `RuntimeException` |
| **When** | `completeSignup(userId)` 호출 |
| **Then** | 예외 없이 정상 종료, `updateFirebaseUid()` 정상 적용, `user.activate()` 호출됨 |

#### 실행 흐름

```
createUser() → UserRecord(uid="firebase-uid-123")
  └─ updateFirebaseUid("firebase-uid-123")
  └─ linkUser() → RuntimeException
       └─ 내부 catch → log.warn() → 예외 무시
user.activate()  ← try-catch 바깥, 무조건 실행
```

#### 포인트

```java
willThrow(new RuntimeException("연동 실패"))
    .given(transactionServerClient)
    .linkUser(anyLong(), anyString(), anyString(), anyString());

// 1. 예외 미전파 — 정상 종료
assertThatCode(() -> authService.completeSignup(1L))
    .doesNotThrowAnyException();

// 2. Firebase uid는 linkUser 실패 이전에 이미 반영됨
assertThat(user.getFirebaseUid()).isEqualTo("firebase-uid-123");

// 3. activate()는 독립적으로 실행
assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
```

---

### E-01 ~ E-04 종합 비교

| 시나리오 | Firebase | linkUser | `firebaseUid` 업데이트 | `activate()` |
|----------|----------|----------|------------------------|--------------|
| **E-01** | 성공 | 성공 | ✅ 신규 uid | ✅ |
| **E-02** | 폴백 성공 | 성공 | ✅ 폴백 uid | ✅ |
| **E-03** | 전체 실패 | 미호출 | ❌ 임시값 유지 | ✅ |
| **E-04** | 성공 | 실패 | ✅ 신규 uid | ✅ |

> `user.activate()`는 어떤 경우에도 반드시 호출된다 — `completeSignup()`의 핵심 불변

---

## F. PinService — PIN 등록 `registerPin`

> 실제 구현 위치: `AuthService.registerPin`

### 전체 실행 흐름

```
1. validatePinFormat(pin)      ← F-03 ~ F-06 여기서 조기 종료
2. userRepository.findById()   ← F-07 여기서 조기 종료
3. existsByUserId() 분기
   ├── false → PinAuth 신규 생성 → save()     ← F-01
   └── true  → findByUserId → changePin()     ← F-02
```

---

### F-01. 최초 PIN 등록

| 항목 | 내용 |
|------|------|
| **Given** | `existsByUserId(userId)` = `false` / `findById(userId)` = User / PIN `"147258"` |
| **When** | `registerPin(userId, request)` 호출 |
| **Then** | `pinAuthRepository.save()` 호출, `PinAuth(failCount=0, lockedYn=false)`, `encode("147258")` 호출 |

#### 포인트

```java
ArgumentCaptor<PinAuth> captor = ArgumentCaptor.forClass(PinAuth.class);
then(pinAuthRepository).should().save(captor.capture());

assertThat(captor.getValue().getPinHash()).isEqualTo("encoded-147258");
assertThat(captor.getValue().getFailCount()).isZero();
assertThat(captor.getValue().isLockedYn()).isFalse();
```

> `failCount=0`, `lockedYn=false` — 최초 등록 시 깨끗한 상태로 시작한다는 서비스 정책을 명시

---

### F-02. PIN 재등록 (upsert)

| 항목 | 내용 |
|------|------|
| **Given** | `existsByUserId(userId)` = `true` / 기존 `PinAuth(pinHash="old-encoded-pin")` 존재 / 새 PIN `"258369"` |
| **When** | `registerPin(userId, request)` 호출 |
| **Then** | `existing.changePin("encoded-258369")` 호출, `save()` 미호출 |

#### 포인트

재등록은 기존 엔티티 수정 방식 → `save()` 호출 없이 JPA dirty checking으로 반영

```java
assertThat(existingPinAuth.getPinHash()).isEqualTo("encoded-258369");
assertThat(existingPinAuth.getFailCount()).isZero();    // changePin() 내부에서 리셋
assertThat(existingPinAuth.isLockedYn()).isFalse();     // changePin() 내부에서 리셋

then(pinAuthRepository).should(never()).save(any(PinAuth.class));
```

---

### F-03 ~ F-06. PIN 형식 유효성 검증

> `validatePinFormat()`은 `findById()`보다 먼저 실행 → User stub 불필요, Repository 미호출

| 시나리오 | 입력 PIN | 걸리는 조건 |
|----------|----------|-------------|
| **F-03** | `"1234"` | `\d{6}` 정규식 불충족 (4자리) |
| **F-04** | `"111111"` | `distinct().count() == 1` (모두 동일) |
| **F-05** | `"123456"` | ascending 루프 (연속 오름차순) |
| **F-06** | `"654321"` | descending 루프 (연속 내림차순) |

모두 `BusinessException(AUTH_010)` 발생, `pinAuthRepository` 미호출

```java
assertThatThrownBy(() -> authService.registerPin(1L, request))
    .isInstanceOf(BusinessException.class)
    .extracting(ex -> ((BusinessException) ex).getErrorCode())
    .isEqualTo(ErrorCode.AUTH_010);

then(pinAuthRepository).shouldHaveNoInteractions();
```

> F-05(오름차순) · F-06(내림차순) 두 케이스를 모두 커버해야 PIN 유효성 검증이 완전해짐

---

### F-07. 사용자 없음

| 항목 | 내용 |
|------|------|
| **Given** | PIN `"147258"` (형식 통과) / `findById(userId)` = `Optional.empty()` |
| **When** | `registerPin(userId, request)` 호출 |
| **Then** | `BusinessException(USER_001)` 발생, `pinAuthRepository.save()` 미호출 |

#### 포인트

형식 검증은 통과하지만 User 조회 단계에서 예외 발생 → orphan 데이터 생성 방지

```java
assertThatThrownBy(() -> authService.registerPin(1L, request))
    .isInstanceOf(BusinessException.class)
    .extracting(ex -> ((BusinessException) ex).getErrorCode())
    .isEqualTo(ErrorCode.USER_001);

then(pinAuthRepository).should(never()).save(any(PinAuth.class));
```

---

### F 시나리오 총정리

| 시나리오 | 조기 종료 위치 | 에러코드 | `save()` 호출 |
|----------|--------------|----------|:-------------:|
| **F-01** | — (정상) | — | ✅ |
| **F-02** | — (정상, upsert) | — | ❌ |
| **F-03** | `validatePinFormat()` | `AUTH_010` | ❌ |
| **F-04** | `validatePinFormat()` | `AUTH_010` | ❌ |
| **F-05** | `validatePinFormat()` | `AUTH_010` | ❌ |
| **F-06** | `validatePinFormat()` | `AUTH_010` | ❌ |
| **F-07** | `findById()` | `USER_001` | ❌ |

---

## G. PinService — PIN 검증 `verifyPin`

> `@Transactional(noRollbackFor = BusinessException.class)`  
> PIN 실패 횟수 증가가 예외 발생 시에도 롤백되지 않아야 함

### 전체 실행 흐름

```
1. pinAuthRepository.findByUserId()   ← G-05 여기서 조기 종료
2. lockedYn == true 체크              ← G-02 여기서 조기 종료 (PIN 비교 없이)
3. passwordEncoder.matches() 체크
   ├── false → pinAuth.fail()
   │          ├── failCount < 5  → AUTH_008    ← G-03
   │          └── failCount >= 5 → updateStatus(LOCKED) → AUTH_009  ← G-04
   └── true  → pinAuth.resetFailCount()         ← G-01
```

---

### G-01. 정상 PIN 검증

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=false, failCount=2)` / `matches()` = `true` |
| **When** | `verifyPin(userId, request)` 호출 |
| **Then** | `resetFailCount()` 호출 → `failCount=0`, 예외 없음 |

#### 포인트

`failCount=2`로 시작하는 이유 — `resetFailCount()` 호출 여부를 **값 변화**로 증명하기 위함 (`0`으로 시작하면 호출 여부 구분 불가)

```java
assertThatCode(() -> authService.verifyPin(1L, request))
    .doesNotThrowAnyException();

assertThat(pinAuth.getFailCount()).isZero();  // 이전 실패 이력 2회가 성공 시 초기화됨
```

---

### G-02. 잠금 상태에서 검증 시도

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=true, failCount=5)` |
| **When** | `verifyPin(userId, anyPin)` 호출 |
| **Then** | `BusinessException(AUTH_009)` 발생, `passwordEncoder` 미호출, `failCount` 유지 |

#### 포인트

`lockedYn` 체크는 `matches()` 이전에 수행 → PIN이 맞는지 확인조차 하지 않음

```java
then(passwordEncoder).shouldHaveNoInteractions();  // PIN 비교 시도 없음
assertThat(pinAuth.getFailCount()).isEqualTo(5);   // 잠금 상태에서 시도해도 failCount 증가 없음
```

---

### G-03. PIN 불일치 (1~4회 — 잠금 미발생)

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=false, failCount=3)` / `matches()` = `false` |
| **When** | `verifyPin(userId, wrongPin)` 호출 |
| **Then** | `failCount=4`, `lockedYn=false` 유지, `BusinessException(AUTH_008)` 발생 |

#### 포인트

```java
assertThat(pinAuth.getFailCount()).isEqualTo(4);  // 정확히 1 증가
assertThat(pinAuth.isLockedYn()).isFalse();        // 5회 미만 → 잠금 없음

assertThatThrownBy(() -> authService.verifyPin(1L, request))
    .isInstanceOf(BusinessException.class)
    .extracting(ex -> ((BusinessException) ex).getErrorCode())
    .isEqualTo(ErrorCode.AUTH_008);
```

---

### G-04. PIN 5회 실패 → 잠금 처리

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=false, failCount=4)` + 내장 `User(status=ACTIVE)` / `matches()` = `false` |
| **When** | `verifyPin(userId, wrongPin)` 호출 |
| **Then** | `failCount=5`, `lockedYn=true`, `lockedAt` 기록, `user.status=LOCKED`, `BusinessException(AUTH_009)` 발생 |

#### 포인트

`PinAuth` 내부에 실제 `User` 객체를 내장 → `updateStatus(LOCKED)` 호출 결과를 상태 변화로 직접 검증

```java
assertThat(pinAuth.getFailCount()).isEqualTo(5);
assertThat(pinAuth.isLockedYn()).isTrue();
assertThat(pinAuth.getLockedAt()).isNotNull();
assertThat(pinAuth.getUser().getStatus()).isEqualTo(UserStatus.LOCKED);
```

> 5회 실패 직후 `lockedYn=true`로 전환되어 `AUTH_009`가 즉시 반환됨 (`AUTH_008`이 아님)

---

### G-05. PinAuth 없음

| 항목 | 내용 |
|------|------|
| **Given** | `findByUserId(userId)` = `Optional.empty()` |
| **When** | `verifyPin(userId, pin)` 호출 |
| **Then** | `BusinessException(USER_001)` 발생, `passwordEncoder` 미호출 |

```java
then(passwordEncoder).shouldHaveNoInteractions();
```

---

### G-03 vs G-04 핵심 차이

| | G-03 | G-04 |
|-|------|------|
| `failCount` 진입값 | 3 | 4 |
| `fail()` 후 `failCount` | 4 | 5 |
| `lockedYn` | `false` | `true` |
| `lockedAt` | `null` | 기록됨 |
| `user.status` | 변화 없음 | `LOCKED` |
| 예외 코드 | `AUTH_008` | `AUTH_009` |

**G-04에서 `AUTH_009`가 반환되는 이유**  
`fail()` 호출 직후 `lockedYn=true`로 전환 → 곧바로 `if (lockedYn)` 조건에 걸려 `AUTH_009` 발생. 5회 실패 시점에 잠금 적용과 잠금 예외가 동시에 반환됨.

---

## H. PinService — PIN 변경 `changePin`

> `@Transactional(noRollbackFor = BusinessException.class)`

### 전체 실행 흐름

```
1. pinAuthRepository.findByUserId()   ← H-05 여기서 조기 종료
2. lockedYn == true 체크              ← H-02 여기서 조기 종료 (matches 미호출)
3. passwordEncoder.matches() 체크
   └── false → pinAuth.fail() + AUTH_008        ← H-03
4. validatePinFormat(newPin)
   └── 실패 → AUTH_010 (pinHash 변경 없음)      ← H-04
5. pinAuth.changePin(encode(newPin))             ← H-01 정상 완료
```

---

### H-01. 정상 PIN 변경

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=false, failCount=2)` / `matches(currentPin)` = `true` / 새 PIN `"369147"` 유효 |
| **When** | `changePin(userId, request)` 호출 |
| **Then** | `pinHash` 교체, `failCount=0`, `lockedYn=false`, `pinChangedAt` 기록, 예외 없음 |

#### 포인트

`failCount=2`로 시작하는 이유 — `changePin()` 내부의 실패 이력 리셋을 **값 변화**로 증명하기 위함

```java
assertThatCode(() -> authService.changePin(1L, request))
    .doesNotThrowAnyException();

assertThat(pinAuth.getPinHash()).isEqualTo("encoded-369147");
assertThat(pinAuth.getFailCount()).isZero();
assertThat(pinAuth.isLockedYn()).isFalse();
assertThat(pinAuth.getPinChangedAt()).isNotNull();
```

---

### H-02. 잠금 상태에서 변경 시도

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=true, failCount=5)` |
| **When** | `changePin(userId, request)` 호출 |
| **Then** | `BusinessException(AUTH_009)` 발생, `passwordEncoder` 미호출, `failCount` 유지 |

#### 포인트

`lockedYn` 체크가 `matches()` 이전에 수행 → 현재 PIN 확인조차 하지 않고 즉시 종료

```java
then(passwordEncoder).shouldHaveNoInteractions();
assertThat(pinAuth.getFailCount()).isEqualTo(5);
```

---

### H-03. 현재 PIN 불일치

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=false, failCount=0)` / `matches(currentPin)` = `false` |
| **When** | `changePin(userId, request)` 호출 |
| **Then** | `pinAuth.fail()` 호출 → `failCount=1`, `BusinessException(AUTH_008)` 발생 |

#### 포인트

현재 PIN이 틀리면 `fail()`이 먼저 누적 → 5회 반복 시 계정 잠금 발생 (`verifyPin`과 동일 메커니즘)

```java
assertThat(pinAuth.getFailCount()).isEqualTo(1);

assertThatThrownBy(() -> authService.changePin(1L, request))
    .isInstanceOf(BusinessException.class)
    .extracting(ex -> ((BusinessException) ex).getErrorCode())
    .isEqualTo(ErrorCode.AUTH_008);
```

---

### H-04. 새 PIN 유효성 실패 (반복 숫자)

| 항목 | 내용 |
|------|------|
| **Given** | `PinAuth(lockedYn=false)` / `matches(currentPin)` = `true` / 새 PIN `"999999"` |
| **When** | `changePin(userId, request)` 호출 |
| **Then** | `BusinessException(AUTH_010)` 발생, `pinAuth.changePin()` 미호출, 기존 `pinHash` 유지 |

#### 포인트

현재 PIN은 일치하지만 새 PIN이 형식 검증에서 탈락 → `fail()` 미호출 (실패 카운트 오르지 않음)

```java
assertThat(pinAuth.getPinHash()).isEqualTo("encoded-pin");  // 기존 PIN 그대로 유지
```

> **`fail()` 누적 규칙** — 현재 PIN이 틀렸을 때만 발생 / 새 PIN이 약해서 거절되는 경우는 카운트 오르지 않음

---

### H-05. PinAuth 없음

| 항목 | 내용 |
|------|------|
| **Given** | `findByUserId(userId)` = `Optional.empty()` |
| **When** | `changePin(userId, request)` 호출 |
| **Then** | `BusinessException(USER_001)` 발생, `passwordEncoder` 미호출 |

```java
then(passwordEncoder).shouldHaveNoInteractions();
```

---

### H 시나리오 총정리

| 시나리오 | 조기 종료 위치 | `fail()` 호출 | `changePin()` 호출 | 에러코드 |
|----------|--------------|:-------------:|:------------------:|----------|
| **H-01** | — (정상) | ❌ | ✅ | — |
| **H-02** | `lockedYn` 체크 | ❌ | ❌ | `AUTH_009` |
| **H-03** | `matches()` 실패 | ✅ | ❌ | `AUTH_008` |
| **H-04** | `validatePinFormat()` 실패 | ❌ | ❌ | `AUTH_010` |
| **H-05** | `findByUserId()` | ❌ | ❌ | `USER_001` |

---

## I. UserService — 내 정보 조회 `getMe`

> `@Mock` 2개 (`userRepository`, `userProfileRepository`) — AuthService 대비 단순한 의존성 구조

### 전체 실행 흐름

```
1. userRepository.findById(userId)             ← I-02 여기서 조기 종료
2. userProfileRepository.findByUserId(userId)  ← I-03 여기서 조기 종료
3. UserResponse.of(user, profile) 반환         ← I-01 정상 완료
```

---

### I-01. 정상 조회

| 항목 | 내용 |
|------|------|
| **Given** | `findById()` = User / `findByUserId()` = UserProfile |
| **When** | `getMe(userId)` 호출 |
| **Then** | `UserResponse.of(user, profile)` 반환 — User + UserProfile 양쪽 필드 모두 포함 |

#### 포인트

`createdAt`은 `@PrePersist`로만 설정되는 필드 → 빌더 사용 불가, `ReflectionTestUtils`로 직접 주입

```java
ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 5, 1, 10, 0));
```

`role`, `status`는 enum이지만 `UserResponse.of()` 내부에서 `.name()`으로 변환 → **String으로 비교**

```java
assertThat(response.getUserId()).isEqualTo(1L);
assertThat(response.getEmail()).isEqualTo("user@test.com");
assertThat(response.getUserName()).isEqualTo("홍길동");
assertThat(response.getRole()).isEqualTo("USER");      // enum → String
assertThat(response.getStatus()).isEqualTo("ACTIVE");  // enum → String
assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 0));

assertThat(response.isFreelancerYn()).isTrue();
assertThat(response.getJobType()).isEqualTo("DEVELOPER");
```

---

### I-02. User 없음

| 항목 | 내용 |
|------|------|
| **Given** | `findById(userId)` = `Optional.empty()` |
| **When** | `getMe(userId)` 호출 |
| **Then** | `BusinessException(USER_001)` 발생, `userProfileRepository` 미호출 |

#### 포인트

User 조회 실패 시 불필요한 프로필 조회 없이 즉시 종료됨을 명시적으로 검증

```java
then(userProfileRepository).shouldHaveNoInteractions();
```

---

### I-03. UserProfile 없음

| 항목 | 내용 |
|------|------|
| **Given** | `findById()` = User 존재 / `findByUserId()` = `Optional.empty()` |
| **When** | `getMe(userId)` 호출 |
| **Then** | `BusinessException(USER_001)` 발생 |

#### 포인트

User는 있으나 프로필이 없는 불완전한 상태 — `findProfile()` 내부도 동일하게 `USER_001` 발생

> I-02와 같은 에러코드 → 어떤 계층에서 실패했는지 외부에 노출하지 않는 설계

---

### I-02 vs I-03 비교

| | I-02 | I-03 |
|-|------|------|
| **실패 지점** | `findUser()` — User 조회 실패 | `findProfile()` — Profile 조회 실패 |
| **에러코드** | `USER_001` | `USER_001` |
| `userProfileRepository` 호출 | ❌ 미호출 | ✅ 호출됨 |

> I-02에서 `shouldHaveNoInteractions()`를 별도 검증하는 이유 — User가 없으면 불필요한 DB 조회 없이 바로 종료된다는 것을 명시적으로 보장

---

## J. UserService — 알림 동의 수정 `updateConsent`

### 전체 실행 흐름

```
1. userRepository.findById(userId)          ← J-03 여기서 조기 종료
2. user.updateNotificationConsent(value)    ← J-01, J-02 상태 변경
   (@Transactional dirty checking → 자동 UPDATE, save() 미호출)
```

---

### J-01. 알림 동의 ON

| 항목 | 내용 |
|------|------|
| **Given** | `User(notificationConsentYn=false)` / `request.getNotificationConsentYn()` = `true` |
| **When** | `updateConsent(userId, request)` 호출 |
| **Then** | `user.notificationConsentYn=true`, `save()` 미호출 |

#### 포인트

`@Transactional` dirty checking 설계 의도를 단위 테스트에서 보장

```java
assertThat(user.getNotificationConsentYn()).isTrue();

// save() 미호출 — dirty checking 의존 설계 명시적 보장
then(userRepository).should().findById(1L);
then(userRepository).shouldHaveNoMoreInteractions();
```

> 누군가 실수로 `save()`를 추가하면 이 테스트가 실패하여 중복 저장 의도를 경고함

---

### J-02. 알림 동의 OFF

| 항목 | 내용 |
|------|------|
| **Given** | `User(notificationConsentYn=true)` / `request.getNotificationConsentYn()` = `false` |
| **When** | `updateConsent(userId, request)` 호출 |
| **Then** | `user.notificationConsentYn=false` |

#### 포인트

J-01과 대칭 구조 — `true → false` 방향 변경도 동일하게 동작함을 검증

```java
assertThat(user.getNotificationConsentYn()).isFalse();
```

> dirty checking 검증은 J-01에서 이미 다루었으므로 J-02는 상태 변화에만 집중

---

### J-03. User 없음

| 항목 | 내용 |
|------|------|
| **Given** | `findById(userId)` = `Optional.empty()` |
| **When** | `updateConsent(userId, request)` 호출 |
| **Then** | `BusinessException(USER_001)` 발생 |

#### 포인트

`getMe()`의 `findUser()` 헬퍼를 공유 → I-02와 동일한 `orElseThrow()` 경로. User가 없으면 동의 값 설정 시도 없이 즉시 종료.

---

### getMe(I-02) vs updateConsent(J-03) 비교

| | `getMe` I-02 | `updateConsent` J-03 |
|-|--------------|----------------------|
| **에러코드** | `USER_001` | `USER_001` |
| `findUser()` 헬퍼 공유 | ✅ | ✅ |
| `userProfileRepository` 사용 | ✅ (이후 `findProfile()` 호출) | ❌ (애초에 미사용) |
| 추가 검증 | `shouldHaveNoInteractions()` | 불필요 |

---

### dirty checking 설계 의도

`updateConsent()`에는 명시적 `save()` 호출이 없음 — JPA가 트랜잭션 종료 시 변경된 필드를 자동 감지하여 `UPDATE` 쿼리 실행

단위 테스트에서는 실제 트랜잭션이 동작하지 않으므로 dirty checking 자체는 검증 불가 → **`save()` 미호출 검증**으로 이 설계 의도를 대신 보장

---

## 구현 시 주의사항

### 1. `@Transactional(noRollbackFor = BusinessException.class)` 검증

`verifyPin`, `changePin`은 PIN 불일치 예외가 발생해도 `fail()` 호출(failCount 증가)이 롤백되지 않아야 함

| 검증 방식 | 내용 |
|-----------|------|
| **단위 테스트** | Mockito `verify`로 `pinAuth.fail()` 호출 여부만 확인 |
| **통합 테스트** | 실제 롤백 미발생 동작 검증 |

> 단위 테스트에서는 트랜잭션이 동작하지 않으므로 롤백 여부는 통합 테스트에서 별도 검증 필요

---

### 2. Firebase Static Mocking

`FirebaseAuth.getInstance()`는 static 메서드이므로 일반 `@Mock`으로 제어 불가 → `MockedStatic` 사용

```java
try (MockedStatic<FirebaseAuth> mockedFirebase = mockStatic(FirebaseAuth.class)) {
    mockedFirebase.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);
    // ...
}
// 블록 종료 시 static mock 자동 원복 → 다른 테스트에 영향 없음
```

---

### 3. PIN 유효성 경계값

`validatePinFormat`은 `private` 메서드이므로 직접 테스트 불가  
→ `registerPin`, `changePin`을 통한 **간접 검증**으로 커버

| 입력 | 조건 | 예상 결과 |
|------|------|-----------|
| `"1234"` | 4자리 | `AUTH_010` |
| `"1234567"` | 7자리 | `AUTH_010` |
| `"abcdef"` | 문자 포함 | `AUTH_010` |
| `"111111"` | 동일 숫자 반복 | `AUTH_010` |
| `"123456"` | 오름차순 연속 | `AUTH_010` |
| `"654321"` | 내림차순 연속 | `AUTH_010` |
| `"147258"` | 유효한 PIN | 정상 처리 |
| `"234567"` | 오름차순 아님 (각 자리 차이 ≠ 1) | 정상 처리 |