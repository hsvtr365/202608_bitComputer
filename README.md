# Internal Employee Portal

비트컴퓨터 사내 직원 관리 과제다. 직원은 자신의 정보만 보고 전화번호를 수정한다. 관리자는 직원 생성·조회·수정·퇴사 처리와 Background Check를 수행한다.

## 기술

- Vue 3, TypeScript, Vite, Tailwind CSS
- Java 21, Spring Boot, Spring Security Session, Spring Data JPA
- PostgreSQL 16
- 외부 Background Check API: 제공된 Swagger 계약 사용

외부 계약 원본은 `docs/background-check-swagger.yaml`에 보관한다.
화면 디자인은 `design.md`의 모노스페이스·크림 캔버스·ASCII UI 규칙을 따른다.

## 로컬 실행

필요 조건: Node.js 22+, SSH `ubuntu` 접속 설정. JDK 21은 이 PC의 Git 제외 `.tools` 폴더에 준비되어 있다.

1. OCI DB SSH 터널 실행:

```powershell
.\scripts\db-tunnel.ps1
```

2. 새 터미널에서 Backend 실행:

```powershell
.\scripts\run-backend.ps1
```

3. 새 터미널에서 Frontend 실행:

```powershell
cd frontend
npm install
npm run dev
```

4. `http://localhost:5173` 접속.

실제 계정과 DB 비밀번호는 Git에서 제외된 루트 `.env`에 있다. Public 저장소에는 `.env.example`만 커밋한다.

## 테스트

```powershell
cd backend
$env:JAVA_HOME = (Get-ChildItem '..\.tools' -Directory -Filter 'jdk-21*' | Select-Object -First 1).FullName
.\gradlew.bat test

cd ..\frontend
npm run build
```

핵심 자동 테스트: Role 차단, 퇴사자 로그인 차단, 기존 Session 즉시 차단, 한글 이름 분리, 외부 API 생성·503 `retryAfter` 변환.

## API

- `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/csrf`
- `GET/PATCH /api/me`
- `GET/POST /api/admin/employees`
- `GET/PATCH /api/admin/employees/{id}`
- `POST /api/admin/employees/{id}/terminate`
- `POST/GET /api/admin/employees/{id}/background-checks`
- `GET /api/admin/background-checks/{checkId}`
- Swagger UI: `http://127.0.0.1:8081/swagger-ui.html`

## 보안

- BCrypt 비밀번호 Hash
- HttpOnly/SameSite Session Cookie와 CSRF Token
- `/api/admin/**` 서버 Role 검증
- `/api/me`는 URL에서 직원 ID를 받지 않음
- 보호 요청마다 DB의 최신 Role/재직 상태 재조회
- 퇴사 후 기존 Session도 다음 요청부터 차단
- 외부 API와 DB 비밀값은 Backend `.env`에서만 사용
- Background Check 결과는 내부 DB에 복제하지 않음

## Background Check

한글 원본 이름은 바꾸지 않는다. 요청 직전에 성/이름을 분리하고 관리자가 수정한 뒤 실행할 수 있다. `pending`은 Frontend가 최대 15회, 기본 4초 간격으로 조회한다. 503은 `retryAfter`를 사용한다. 무한 재시도와 서버 Thread 대기는 없다.

## OCI 배포 계획

아직 배포하지 않았다. 배포 시 Spring Boot는 기존 서비스와 겹치지 않는 `127.0.0.1:8081`, Vue는 `/bitComProject/`, API는 `/bitComProject/api/`를 사용한다. 예시는 `infra/nginx/bitcomputer.conf`에 있다.

## AI 활용

Codex로 요구사항 분석, 최소 구조 설계, CRUD·권한·외부 API 구현, 테스트 작성과 빌드 검증을 수행했다.
