# 🚀 Passfail: 온라인 코딩 테스트 및 배틀 플랫폼

**Passfail**은 개발자들이 코딩 문제를 풀고, 실시간으로 경쟁하며, AI 기반의 피드백을 통해 성장할 수 있는 통합 코딩 플랫폼입니다.

---

## 🌟 주요 기능

### 1. 💻 온라인 코딩 환경 및 채점 시스템
- **Monaco Editor**: VS Code 기반의 강력한 웹 에디터 인터페이스 제공.
- **Java 샌드박스 실행**: 로컬 프로세스를 격리하여 안전하고 빠른 코드 컴파일 및 실행.
- **실시간 채점**: 공식 테스트 케이스 및 커스텀 테스트 케이스를 통한 즉각적인 결과 확인.
- **상태 코드**: CORRECT, WRONG, TIMEOUT, RUNTIME_ERROR, COMPILE_ERROR 등 상세 피드백 제공.

### 2. 🤖 AI 통합 서비스
- **AI 코드 리뷰**: 제출한 코드에 대해 개선 방향, 시간 복잡도 분석 등 피드백 제공.
- **문제 해설 및 질문**: AI를 통한 문제 이해 및 학습 보조 기능.

### 3. ⚔️ 경쟁 및 게이미피케이션 (Battle Mode)
- **로직 메이즈(Logic Maze)**: 실시간 웹소켓 기반의 경쟁 코딩 모드.
- **티어 및 랭킹**: 점수에 따른 등급(Tier) 부여 및 매일 자정 갱신되는 랭킹 시스템.
- **포인트 시스템**: 문제 해결 및 활동을 통해 획득한 포인트로 스토어 이용 가능.

### 4. 🔐 스마트 인증 및 회원 관리
- **소셜 로그인**: Google, Kakao, Naver, Github 연동 및 자동 계정 통합.
- **보안**: Spring Security 기반의 권한 관리 및 이메일 기반 비밀번호 재설정.
- **마이페이지**: 상세 활동 내역(해결한 문제, 제출 기록), 포인트, 알림 통합 관리.

### 5. 💬 커뮤니티 및 부가 기능
- **게시판**: 개발 지식 공유 및 자유로운 소통 공간.
- **PDF 생성**: 성적표 또는 문제 데이터의 PDF 내보내기 기능.
- **알림 시스템**: 댓글, 결과, 공지 등 실시간 알림 제공.

---

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.3.4
- **Language**: Java 21
- **Security**: Spring Security 6 (OAuth2, JWT)
- **Data**: Spring Data JPA, Hibernate
- **Database**: MySQL (Runtime), Redis (Cache/Session)
- **Communication**: WebSocket, Spring Mail, WebFlux (AI API)

### Frontend
- **Templating**: Thymeleaf
- **Styling**: Vanilla CSS (Glass-morphism UI), Pretendard Font
- **Scripting**: Vanilla JavaScript
- **Editor**: Monaco Editor

### Tools & Others
- **Build**: Gradle
- **PDF**: iText PDF 8
- **AWS**: Spring Cloud AWS

---

## 📂 프로젝트 구조

```text
src/main/java/com/passfail/
├── admin/          # 관리자 전용 기능 (문제/회원 관리)
├── ai/             # AI 연동 및 프롬프트 처리
├── auth/           # 인증 및 소셜 로그인 로직
├── battle/         # 실시간 경쟁 모드 (WebSocket 기반)
├── codingtest/     # 코드 에디터 및 샌드박스 실행 엔진
├── config/         # 보안, Redis, WebSocket 등 설정
├── entity/         # JPA 도메인 모델
├── member/         # 회원 관리 및 마이페이지
├── problem/        # 코딩 테스트 문제 데이터 관리
├── ranking/        # 랭킹 배치 및 티어 산출
└── payment/        # 포인트 및 결제 시스템 (확장 예정)
```

---

## 🚀 시작하기

### 요구 사항
- Java 21 이상
- MySQL / MariaDB
- Redis (선택 사항, 세션 및 캐시 용도)

### 설치 및 실행
1. 저장소 클론:
   ```bash
   git clone https://github.com/your-repo/passfail.git
   ```
2. `application-local.yml` 또는 `application.properties` 설정 (DB 정보, OAuth2 클라이언트 키 등).
3. 프로젝트 빌드:
   ```bash
   ./gradlew build
   ```
4. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun
   ```

---

## 🛠 Troubleshooting (기술적 도전과 해결)

프로젝트 개발 과정에서 직면했던 주요 문제들과 이를 해결하기 위한 기술적 노력입니다.

### 1. 🔐 로그인 및 인증 (Login & Auth)
*   **문제**: 소셜 로그인 후 한글 닉네임을 가진 사용자가 마이페이지로 이동할 때 `IllegalArgumentException` 또는 경로 탐색 오류 발생.
*   **해결**: 리다이렉트 URL에 사용자 닉네임이 포함되는 경우, `URLEncoder`를 사용하여 UTF-8 인코딩을 적용함으로써 유니코드 문자(한글)가 포함된 경로를 안정적으로 처리하도록 개선했습니다.
*   **문제**: 회원가입 및 비밀번호 찾기 시 이메일 발송 작업이 수 초간 클라이언트의 응답을 차단하여 사용자 경험 저하.
*   **해결**: `@Async` 어노테이션과 전용 Task Executor를 도입하여 메일 발송 로직을 비동기화했습니다. 이를 통해 API 응답 속도를 **약 90% 이상 단축**했습니다.

### 2. 👤 마이페이지 (MyPage)
*   **문제**: 본인과 타인의 프로필 방문 시 보여지는 정보(포인트, 게시글 관리 등)가 동일하게 노출되는 보안 및 UI 문제.
*   **해결**: Thymeleaf의 조건부 렌더링(`th:if`)과 DTO 내 `isOwnProfile` 플래그를 활용하여 소유자 전용 UI와 방문자용 UI를 완벽히 분리했습니다.
*   **문제**: 누적된 방대한 제출 기록 및 활동 내역을 한 번에 조회할 때 발생하는 성능 저하 및 메모리 부담.
*   **해결**: Spring Data JPA의 `Pageable`을 적용하여 활동 내역을 5개 단위로 페이징 처리하고, 필요한 시점에만 데이터를 로드하도록 최적화했습니다.

### 3. 💻 코딩테스트 (Coding Test)
*   **문제**: 사용자가 제출한 악의적인 코드(무한 루프, 메모리 과다 점유)가 서버 자원을 고갈시킬 위험.
*   **해결**: `ProcessBuilder`를 이용한 로컬 샌드박스를 구축하고, `process.waitFor(timeout)`와 JVM 옵션(`-Xmx`)을 통해 시간 및 메모리 사용량을 엄격히 제한했습니다.
*   **문제**: 프로세스가 입력을 다 읽기 전에 조기 종료될 경우 발생하는 'Broken Pipe' (IOException) 에러로 인한 채점 중단.
*   **해결**: 입력 스트림 전달 시 발생하는 예외를 세밀하게 캐치하여, 프로세스가 일찍 종료되더라도 이미 출력된 결과를 바탕으로 정확한 채점 결과(Runtime Error 등)를 반환하도록 로직을 보완했습니다.

### 4. 🤖 AI 코드 리뷰 (AI Code Review)
*   **문제**: AI가 제공하는 피드백이 문제의 맥락을 고려하지 않고 일반적인 답변만 반복하는 현상.
*   **해결**: 프롬프트 엔지니어링을 통해 제출된 코드뿐만 아니라 **실제 실행 결과(평균 실행 시간, 통과 여부)**를 함께 전달하고, 시간 복잡도 분석 및 Java 21 관례 준수 여부를 구체적으로 요청하도록 프롬프트를 고도화했습니다.
*   **문제**: AI API(Gemini/OpenAI) 호출 실패 시 에디터 전체가 멈추거나 사용자에게 불친절한 에러 메시지 노출.
*   **해결**: WebClient의 에러 핸들링 로직을 강화하고, API 키 미설정 또는 통신 장애 시 사용자에게 명확한 안내 메시지를 반환하도록 예외 처리 레이어를 구축했습니다.

---

## 📝 라이선스
이 프로젝트는 [MIT License](LICENSE)를 따릅니다.
