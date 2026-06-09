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

## 📝 라이선스
이 프로젝트는 [MIT License](LICENSE)를 따릅니다.
