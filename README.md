# Internal Employee Portal

비트컴퓨터 사내 직원 관리 과제다. 직원은 자신의 정보를 조회하고 이름·이메일·전화번호를 수정한다. 관리자는 직원 계정·재직 상태·Background Check를 관리한다.

## 기술 스택

- Vue 3, TypeScript, Vite, Vue Router, Axios, Tailwind CSS
- Java 21, Spring Boot, Spring Security Session, Spring Data JPA, Bean Validation
- PostgreSQL 16
- 외부 Background Check API: `docs/background-check-swagger.yaml`

## 로컬 실행

필요 조건: Node.js 22+, Tailscale VPN 연결, 프로젝트 `.tools` 아래 JDK 21.

1. `.env.example`을 복사해 `.env`를 만들고 실제 계정 값을 입력한다.
2. 백엔드를 실행한다.

```powershell
.\scripts\run-backend.ps1
```

3. 다른 터미널에서 프런트엔드를 실행한다.

```powershell
cd frontend
npm install
npm run dev
```

4. `http://localhost:5173`에 접속한다.

PostgreSQL은 Tailscale VPN의 `100.92.167.33:5432`로 직접 연결한다. SSH DB 터널은 사용하지 않는다.

## 환경변수

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `BACKGROUND_CHECK_API_URL`
- `SEED_ENABLED`
- `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD`
- `SEED_EMPLOYEE_EMAIL`, `SEED_EMPLOYEE_PASSWORD`
- `SEED_TERMINATED_EMAIL`, `SEED_TERMINATED_PASSWORD`

실제 계정과 비밀번호는 Git에서 제외된 루트 `.env`에만 둔다. Public 저장소에는 `.env.example`만 포함한다.

## Seed 계정

`SEED_ENABLED=true`이면 `.env`에 설정한 관리자, 재직 직원, 퇴사 직원 계정을 최초 1회 생성한다. 로그인 정보는 `.env` 값을 사용한다.

## 권한과 보안

- 비밀번호는 BCrypt Hash로 저장한다.
- 인증은 HttpOnly/SameSite Session Cookie와 CSRF Token을 사용한다.
- `/api/admin/**`는 서버에서 `ADMIN` Role을 검사한다.
- `/api/me`는 URL에서 직원 ID를 받지 않아 다른 직원 데이터에 접근할 수 없다.
- 모든 보호 요청에서 DB의 최신 Role과 재직 상태를 다시 검사한다.
- 퇴사 직원은 신규 로그인과 기존 Session 요청이 즉시 차단된다.
- 관리자 계정은 UI와 서버 양쪽에서 퇴사 처리를 금지한다.
- 프런트 HTML Validation과 백엔드 Bean Validation을 모두 적용한다.
- 부서와 직급은 `organization_codes` 코드 테이블의 등록값만 사용한다.
- Vue 텍스트 이스케이프, 허용목록 입력 검증, CSRF, DB 파라미터 바인딩으로 XSS·인젝션을 차단한다.
- 배포 Nginx는 CSP, frame 차단, `nosniff` 보안 헤더를 적용한다.

## Background Check

프런트엔드는 외부 API를 직접 호출하지 않는다.

```text
Vue → Spring Boot → External Background Check API
```

한글 이름은 요청 직전에 성/이름으로 분리하며 복성을 지원한다. 관리자는 분리 결과를 수정할 수 있다. History와 상세 결과는 외부 API를 Source of Truth로 사용하고 내부 DB에 복제하지 않는다.

외부 API에는 연결·응답 Timeout을 적용한다. 404, 500, 503을 내부 오류 형식으로 변환한다. `503 retryAfter`와 일시적인 502/503은 프런트에서 최대 3회 재조회하며 무한 Polling하지 않는다.

## 주요 API

- `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/csrf`
- `GET/PATCH /api/me`
- `GET /api/organization-codes/departments`, `GET /api/organization-codes/positions`
- `GET/POST /api/admin/employees`
- `GET/PATCH /api/admin/employees/{id}`
- `POST /api/admin/employees/{id}/terminate`
- `POST/GET /api/admin/employees/{id}/background-checks`
- `GET /api/admin/background-checks/{checkId}`
- Swagger UI: `http://127.0.0.1:8081/swagger-ui.html`

## 테스트

```powershell
cd backend
$env:JAVA_HOME = (Get-ChildItem '..\.tools' -Directory -Filter 'jdk-21*' | Select-Object -First 1).FullName
.\gradlew.bat test

cd ..\frontend
npm run build
```

핵심 테스트는 Role 차단, 관리자 퇴사 금지, 퇴사자 로그인·기존 Session 차단, 입력 검증, 한글 이름 분리, 외부 API 응답·503 `retryAfter` 변환을 다룬다.

## OCI 배포 구조

OCI 서버 `/home/ubuntu/app/202608_bitComputer`에 배포한다.

- 웹: `https://www.jujeop.com/bitComProject/`
- API: `https://www.jujeop.com/bitComProject/api/`
- 프런트 정적 파일: `/var/www/bitcomputer/bitComProject`
- 백엔드 PM2 프로세스: `bitcomputer-api` (포트 8081)

서버의 프로젝트 루트에 Git에 포함되지 않는 `.env`를 만들고 권한을 제한한다.

```sh
cd /home/ubuntu/app/202608_bitComputer
chmod 600 .env
./deploy.sh
```

`deploy.sh`는 `git pull --ff-only`, 프런트 빌드/정적 파일 배포, Spring Boot JAR 빌드, PM2 재시작, API 헬스체크와 `pm2 save`를 실행한다. Windows CRLF 형식의 `.env`도 처리한다.

Nginx는 `infra/nginx/bitcomputer.conf`의 location 블록을 `jujeop.com` HTTPS 서버 블록에 포함해야 한다. 배포 후 확인 명령은 다음과 같다.

```sh
pm2 status bitcomputer-api
curl -fsS http://127.0.0.1:8081/api/auth/csrf
curl -I https://www.jujeop.com/bitComProject/
```

## AI 활용

Codex를 요구사항 분석, 최소 구조 설계, CRUD·권한·외부 API 구현, 입력 검증, 테스트 작성과 브라우저 검증에 사용했다.
