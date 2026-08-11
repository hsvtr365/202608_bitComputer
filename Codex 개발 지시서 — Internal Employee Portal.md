# Codex 개발 지시서 — Internal Employee Portal

비트컴퓨터 채용 과제로 제출할 사내 직원 관리 시스템(Internal Employee Portal)을 구현한다.

평가자가 완성된 애플리케이션을 직접 사용하면서 기능을 확인한다.

목표는 기능을 과도하게 확장하는 것이 아니라 다음을 안정적으로 구현하는 것이다.

- 로그인
- 직원 자신의 정보 조회/수정
- 관리자 직원 관리
- Role 기반 권한 제어
- 퇴사 직원 즉시 접근 차단
- 외부 Background Check API 연동
- 외부 API 지연/오류 대응
- 기본적인 기업용 UI
- Oracle Cloud 배포

불필요한 Microservice, Kubernetes, Message Queue, 별도 Background Job 시스템 등은 사용하지 않는다.

---

# 1. 기술 스택

## Frontend

- Vue 3
- TypeScript
- Vite
- Vue Router
- Axios
- Tailwind CSS

Vue Composition API와 `<script setup lang="ts">`를 사용한다.

전역 상태관리 라이브러리는 기본적으로 사용하지 않는다.

로그인 사용자 정보처럼 필요한 상태는 Vue composable 또는 최소한의 상태 관리로 구현한다.

---

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- Bean Validation
- Spring WebClient
- Springdoc OpenAPI

Build Tool:

- Gradle

---

## Database

- PostgreSQL

직원, 로그인 정보 및 권한 관리에 사용한다.

Background Check 결과를 별도 내부 테이블에 복제 저장하지 않는다.

Background Check History와 상세 결과는 제공된 외부 API를 Source of Truth로 사용한다.

---

## Cloud

Oracle Cloud Infrastructure(OCI)를 사용한다.

단순한 구조로 배포한다.

```text
Internet
   ↓
OCI Compute
   ↓
Nginx
   ├─ Vue 정적 파일
   └─ /api → Spring Boot
                 ↓
             PostgreSQL

Spring Boot
   ↓
External Background Check API
```

Docker는 개발/배포 편의를 위해 필요할 경우에만 사용한다.

GitHub Actions 등의 CI/CD는 과제 필수 범위에서 제외한다.

---

# 2. 사용자 유형

두 가지 Role만 사용한다.

```text
EMPLOYEE
ADMIN
```

직원 상태:

```text
ACTIVE
TERMINATED
```

---

# 3. 주요 화면

필수 화면만 구현한다.

```text
/login

/employee/profile

/admin/employees
/admin/employees/new
/admin/employees/:id
/admin/employees/:id/background-checks
```

별도의 복잡한 Dashboard나 통계 화면은 필수가 아니다.

ADMIN 로그인 시 직원 목록으로 이동해도 된다.

---

# 4. 로그인

Spring Security 기반 Session Authentication을 사용한다.

JWT는 사용하지 않는다.

로그인 ID는 email을 사용한다.

비밀번호는 BCrypt 등 Spring Security PasswordEncoder를 이용해 Hash 상태로 저장한다.

Cookie에는 적절한 보안 설정을 적용한다.

```text
HttpOnly
SameSite
Secure (운영 HTTPS 환경)
```

---

# 5. Employee 모델

Employee 하나의 Entity로 계정과 직원 정보를 관리한다.

과제 규모에서 Account Entity를 별도로 분리하지 않는다.

예:

```text
Employee

id
employeeNumber
name
email
passwordHash
phone
dateOfBirth
department
position
role
status
hireDate
terminationDate
createdAt
updatedAt
```

Constraint:

```text
employeeNumber UNIQUE
email UNIQUE
```

내부 직원의 한글 이름은 하나의 `name` 필드로 저장한다.

예:

```text
김민준
이서연
남궁민수
```

DB에서 firstName / lastName을 별도 필드로 저장하지 않는다.

---

# 6. 직원 기능

EMPLOYEE는 자신의 정보만 조회할 수 있다.

```text
GET /api/me
```

자신의 수정 가능한 정보만 수정할 수 있다.

```text
PATCH /api/me
```

수정 가능 필드 예:

```text
phone
```

관리자가 관리해야 하는 다음 필드는 EMPLOYEE가 수정할 수 없다.

```text
employeeNumber
name
role
status
department
position
hireDate
terminationDate
```

필요하면 이메일 수정 여부는 구현 단계에서 정책을 정한다.

PATCH Request DTO에 수정 가능한 필드만 정의한다.

Mass Assignment가 발생하지 않도록 한다.

---

# 7. 관리자 기능

ADMIN은 다음 기능을 사용할 수 있다.

```text
신규 직원 및 로그인 계정 생성
전체 직원 목록 조회
직원 상세 조회
직원 정보 수정
퇴사 처리
Background Check 실행
Background Check History 조회
Background Check 상세 결과 조회
```

내부 API:

```text
GET   /api/admin/employees
POST  /api/admin/employees

GET   /api/admin/employees/{employeeId}
PATCH /api/admin/employees/{employeeId}

POST  /api/admin/employees/{employeeId}/terminate
```

ADMIN이 아닌 사용자가 `/api/admin/**`를 호출하면 서버에서 403을 반환한다.

UI에서 메뉴만 숨기는 것으로 권한 처리를 끝내지 않는다.

---

# 8. 직원 생성

관리자가 다음 정보를 입력해 직원을 생성한다.

```text
사번
한글 이름
이메일
초기 비밀번호
생년월일
전화번호
부서
직급
Role
입사일
```

생성 시 기본 재직 상태:

```text
ACTIVE
```

초기 비밀번호는 Hash 후 저장한다.

비밀번호 초기화, 이메일 인증, 초대 이메일 등의 기능은 이번 과제 범위에서 제외한다.

---

# 9. 퇴사자 접근 차단

과제 핵심 요구사항이다.

관리자가 퇴사 처리하면:

```text
status = TERMINATED
terminationDate = 현재 시각
```

으로 변경한다.

퇴사 직원은 다음이 불가능해야 한다.

```text
신규 로그인
Employee Portal 접근
/api/me 접근
기존 로그인 세션을 이용한 API 접근
```

복잡한 Session 저장소를 별도로 구축하지 않는다.

대신 모든 보호된 요청에서 현재 로그인한 Employee의 최신 상태를 DB에서 검증한다.

```text
인증 여부 확인
    ↓
Employee 조회
    ↓
status == ACTIVE 확인
    ↓
Role 확인
    ↓
요청 처리
```

따라서 사용자가 퇴사 처리된 이후 기존 Session Cookie를 가지고 있어도 다음 요청부터 즉시 차단된다.

가능하면 퇴사 처리 시 현재 세션도 만료시키되, 핵심 보안은 매 요청 시 ACTIVE 상태 검증으로 보장한다.

---

# 10. 직원 자신의 데이터만 접근

EMPLOYEE API는 employeeId를 클라이언트로부터 받지 않는다.

```text
GET /api/me
PATCH /api/me
```

현재 Spring Security 인증 사용자에서 Employee를 식별한다.

따라서 다른 직원 ID를 URL에 넣어서 데이터를 가져오는 IDOR 구조 자체를 만들지 않는다.

---

# 11. Background Check API

제공된 Swagger YAML을 정확한 API 계약으로 사용한다.

외부 API URL은 환경변수로 설정한다.

```text
BACKGROUND_CHECK_API_URL
```

Frontend에서 외부 Background Check API를 직접 호출하지 않는다.

항상 Spring Boot Backend를 경유한다.

```text
Vue
 ↓
Spring Boot
 ↓
BackgroundCheckClient
 ↓
External Background Check API
```

API Key 등 외부 API 인증정보가 존재한다면 Backend 환경변수로만 관리한다.

---

# 12. Background Check 생성

외부 API:

```text
POST /background-checks
```

Request:

```json
{
  "employeeId": "EMP-2024-001",
  "firstName": "민준",
  "lastName": "김",
  "dateOfBirth": "1990-03-15"
}
```

필수값:

```text
employeeId
firstName
lastName
dateOfBirth
```

HTTP 성공:

```text
201 Created
```

status:

```text
pending
clear
flagged
```

잘못된 요청:

```text
400 Bad Request
```

외부 API 오류를 내부 API의 적절한 오류 응답으로 변환한다.

---

# 13. 한글 이름 → firstName / lastName 변환

사내 DB에는:

```text
name = 김민준
```

하나만 저장한다.

외부 Background Check 요청 직전에 변환한다.

```text
김민준

lastName = 김
firstName = 민준
```

한국의 복성도 최소한 고려한다.

예:

```text
남궁
황보
제갈
선우
사공
서문
독고
```

예:

```text
남궁민수

lastName = 남궁
firstName = 민수
```

`KoreanNameMapper` 정도의 작은 독립 클래스로 구현한다.

과도한 이름 분석 시스템은 만들지 않는다.

Background Check 실행 화면에서는 자동 분리 결과를 관리자에게 보여주고 필요한 경우 수정할 수 있게 한다.

이는 외부 API 요청용 값일 뿐 Employee의 원본 `name`은 변경하지 않는다.

---

# 14. Background Check 상태

Background Check 생성 결과는 바로 완료될 수도 있고:

```text
clear
flagged
```

아직 처리 중일 수도 있다.

```text
pending
```

pending인 경우 Swagger에 정의된 다음 API로 상태를 조회한다.

```text
GET /background-checks/{checkId}
```

---

# 15. Polling 구조

Spring Boot 서버 내부에서 오래 대기하는 Polling Job을 만들지 않는다.

구조는 다음과 같이 단순하게 한다.

```text
Vue
 ↓
우리 Backend의 Background Check 상태 API 호출
 ↓
Spring Boot
 ↓
외부 GET /background-checks/{checkId} 1회 호출
 ↓
결과 반환
```

Vue는 status가 `pending`인 경우 일정 간격 후 다시 우리 Backend를 호출한다.

예:

```text
3~5초 간격
```

최대 Polling 시간 또는 최대 횟수를 둔다.

무한 Polling은 하지 않는다.

최대 시간을 초과해도 Background Check 자체가 실패했다고 판단하지 않고:

```text
"처리 중입니다. 잠시 후 다시 확인해 주세요."
```

상태로 종료한다.

사용자가 나중에 다시 상세 화면을 열면 상태를 다시 조회할 수 있어야 한다.

---

# 16. Background Check History

Swagger에서 제공하는:

```text
GET /background-checks?employeeId={employeeId}
```

API를 사용한다.

따라서 Background Check History를 PostgreSQL에 중복 저장하지 않는다.

관리자 화면:

```text
Background Check History

상태
요청 일시
완료 일시
```

각 항목을 선택하면:

```text
GET /background-checks/{checkId}
```

를 Backend를 통해 호출해 상세 결과를 가져온다.

---

# 17. Background Check 상세

완료된 결과에 다음 필드가 존재할 수 있다.

```text
criminalRecord
educationVerified
employmentVerified
creditScore
```

creditScore:

```text
excellent
good
fair
poor
```

pending 상태에서는 완료 결과 필드가 null 또는 존재하지 않을 수 있으므로 이를 정상적으로 처리한다.

시간값은 외부 API가 제공하는 UTC timestamp를 그대로 계약상 받아들이고, UI 표시 시 필요하면 사용자 로컬 시간으로 변환한다.

---

# 18. Background Check 권한

다음 기능은 ADMIN만 사용할 수 있다.

```text
Background Check 실행
History 조회
상세 결과 조회
```

EMPLOYEE에게는 다음 정보를 반환하지 않는다.

```text
criminalRecord
educationVerified
employmentVerified
creditScore
Background Check History
```

Backend에서 Role을 검증한다.

---

# 19. 외부 API 장애 대응

이메일 요구사항에 따라 외부 Background Check API의 지연 및 오류를 고려한다.

필수 구현:

```text
Connection Timeout
Read Timeout
500 처리
503 처리
retryAfter 처리
404 처리
사용자 친화적인 오류 표시
```

별도의 Resilience4j 의존성은 필수가 아니다.

Spring WebClient만으로 단순하고 명확하게 구현할 수 있다면 그대로 구현한다.

---

# 20. 503 처리

외부 API의 503 응답에는 다음 값이 올 수 있다.

```json
{
  "error": "Service Unavailable",
  "message": "...",
  "retryAfter": 30,
  "statusCode": 503
}
```

`retryAfter`는 초 단위다.

서버 Thread를 30초 동안 대기시키지 않는다.

우리 Backend는 retryAfter 정보를 Frontend에 전달할 수 있는 형태로 변환한다.

Frontend는 해당 시간 이후 상태 조회를 다시 시도한다.

예:

```json
{
  "code": "BACKGROUND_CHECK_UNAVAILABLE",
  "message": "Background Check 서비스가 일시적으로 사용할 수 없습니다.",
  "retryAfter": 30
}
```

무한 Retry 금지.

---

# 21. 500 처리

500 오류에 대해 공격적인 자동 Retry를 구현하지 않는다.

필요하면 짧은 Retry를 최대 1~2회 정도만 적용한다.

구현 복잡성이 증가한다면 자동 Retry 없이 사용자에게 재시도 UI를 제공해도 된다.

중요한 것은:

```text
무한 Retry 없음
Timeout 있음
UI 무한 Loading 없음
외부 Stack Trace 노출 없음
```

이다.

---

# 22. 404 처리

다음 API:

```text
GET /background-checks/{checkId}
```

는 존재하지 않는 checkId에 대해 404를 반환할 수 있다.

이를 정상적인 Application Error로 변환한다.

예:

```text
BACKGROUND_CHECK_NOT_FOUND
```

---

# 23. Backend Background Check 구조

필요 이상의 계층을 만들지 않는다.

다음 정도로 제한한다.

```text
BackgroundCheckController
BackgroundCheckService
BackgroundCheckClient
KoreanNameMapper
DTO
```

Controller에서 직접 WebClient를 호출하지 않는다.

외부 API DTO와 내부 Employee Entity는 분리한다.

---

# 24. 내부 Background Check API

예:

```text
POST
/api/admin/employees/{employeeId}/background-checks
```

Background Check 생성.

```text
GET
/api/admin/employees/{employeeId}/background-checks
```

직원 History 조회.

```text
GET
/api/admin/background-checks/{checkId}
```

특정 Background Check 상세/현재 상태 조회.

모든 API는 ADMIN Role을 요구한다.

---

# 25. Error Response

Backend API 오류 응답은 단순하게 통일한다.

예:

```json
{
  "code": "EMPLOYEE_NOT_FOUND",
  "message": "직원을 찾을 수 없습니다."
}
```

필요하면:

```text
timestamp
```

정도만 추가한다.

복잡한 Error Framework는 만들지 않는다.

GlobalExceptionHandler 하나로 관리한다.

---

# 26. Validation

Frontend와 Backend 모두 기본 Validation을 한다.

Backend Validation이 최종 기준이다.

확인할 항목:

```text
필수값
Email 형식
중복 사번
중복 Email
생년월일 형식
허용되지 않은 Role
허용되지 않은 수정 필드
```

DB에도 필요한 UNIQUE / NOT NULL Constraint를 적용한다.

---

# 27. UI

화려한 디자인보다 사용성을 우선한다.

기업용 사내 시스템 형태의 깔끔한 UI로 구현한다.

## Login

```text
Email
Password
Login
```

## Employee

```text
내 정보
정보 수정
Logout
```

## Admin

```text
직원 목록
직원 생성
직원 상세
직원 수정
퇴사 처리
Background Check
Logout
```

---

# 28. 직원 목록

표시 정보:

```text
사번
이름
부서
직급
이메일
재직 상태
입사일
```

검색은 이름 또는 사번 정도만 지원하면 충분하다.

대량 데이터용 복잡한 Search Engine은 만들지 않는다.

Pagination도 데이터 규모상 필수가 아니다.

---

# 29. 직원 상세

다음 정보를 표시한다.

```text
기본정보
근무정보
계정/재직 상태
Background Check
```

ACTIVE / TERMINATED 상태를 명확하게 표시한다.

퇴사 처리 버튼에는 Confirmation을 둔다.

---

# 30. 테스트

모든 코드를 테스트하려 하지 않는다.

과제 핵심 비즈니스 로직만 우선 테스트한다.

Backend 테스트:

```text
로그인 성공/실패

EMPLOYEE가 /api/admin/** 접근 → 403

EMPLOYEE가 자신의 정보 조회

퇴사 사용자의 로그인 차단

기존 Session을 가진 사용자가 퇴사 처리 후 API 접근 → 차단

직원 생성

중복 사번/이메일 차단

KoreanNameMapper

Background Check 요청 Mapping

pending / clear / flagged 처리

404 처리

503 retryAfter Mapping
```

JUnit 5 + Spring Boot Test / Mockito를 적절하게 사용한다.

Frontend는 핵심 기능 구현이 끝난 뒤 시간이 남으면 최소 테스트를 작성한다.

---

# 31. Seed Data

평가자가 실행 직후 기능을 테스트할 수 있도록 테스트 데이터를 제공한다.

예:

```text
ADMIN
admin@bitcomputer.test

ACTIVE EMPLOYEE
minjun@bitcomputer.test

TERMINATED EMPLOYEE
terminated@bitcomputer.test
```

비밀번호는 README에 명시한다.

한국 이름 데이터를 사용한다.

```text
김민준
이서연
남궁민수
```

---

# 32. README

README에는 다음만 명확하게 정리한다.

```text
프로젝트 개요
기술 스택
실행 방법
환경변수
PostgreSQL 설정
Seed 계정
권한 구조
퇴사자 접근 차단 방식
Background Check 연동 구조
외부 API 오류 대응
Oracle Cloud 배포 구조
AI 활용 방식
테스트 실행 방법
```

과도한 문서 작업은 하지 않는다.

---

# 33. AI 활용

본 과제는 AI 활용을 요구한다.

README에 실제 개발 과정에서 사용한 AI 활용 내역을 간단히 기록한다.

예:

```text
Codex
- 요구사항 분석
- 프로젝트 초기 구조 작성
- 반복적인 CRUD 구현 지원
- Vue UI 구현 지원
- Swagger 기반 API DTO/Client 구현 지원
- Test Case 작성 지원
- 코드 리뷰 및 Refactoring
```

실제로 사용하지 않은 AI 활용 사례를 허위로 추가하지 않는다.

애플리케이션 자체에 생성형 AI 기능을 억지로 넣을 필요는 없다.

---

# 34. 프로젝트 구조

단순 Monorepo.

```text
employee-portal/

frontend/
  src/
    api/
    components/
    composables/
    router/
    types/
    views/

backend/
  src/main/java/.../
    config/
    security/
    controller/
    service/
    repository/
    domain/
    dto/
    exception/
    integration/backgroundcheck/

  src/test/

infra/
  nginx/

README.md
```

필요 없는 계층과 추상화는 추가하지 않는다.

---

# 35. 구현하지 않을 것

다음은 이번 과제 범위에서 제외한다.

```text
Microservices
Kubernetes
Kafka / Message Queue
Redis
별도 Session DB
별도 Background Worker
Background Check 결과 복제 DB
Event Sourcing
CQRS
GraphQL
Elasticsearch
복잡한 Audit 시스템
복잡한 Dashboard 통계
CI/CD 구축
Refresh Token 시스템
OAuth / SSO
메일 인증
비밀번호 찾기
초대 이메일
파일 업로드
```

과제 요구사항에 필요한 경우가 새롭게 확인되지 않는 한 추가하지 않는다.

---

# 36. 보안 체크

다음 시나리오는 반드시 차단되어야 한다.

```text
EMPLOYEE가 /api/admin/** 직접 호출

EMPLOYEE가 Frontend에서 Role 값을 조작

EMPLOYEE가 PATCH /api/me에 role=ADMIN 전달

EMPLOYEE가 status=ACTIVE 전달

퇴사 직원이 다시 로그인

퇴사 직원이 기존 Session Cookie 사용

비로그인 사용자가 직원 데이터 API 접근
```

서버가 최종 권한 검증 주체다.

---

# 37. 완료 기준

다음 시나리오가 실제로 동작하면 완료로 판단한다.

## 관리자

```text
로그인
→ 직원 목록
→ 신규 직원 생성
→ 직원 상세 확인
```

## 직원

```text
로그인
→ 자신의 정보 확인
→ 허용된 정보 수정
```

## 권한

```text
EMPLOYEE
→ Admin API 직접 호출
→ 403
```

## 퇴사

```text
ADMIN
→ ACTIVE 직원 퇴사 처리

해당 직원
→ 기존 Session으로 API 요청
→ 차단

재로그인
→ 차단
```

## Background Check

```text
ADMIN
→ 직원 상세
→ Background Check 실행

clear/flagged
→ 결과 표시

pending
→ 일정 시간 후 상태 재조회
```

## History

```text
ADMIN
→ 직원별 Background Check History 조회
→ checkId 선택
→ 상세 결과 조회
```

## 외부 API 장애

```text
503
→ retryAfter 기반 안내/재조회

404
→ 결과 없음 표시

500/Timeout
→ 무한 Loading 없이 오류 표시
```

---

# 38. 구현 우선순위

## Phase 1

```text
Spring Boot / Vue 프로젝트 생성
PostgreSQL 연결
Employee Entity
Seed Data
Spring Security 로그인
```

## Phase 2

```text
Employee 자기 정보 조회/수정
```

## Phase 3

```text
Admin 직원 목록
직원 상세
직원 생성
직원 수정
퇴사 처리
```

## Phase 4

```text
Role 검증
퇴사자 즉시 차단
보안 테스트
```

## Phase 5

```text
Background Check API Client
이름 Mapping
Background Check 생성
상세 조회
History
Frontend Polling
503 retryAfter
Timeout/Error 처리
```

## Phase 6

```text
UI 정리
Validation
테스트
Swagger
README
OCI 배포
```

---

# 39. Codex 작업 지침

한 번에 전체 애플리케이션을 생성하지 않는다.

먼저 현재 Repository 상태를 확인한다.

그 후:

1. 요구사항 분석
2. 최소 Architecture 제안
3. Employee DB Schema 제안
4. REST API 목록 제안
5. Spring Security 구조 제안
6. Background Check 연동 흐름 제안
7. 구현 순서 제안

을 먼저 작성한다.

설계 시 항상 다음 기준으로 판단한다.

```text
"이 설계가 과제 요구사항을 만족하기 위해 실제 필요한가?"
```

아니라면 추가하지 않는다.

설계 완료 후 Phase 단위로 구현한다.

각 Phase마다:

```text
구현
→ Build
→ Test
→ 오류 수정
→ 다음 Phase
```

순서로 진행한다.

마지막에는 전체 기능을 다시 점검하고 특히 다음 세 가지를 검증한다.

```text
권한 우회 가능 여부
퇴사자 기존 Session 접근 가능 여부
Background Check API 장애 시 애플리케이션 동작
```