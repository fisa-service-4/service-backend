# Admin Controller 단위 테스트 시나리오 명세

**테스트 방식:** `@WebMvcTest` + `@WithMockUser(roles = "ADMIN")`

---

## 목차

- [M. AdminUserController — PATCH `/admin/users/{id}/status`](#m-adminusercontroller--patch-adminusersidstatus)
- [N. AdminUserController — GET `/admin/users`](#n-adminusercontroller--get-adminusers)
- [O. AdminLogController — PATCH `/admin/logs/error/{id}`](#o-adminlogcontroller--patch-adminlogserrorid)
- [P. AdminServiceHealthController — GET `/admin/services/health`](#p-adminservicehealthcontroller--get-adminserviceshealth)
- [공통 테스트 구조](#공통-테스트-구조-webmvctest)
- [시나리오 우선순위 요약](#시나리오-우선순위-요약)

---

## M. AdminUserController — PATCH `/admin/users/{id}/status`

**대상:** `AdminUserController.updateUserStatus()`  
**파일 위치:** `src/test/java/com/service/domain/admin/controller/AdminUserControllerTest.java`

### 시나리오 요약

| 시나리오 | 입력 | 기대 결과 | 검증 포인트 |
|----------|------|-----------|-------------|
| M-01 | status=ACTIVE, userId=1 | HTTP 200, data.status=ACTIVE | 서비스 1회 호출 확인 (`then().should()`) |
| M-02 | status=LOCKED, userId=1 | HTTP 200, data.status=LOCKED | HTTP 레이어에서 상태값 그대로 반환 확인 |
| M-03 | 존재하지 않는 userId=99 | HTTP 404, error.code=USER_001 | `BusinessException(USER_001)` → `GlobalExceptionHandler` → 404 매핑 |

---

### M-01 — LOCKED → ACTIVE (pin_auth 초기화)

- **Given:** `User(status=LOCKED)`, `PinAuth(lockedYn=true, failCount=5, lockedAt=now)`
- **When:** `PATCH /admin/users/1/status` `{"status":"ACTIVE"}`
- **Then:** HTTP 200, `data.userId=1`, `data.status=ACTIVE`

```java
willDoNothing().given(adminUserService).updateUserStatus(eq(1L), any());

mockMvc.perform(patch("/api/v1/admin/users/1/status")
        .content("{\"status\":\"ACTIVE\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data.userId").value(1))
    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

then(adminUserService).should().updateUserStatus(eq(1L), any());
```

> **컨트롤러 테스트 범위 제한**  
> `pinAuth.unlock()` (`lockedYn=false`, `failCount=0`, `lockedAt=null`)이 실제로 호출되는지는 컨트롤러 테스트에서 확인하지 않는다.  
> `@Transactional` 내에서 `user.updateStatus()` + `pinAuth.unlock()`이 원자적으로 처리되는지는 단위 테스트로 확인 불가한 영역 — 통합 테스트(DB 직접 검증)에서 다뤄야 한다.

---

### M-02 — ACTIVE → SUSPENDED (pin_auth 미변경)

- **Given:** `User(status=ACTIVE)`, `PinAuth(lockedYn=false, failCount=0)`
- **When:** `PATCH /admin/users/1/status` `{"status":"LOCKED"}`
- **Then:** HTTP 200, `data.userId=1`, `data.status=LOCKED`

```java
willDoNothing().given(adminUserService).updateUserStatus(eq(1L), any());

mockMvc.perform(patch("/api/v1/admin/users/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"LOCKED\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data.userId").value(1))
    .andExpect(jsonPath("$.data.status").value("LOCKED"));
```

> **M-01 vs M-02 차이**  
> M-01은 `then().should()`로 서비스 호출 자체를 추가 검증한다.  
> M-02는 응답 본문 포맷만 확인한다.  
> LOCKED 시 `pinAuth.unlock()` 미호출 여부는 서비스 단위 테스트의 책임이므로 컨트롤러 테스트에서는 다루지 않는다.

---

### M-03 — User 없음 → HTTP 404 (USER_001)

- **Given:** `userId=999` 존재하지 않음
- **When:** `PATCH /admin/users/99/status` `{"status":"ACTIVE"}`
- **Then:** HTTP 404, `success=false`, `error.code=USER_001`

```java
willThrow(new BusinessException(ErrorCode.USER_001))
    .given(adminUserService)
    .updateUserStatus(eq(99L), any());

mockMvc.perform(patch("/api/v1/admin/users/99/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"LOCKED\"}"))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.success").value(false))
    .andExpect(jsonPath("$.error.code").value("USER_001"));
```

**에러 처리 흐름**

```
AdminUserService.updateUserStatus() → throw BusinessException(USER_001)
  └─ GlobalExceptionHandler → USER_001 → HTTP 404 Not Found
```

---

## N. AdminUserController — GET `/admin/users`

**대상:** `AdminUserController.getUsers()`  
**파일 위치:** `src/test/java/com/service/domain/admin/controller/AdminUserControllerTest.java`

### 단위 테스트 범위 vs 통합 테스트 범위

| 검증 항목 | 단위 테스트 (현재) | 통합 테스트 (별도 필요) |
|-----------|:------------------:|:-----------------------:|
| 필터 파라미터 → 서비스 전달 매핑 | O | - |
| String → `User.Status` enum 변환 | O | - |
| `isNull()` — 미전달 파라미터 null 처리 | O | - |
| Redis scan → `onlineUserIds` → IN/NOT IN 술어 | X | O |
| `role=USER` 조건으로 ADMIN 계정 제외 | X | O |
| 복합 필터 교집합 결과 | X | O |

> **N 시나리오 핵심:** 각 필터가 서비스 메서드 파라미터에 올바르게 매핑·변환되는지를 검증한다.  
> `isNull()`로 미전달 파라미터가 null로 처리되는지도 함께 확인한다.

### 시나리오 요약

| 시나리오 | 쿼리 파라미터 | 검증 포인트 |
|----------|--------------|-------------|
| N-01 | `loginStatus=ONLINE` | 서비스에 `"ONLINE"` 문자열 그대로 전달 |
| N-02 | `loginStatus=OFFLINE` | 서비스에 `"OFFLINE"` 전달, 나머지 파라미터 null |
| N-03 | `status=ACTIVE` | `"ACTIVE"` → `User.Status.ACTIVE` enum 변환 후 서비스 전달 |
| N-04 | `sort=name` | sort 파라미터 전달 확인 (정렬 로직은 서비스 책임) |
| N-05 | `keyword=홍길동` | 한글 키워드 전달, 검색 결과 응답 포맷 확인 |

---

### N-01 — loginStatus=ONLINE

- **Given:** Redis `"refresh:1"` 존재, `"refresh:2"` 없음
- **When:** `loginStatus=ONLINE`
- **Then:** `userId=1`만 반환, `userId=2` 미포함

> **핵심 흐름 (단위 테스트로 확인 불가)**  
> `getOnlineUserIds()` Redis scan → `onlineUserIds` Set → `buildSpec()`에서 IN 술어 생성

```java
given(adminUserService.getUsers(
        isNull(), isNull(), isNull(), eq("ONLINE"), isNull(), any(Pageable.class)))
    .willReturn(new PageImpl<>(List.of(buildListResponse(1L, "ACTIVE"))));

mockMvc.perform(get("/api/v1/admin/users").param("loginStatus", "ONLINE"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.content[0].userId").value(1));

then(adminUserService).should()
    .getUsers(isNull(), isNull(), isNull(), eq("ONLINE"), isNull(), any(Pageable.class));
```

---

### N-02 — loginStatus=OFFLINE

- **Given:** Redis `"refresh:1"` 존재, `"refresh:2"` 없음
- **When:** `loginStatus=OFFLINE`
- **Then:** `userId=2`만 반환, `userId=1` 미포함

```java
given(adminUserService.getUsers(
        isNull(), isNull(), isNull(), eq("OFFLINE"), isNull(), any(Pageable.class)))
    .willReturn(new PageImpl<>(List.of(buildListResponse(2L, "ACTIVE"))));

mockMvc.perform(get("/api/v1/admin/users").param("loginStatus", "OFFLINE"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.content[0].userId").value(2));

then(adminUserService).should()
    .getUsers(isNull(), isNull(), isNull(), eq("OFFLINE"), isNull(), any(Pageable.class));
```

---

### N-03 — status=ACTIVE (enum 변환)

- **Given:** ACTIVE 2명, SUSPENDED 1명
- **When:** `status=ACTIVE`
- **Then:** ACTIVE 2명만 반환, ADMIN role 계정 제외

> **핵심 (단위 테스트로 확인 불가)**  
> `buildSpec()`이 항상 `role=USER` 조건을 추가하므로 ADMIN 계정이 결과에서 제외된다.

```java
given(adminUserService.getUsers(
        isNull(), eq(User.Status.ACTIVE), isNull(), isNull(), isNull(), any(Pageable.class)))
    .willReturn(new PageImpl<>(List.of(buildListResponse(1L, "ACTIVE"))));

mockMvc.perform(get("/api/v1/admin/users").param("status", "ACTIVE"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));

then(adminUserService).should()
    .getUsers(isNull(), eq(User.Status.ACTIVE), isNull(), isNull(), isNull(), any(Pageable.class));
```

> **검증 핵심:** 컨트롤러에서 쿼리 파라미터 `"ACTIVE"` 문자열이 `User.Status.ACTIVE` enum으로 변환되어 서비스에 전달되는지 확인한다.

---

### N-04 — sort=name

- **Given:** 사용자 여러 명
- **When:** `sort=name`
- **Then:** `userName` 기준 오름차순 정렬

```java
given(adminUserService.getUsers(
        isNull(), isNull(), isNull(), isNull(), eq("name"), any(Pageable.class)))
    .willReturn(new PageImpl<>(List.of(buildListResponse(1L, "ACTIVE"))));

mockMvc.perform(get("/api/v1/admin/users").param("sort", "name"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.content").isArray());

then(adminUserService).should()
    .getUsers(isNull(), isNull(), isNull(), isNull(), eq("name"), any(Pageable.class));
```

---

### N-05 — keyword=홍길동

- **Given:** ACTIVE + Redis 키 있는 사용자 1명
- **When:** `status=ACTIVE` & `loginStatus=ONLINE` & `sort=createdAt`
- **Then:** 조건 교집합만 반환, 정렬 적용

```java
given(adminUserService.getUsers(
        eq("홍길동"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
    .willReturn(new PageImpl<>(List.of(buildListResponse(1L, "ACTIVE"))));

mockMvc.perform(get("/api/v1/admin/users").param("keyword", "홍길동"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
    .andExpect(jsonPath("$.data.content[0].userId").value(1));

then(adminUserService).should()
    .getUsers(eq("홍길동"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
```

---

## O. AdminLogController — PATCH `/admin/logs/error/{id}`

**대상:** `AdminLogController.resolveErrorLog()`  
**파일 위치:** `src/test/java/com/service/domain/admin/controller/AdminLogControllerTest.java`

### 단위 테스트 범위 vs 통합 테스트 범위

| 검증 항목 | 단위 테스트 (현재) | 통합 테스트 (별도 필요) |
|-----------|:------------------:|:-----------------------:|
| HTTP 응답 코드 및 바디 포맷 | O | - |
| `resolvedAt` 직렬화 형식 (`LocalDateTime`) | O | - |
| `BusinessException` → HTTP 상태코드 매핑 | O | - |
| `log.resolve(memo)` dirty checking → DB 반영 | X | O |
| `resolved_at` 실제 DB 컬럼 반영 여부 | X | O |

> **O-01 핵심 (통합 테스트 관점)**  
> `log.resolve(memo)` 호출 후 dirty checking으로 `resolved_at`이 실제 DB에 반영되는지 확인.  
> 단위 테스트의 `shouldHaveNoMoreInteractions(save())` 검증과 연계되는 통합 검증 포인트.

### 시나리오 요약

| 시나리오 | 입력 | 기대 결과 | 검증 포인트 |
|----------|------|-----------|-------------|
| O-01 | 존재하는 `errorLogId=1` | HTTP 200, `resolvedYn=true`, `resolvedAt` 포함 | `resolvedAt`이 `"2026-06-15T10:00:00"` 형식으로 직렬화되는지 확인 (JacksonConfig) |
| O-02 | 존재하지 않는 `errorLogId=99` | HTTP 403, `error.code=ADMIN_001` | `findById` 실패 시 `USER_001`(404)이 아닌 `ADMIN_001`(403)을 던지는 설계 검증 |

---

### O-01 — 정상 해결 처리

- **Given:** `SystemErrorLog(resolvedYn=false, resolvedAt=null)`
- **When:** `PATCH /admin/logs/error/1` `{"resolvedYn":true, "resolvedMemo":"원인 파악 후 수정"}`
- **Then:** HTTP 200, `resolvedYn=true` / `resolvedAt≠null` DB 반영

```java
LocalDateTime resolvedAt = LocalDateTime.of(2026, 6, 15, 10, 0, 0);
ErrorLogResponse resolvedResponse = ErrorLogResponse.builder()
    .errorLogId(1L)
    .serviceName("service-backend")
    .errorLevel("ERROR")
    .errorMessage("Connection timeout")
    .resolvedYn(true)
    .resolvedAt(resolvedAt)
    .createdAt(LocalDateTime.of(2026, 6, 15, 9, 0, 0))
    .build();

given(adminLogService.resolveErrorLog(eq(1L), any())).willReturn(resolvedResponse);

mockMvc.perform(patch("/api/v1/admin/logs/error/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"resolvedYn\":true,\"resolvedMemo\":\"DB 연결 설정 수정\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data.errorLogId").value(1))
    .andExpect(jsonPath("$.data.resolvedYn").value(true))
    .andExpect(jsonPath("$.data.resolvedAt").value("2026-06-15T10:00:00"));
```

> **검증 핵심:** `resolvedAt` 필드가 `"2026-06-15T10:00:00"` 형식으로 직렬화되는지 확인한다.  
> `LocalDateTime` 직렬화는 `JacksonConfig` 설정에 따라 달라지므로 포맷 검증이 중요하다.

---

### O-02 — 에러 로그 없음 → HTTP 403 (ADMIN_001)

- **Given:** `id=999` 존재하지 않음
- **When:** `PATCH /admin/logs/error/99`
- **Then:** HTTP 403, `ADMIN_001`

```java
willThrow(new BusinessException(ErrorCode.ADMIN_001))
    .given(adminLogService)
    .resolveErrorLog(eq(99L), any());

mockMvc.perform(patch("/api/v1/admin/logs/error/99")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"resolvedYn\":true}"))
    .andExpect(status().isForbidden())
    .andExpect(jsonPath("$.success").value(false))
    .andExpect(jsonPath("$.error.code").value("ADMIN_001"));
```

**에러 처리 흐름**

```
AdminLogService.resolveErrorLog() → throw BusinessException(ADMIN_001)
  └─ GlobalExceptionHandler → ADMIN_001 → HTTP 403 Forbidden
```

> **O-02 주의**  
> 일반적으로 "없는 리소스 → 404"를 기대하지만, `AdminLogService.resolveErrorLog()`는  
> `BusinessException(ADMIN_001)`을 던지도록 구현되어 있다.  
> 테스트는 현재 코드의 실제 동작을 그대로 검증한다.

---

## P. AdminServiceHealthController — GET `/admin/services/health`

**대상:** `AdminServiceHealthController.getServicesHealth()`  
**파일 위치:** `src/test/java/com/service/domain/admin/controller/AdminServiceHealthControllerTest.java`

### 설계 의도

헬스 체크 API는 모니터링 대시보드용이므로 하위 서비스 장애를 HTTP 에러 코드로 표현하지 않는다.  
장애 여부는 응답 body의 `status` / `error` 필드로 전달하며, HTTP 상태코드는 항상 200을 유지한다.

> **P-02 핵심 (Graceful Degradation)**  
> 외부 서비스 장애가 전파되어 HTTP 500을 반환하면 안 된다.  
> 200 + DOWN 상태 표시가 목표.

### 시나리오 요약

| 시나리오 | 상황 | 기대 결과 | 검증 포인트 |
|----------|------|-----------|-------------|
| P-01 | `bank-server=DOWN`, 나머지=UP | HTTP 200 | 서비스 상태와 무관하게 HTTP 200 유지, 장애 정보는 `data[0].error`에 포함 |
| P-02 | 5개 서비스 모두 UP | HTTP 200, 목록 5개 | `data.length()=5`, 서비스명과 상태 순서 확인 |

---

### P-01 — 일부 서비스 DOWN → HTTP 200 유지

- **Given:** `bank-server` 응답 없음 (Mock 타임아웃)
- **When:** `GET /admin/services/health`
- **Then:** HTTP 200 유지, `bank-server status="DOWN"` (500 아님)

```java
List<ServiceHealthResponse> healthList = List.of(
    new ServiceHealthResponse("bank-server", "DOWN", "Connection refused"),
    new ServiceHealthResponse("stock-server", "UP", null),
    new ServiceHealthResponse("transaction-server", "UP", null),
    new ServiceHealthResponse("mydata-server", "UP", null),
    new ServiceHealthResponse("ai-server", "UP", null));

given(adminServiceHealthService.getAllServicesHealth()).willReturn(healthList);

mockMvc.perform(get("/api/v1/admin/services/health"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data[0].serviceName").value("bank-server"))
    .andExpect(jsonPath("$.data[0].status").value("DOWN"))
    .andExpect(jsonPath("$.data[0].error").value("Connection refused"))
    .andExpect(jsonPath("$.data[1].status").value("UP"));
```

---

### P-02 — 전체 서비스 UP → HTTP 200

- **Given:** 모든 의존 서비스 UP
- **When:** `GET /admin/services/health`
- **Then:** HTTP 200, 각 서비스 `status="UP"`

```java
List<ServiceHealthResponse> healthList = List.of(
    new ServiceHealthResponse("bank-server", "UP", null),
    new ServiceHealthResponse("stock-server", "UP", null),
    new ServiceHealthResponse("transaction-server", "UP", null),
    new ServiceHealthResponse("mydata-server", "UP", null),
    new ServiceHealthResponse("ai-server", "UP", null));

given(adminServiceHealthService.getAllServicesHealth()).willReturn(healthList);

mockMvc.perform(get("/api/v1/admin/services/health"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data").isArray())
    .andExpect(jsonPath("$.data.length()").value(5))
    .andExpect(jsonPath("$.data[0].serviceName").value("bank-server"))
    .andExpect(jsonPath("$.data[0].status").value("UP"))
    .andExpect(jsonPath("$.data[4].serviceName").value("ai-server"))
    .andExpect(jsonPath("$.data[4].status").value("UP"));
```

---

## 공통 테스트 구조 (@WebMvcTest)

| 구성 요소 | 역할 |
|-----------|------|
| `@WebMvcTest` | Spring MVC 레이어만 로드 (DB·Redis 없음) |
| `@MockBean` | `AdminXxxService`, `JwtProvider`, `AdminLogSaveService` 모킹 |
| `@WithMockUser(ADMIN)` | JwtFilter 우회, SecurityContext에 `ROLE_ADMIN` 주입 |

### MockBean 등록 이유

| MockBean | 이유 |
|----------|------|
| `AdminXxxService` | 컨트롤러 의존성 — 실제 서비스 로직 격리 |
| `JwtProvider` | SecurityConfig 빈 생성 시 필요 |
| `AdminLogSaveService` | GlobalExceptionHandler 또는 AOP 의존성 |

---

## 시나리오 우선순위 요약

| 우선순위 | 시나리오 | 이유 |
|:--------:|----------|------|
| ★★★ | M-01 | 트랜잭션 원자성 핵심, 이력서 소재 |
| ★★★ | N-04 | `buildSpec role=USER` 필터 실제 DB 검증 |
| ★★★ | O-01 | dirty checking DB 반영 검증 |
| ★★ | N-01 / N-02 | Redis ↔ DB 연동 흐름 |
| ★★ | P-02 | 장애 격리 설계 검증 (Graceful Degradation) |
| ★ | M-02 / M-03 / N-03 / N-05 / O-02 | 보조 케이스 |s