# Admin Service 단위 테스트 시나리오 명세

**테스트 방식:** `@ExtendWith(MockitoExtension.class)` — 순수 Mockito, DB/Redis 없음

---

## 목차

- [K. AdminUserService (K-01 ~ K-05)](#k-adminuserservice)
- [L. AdminLogService (L-01 ~ L-02)](#l-adminlogservice)

---

## K. AdminUserService

**파일 위치:** `src/test/java/com/service/domain/admin/service/AdminUserServiceTest.java`

### 공통 설정

```java
@Mock private UserRepository userRepository;
@Mock private LoginHistoryRepository loginHistoryRepository;
@Mock private PinAuthRepository pinAuthRepository;
@Mock private StringRedisTemplate redisTemplate;
@Mock private Cursor<String> cursor;

@InjectMocks private AdminUserService adminUserService;
```

**픽스처 (`buildUser()`)**

```java
User.builder()
    .userId(1L)
    .email("user@test.com")
    .userName("홍길동")
    .role(User.Role.USER)
    .status(User.Status.ACTIVE)
    .build();
```

---

### K-01 — `getUserDetail` isOnline=true

> Redis `refresh:{userId}` 키 존재 시 `isOnline=true` 반환 검증

#### 흐름

```
adminUserService.getUserDetail(1L)
  └─ userRepository.findByIdWithProfile(1L)          → User 반환
  └─ loginHistoryRepository.findTop...(1L, "LOGIN")  → Optional.empty()
  └─ redisTemplate.hasKey("refresh:1")               → true  ← 핵심
  └─ AdminUserDetailResponse.of(user, null, true)
```

#### 테스트 코드

```java
given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.of(buildUser()));
given(loginHistoryRepository.findTopByUserIdAndLoginTypeOrderByLoggedAtDesc(1L, "LOGIN"))
    .willReturn(Optional.empty());
given(redisTemplate.hasKey("refresh:1")).willReturn(true);

AdminUserDetailResponse response = adminUserService.getUserDetail(1L);

assertThat(response.getIsOnline()).isTrue();
assertThat(response.getUserId()).isEqualTo(1L);
assertThat(response.getStatus()).isEqualTo("ACTIVE");
```

#### 대응 서비스 코드

```java
boolean isOnline = Boolean.TRUE.equals(redisTemplate.hasKey("refresh:" + userId));
return AdminUserDetailResponse.of(user, lastLoginAt, isOnline);
```

#### 검증 포인트

| 항목 | 기대값 |
|------|--------|
| `response.getIsOnline()` | `true` |
| `response.getUserId()` | `1L` |
| `response.getStatus()` | `"ACTIVE"` (enum.name() 변환 확인) |

---

### K-02 — `getUserDetail` isOnline=false

> Redis 키 미존재 시 `isOnline=false` 반환 검증 (K-01 대칭)

#### K-01과 차이점

```java
given(redisTemplate.hasKey("refresh:1")).willReturn(false);
// 또는 null을 반환해도 Boolean.TRUE.equals(null) = false → 동일 결과
```

#### 검증 포인트

| 항목 | 기대값 |
|------|--------|
| `response.getIsOnline()` | `false` |

> **참고:** `Boolean.TRUE.equals(...)` 패턴은 null-safe.  
> `hasKey()`가 `null`을 반환해도 `false`로 처리됨.

---

### K-03 — `getUserDetail` User 없음 → `USER_001`

> User 조회 실패 시 예외 즉시 발생 + 이후 로직 미실행 검증

#### 흐름

```
adminUserService.getUserDetail(1L)
  └─ userRepository.findByIdWithProfile(1L)  → Optional.empty()
  └─ throw BusinessException(USER_001)       ← 즉시 종료
  └─ (loginHistoryRepository, redisTemplate 호출 없음)
```

#### 테스트 코드

```java
given(userRepository.findByIdWithProfile(1L)).willReturn(Optional.empty());

// 1. 예외 타입 + 코드 확인
assertThatThrownBy(() -> adminUserService.getUserDetail(1L))
    .isInstanceOf(BusinessException.class)
    .extracting(ex -> ((BusinessException) ex).getErrorCode())
    .isEqualTo(ErrorCode.USER_001);

// 2. 이후 의존성 미호출 확인
then(loginHistoryRepository).shouldHaveNoInteractions();
then(redisTemplate).shouldHaveNoInteractions();
```

#### 검증 포인트

| 항목 | 기대값 |
|------|--------|
| 예외 타입 | `BusinessException` |
| 에러 코드 | `ErrorCode.USER_001` |
| `loginHistoryRepository` 호출 여부 | 0회 (`shouldHaveNoInteractions()`) |
| `redisTemplate` 호출 여부 | 0회 (`shouldHaveNoInteractions()`) |

> **설계 의도:** 예외 발생 후 불필요한 외부 호출이 없어야 함 (early exit 보장).

---

### K-04 — `getUsers` translateSort 알 수 없는 필드 → 그대로 통과

> `SORT_FIELD_MAP`에 없는 필드는 변환 없이 JPA에 그대로 전달되는지 간접 검증

#### 배경

```java
// 서비스 내부 매핑 테이블
Map.of("name", "userName", "createdAt", "createdAt", "email", "email", "status", "status")

// translateSort 핵심 동작
String mapped = SORT_FIELD_MAP.getOrDefault(order.getProperty(), order.getProperty());
//                                                                  ↑ 없으면 원본 그대로
```

#### 테스트 코드

```java
// Redis scan 빈 커서 설정 (getOnlineUserIds() 내부 호출 대응)
given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
given(cursor.hasNext()).willReturn(false);
given(userRepository.findAll(any(Specification.class), any(Pageable.class)))
    .willReturn(new PageImpl<>(List.of()));

Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "unknownField"));
adminUserService.getUsers(null, null, null, null, null, pageable);

ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
then(userRepository).should().findAll(any(Specification.class), pageableCaptor.capture());

List<Sort.Order> orders = pageableCaptor.getValue().getSort().toList();
assertThat(orders).hasSize(1);
assertThat(orders.get(0).getProperty()).isEqualTo("unknownField"); // 변환 없이 통과
```

#### 검증 포인트

| 항목 | 기대값 |
|------|--------|
| 캡처된 sort 필드 개수 | `1` |
| 캡처된 sort 필드 이름 | `"unknownField"` (변환 없음) |

> **Mock 부가 설정 필요:** `getUsers()` 내부에서 `getOnlineUserIds()` → Redis scan 호출이 먼저 발생하므로 빈 커서를 설정해야 함.

---

### K-05 — `getUsers` sort 없는 Pageable → 원본 반환

> `translateSort()` 내부 `orders.isEmpty()` 분기 검증

#### 대응 서비스 코드

```java
private Pageable translateSort(Pageable pageable) {
    List<Sort.Order> orders = pageable.getSort().stream()
        .map(order -> { ... })
        .collect(Collectors.toList());

    if (orders.isEmpty()) {
        return pageable;  // ← 이 분기를 테스트
    }
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
}
```

#### 테스트 코드

```java
Pageable pageable = PageRequest.of(2, 5); // sort 없음
adminUserService.getUsers(null, null, null, null, null, pageable);

ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
then(userRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
Pageable captured = pageableCaptor.getValue();

assertThat(captured.getPageNumber()).isEqualTo(2);
assertThat(captured.getPageSize()).isEqualTo(5);
assertThat(captured.getSort().isSorted()).isFalse();
```

#### 검증 포인트

| 항목 | 기대값 |
|------|--------|
| `captured.getPageNumber()` | `2` |
| `captured.getPageSize()` | `5` |
| `captured.getSort().isSorted()` | `false` |

---

### K-04 vs K-05 비교

| 항목 | K-04 | K-05 |
|------|------|------|
| 입력 pageable | sort=`"unknownField"` 있음 | sort 없음 |
| `orders.isEmpty()` | `false` → 새 PageRequest 생성 | `true` → 원본 pageable 반환 |
| 캡처 결과 | 새로 생성된 PageRequest | 원본 그대로 |

---

## L. AdminLogService

**대상:** `AdminLogService.getDashboard()`

### Redis @Mock 전략

`@Mock StringRedisTemplate redisTemplate` + `@Mock Cursor<String> cursor`로 순수 단위 테스트 환경 구성.

| 메서드 | 사용하는 Redis API |
|--------|-------------------|
| `getUserDetail()` | `hasKey()` |
| `getUsers()` · `getDashboard()` | `scan()` |

---

### 시나리오 요약

| 시나리오 | Given | Then |
|----------|-------|------|
| L-01 | `avgDurationMsByRequestedAtBetween()` → `null` | `response.getAvgApiResponseMs()` = `null` |
| L-02 | `avgDurationMsByRequestedAtBetween()` → `123.7` | `response.getAvgApiResponseMs()` = `124L` |

---

### L-01 — `avgApiResponseMs = null`

- **Given:** `avgDurationMsByRequestedAtBetween()` → `null`
- **When:** `getDashboard(request)` 호출
- **Then:** `response.getAvgApiResponseMs()` = `null`

#### 포인트

`avgMs != null ? Math.round(avgMs) : null`의 **null 경로** 검증.  
나머지 count 메서드들은 Mock 기본값 `0L`로 자동 처리되므로 별도 stub 불필요.

```java
given(apiLogRepository.avgDurationMsByRequestedAtBetween(any(), any()))
    .willReturn(null);

DashboardResponse response = adminLogService.getDashboard(request);

assertThat(response.getAvgApiResponseMs()).isNull();
```

---

### L-02 — `avgApiResponseMs = 124` (반올림)

- **Given:** `avgDurationMsByRequestedAtBetween()` → `123.7`
- **When:** `getDashboard(request)` 호출
- **Then:** `response.getAvgApiResponseMs()` = `124L`

#### 포인트

`Math.round(123.7) = 124` — 반올림 처리 검증.

```java
given(apiLogRepository.avgDurationMsByRequestedAtBetween(any(), any()))
    .willReturn(123.7);

DashboardResponse response = adminLogService.getDashboard(request);

assertThat(response.getAvgApiResponseMs()).isEqualTo(124L);
```