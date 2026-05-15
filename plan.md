# 구현 계획서: Passfail 사용자 인증 및 코딩테스트 시스템

## 1. 개요
사용자 중심의 핵심 기능인 로그인(소셜/로컬), 회원가입, 마이페이지, 그리고 코딩테스트 시스템을 구현함. 지정된 담당 범위 외의 기능(쇼핑몰 등)은 수정하거나 구현하지 않음.

## 2. 담당 기능 범위
- **인증 시스템 (Auth)**:
    - 소셜 로그인: 카카오, 구글, 네이버, 깃허브 연동.
    - 로컬 로그인: 아이디/비밀번호 기반 로그인.
    - 비밀번호 재설정: 이메일을 통해 임시 비밀번호 전송.
- **회원 관리 (Member & Mypage)**:
    - 회원가입 및 DTO 기반 데이터 처리.
    - 마이페이지: 프로필 이미지 관리, 닉네임 수정, 다중 소셜 계정 통합 연동.
- **코딩테스트 (Coding Test)**:
    - 문제 풀이 및 제출 시스템 연동.

## 3. 구현 단계

### 1단계: 로그인 및 회원가입 (com.passfail.member) [V]
- `MemberController`: 인증 관련 API 전담.
- `MemberService`: 가입 로직 및 보안 설정.
- `CustomOAuth2UserService`: 4대 소셜 로그인 및 계정 통합 로직 완성.

### 2단계: 비밀번호 재설정 및 마이페이지 분리 [V]
- `MypageController` & `MypageService`: 마이페이지 전용 레이어 분리 및 구현.
- `MemberInfoResponse` DTO를 통한 안전한 데이터 반환.

### 3단계: 사용자 활동 데이터 및 보안 고도화 [V]
- 문제 해결 이력, 제출 기록, 알림 시스템 통합.
- 이메일 인증 기반 비밀번호 변경 및 찾기 구현.

### 4단계: 코딩테스트 시스템 및 AI 리뷰 통합 [V]
- Monaco Editor 기반 웹 IDE 및 실시간 채점 엔진 구축.
- 프로그래머스 스타일의 문제 설명 분리 및 커스텀 테스트 케이스 기능.

## 4. DTO 및 엔티티 구조
- **DTO**: `MemberJoinRequest`, `MemberInfoResponse`, `ProblemResponse`, `TestCaseResponse`, `ExecutionResult`.
- **Entity**: `MemberEntity`, `SocialAccountEntity`, `SubmissionEntity`, `SolvedProblemEntity`, `NotificationEntity`, `ProblemEntity`, `TestCaseEntity`.

---

# # 수정 및 진행 내역 (최종 업데이트: 2026-05-15)

### 1. 전역 UI 표준화 및 디자인 통일 (Navigation Bar)
- **네비게이션 바 일관성 확보**: 모든 페이지에서 네비게이션 바의 글씨체와 크기가 다르게 보이던 문제를 해결하기 위해 `px` 단위를 사용하여 고정 레이아웃을 적용.
- **폰트 및 스타일 강제**: 전역 폰트를 'Pretendard'로 통일하고, `!important` 선언을 통해 다른 CSS 라이브러리(Bootstrap 등)의 간섭을 차단.
- **검색창 표준화**: `ProblemList` 페이지를 포함한 모든 페이지의 검색창 안내 문구를 "검색 기능을 이용해보세요"로 통일.

### 2. 이메일 발송 성능 및 사용자 경험(UX) 최적화
- **비동기 메일 서비스 도입**: `@Async` 어노테이션과 전용 `MailService`를 구축하여 이메일 발송 작업을 백그라운드 스레드로 분리. 이를 통해 API 응답 시간을 수 초에서 0.1초 내외로 단축.
- **체감 성능 개선 (Optimistic UI)**: 회원가입 및 비밀번호 찾기 시, 실제 서버 응답 전이라도 즉시 UI 반응(인증창 노출, 성공 메시지 등)을 제공하여 사용자 대기 시간을 최소화.

### 3. 계정 정책 강화 및 보안 고도화
- **아이디 수정 기능 제한**: 마이페이지에서 사용자가 아이디(닉네임)를 직접 수정할 수 없도록 UI를 `readonly`로 변경하고, 서버 측의 업데이트 API(`/mypage/update`)를 삭제하여 식별자 영속성 확보.

### 4. 주요 페이지 디자인 리브랜딩 (Ranking & Auth)
- **랭킹 페이지 전면 개편**: 기존 Bootstrap 스타일을 제거하고 프로젝트 표준인 '글래스 모피즘(Glass-card)' 스타일로 재설계. 순위별 배지 및 티어 컬러 시스템을 통합.
- **인증 페이지 디자인 통일**: 로그인, 회원가입, 비밀번호 찾기 페이지의 테마를 그린 그라데이션과 Pretendard 폰트로 통일하여 브랜드 아이덴티티 강화.

### 5. 패키지 통합 및 구조 정리 (Problem 패키지)
- **파일 병합**: 분산되어 있던 `ProblemController1`, `ProblemRepository1`, `ProblemService1`, `TestCaseRepository1`을 원본 파일로 통합하여 프로젝트 구조 단일화.
- **오타 수정**: 잘못 지정된 패키지 경로(`com.passfail.condingtest.dto`)를 `com.passfail.problem.dto`로 일괄 수정.
- **테스트 코드 제거**: 테스트 목적으로 생성되었던 중복 컨트롤러 및 템플릿(`codingtest/list.html`)을 삭제하여 관리자/사용자 기능 분리.

### 6. JPA 및 DB 쿼리 호환성 해결
- **SQL 문법 수정**: `ProblemService` 내의 Native Query에서 Oracle 방식(`FETCH FIRST`)을 MySQL/MariaDB 방식(`LIMIT 1`)으로 변경하여 쿼리 오류 해결.
- **레포지토리 메서드 매핑**: `TestCaseRepository`에서 `ProblemEntity`의 `id` 필드 참조 오류를 실제 필드명인 `problemId`로 매핑되도록 `@Query`를 통해 명시적으로 수정.
- **의존성 주입 수정**: `MypageController`에서 누락된 `MemberRepository`를 주입하여 런타임 오류 방지.

### 7. 리다이렉트 및 인코딩 오류 수정 (마이페이지)
- **닉네임 기반 경로 이동**: 소셜 로그인 시 고유 ID(Provider ID)가 아닌 실제 사용자의 닉네임을 조회하여 `/mypage/{username}`으로 정확히 리다이렉트되도록 개선.
- **한글 닉네임 지원 (Unicode)**: 리다이렉트 URL에 한글이 포함될 경우 발생하는 `IllegalArgumentException`을 방지하기 위해 `URLEncoder`를 사용한 UTF-8 인코딩 적용.
- **템플릿 보안 제약 해결**: Thymeleaf 3.1+의 보안 정책으로 인해 `th:onclick` 내에서 변수 사용 시 발생하는 오류를 표준 `th:href` 링크 방식으로 변경하여 해결.

### 8. 프론트엔드 경로 및 편의성 개선
- **정적 자원 경로 수정**: `problemList.html`, `problemCreate.html`에서 CSS/JS 파일을 찾지 못하던 경로 문제(`/problem/` 하위 폴더 누락)를 해결.
- **네비게이션 연동**: 메인 화면의 문제 카드 및 네비게이션 메뉴가 통합된 문제 목록 페이지(`/problem/problemList`)와 문제 풀이 화면(`/codingtest/{id}`)으로 정확히 연결되도록 수정.
- **에디터 UI 개선**: 문제 풀이 화면(`editor.html`)에서 '나가기' 시 목록으로 이동하고, 로고 클릭 시 메인으로 이동하는 링크 보완.

### 9. 제출 기록 로직 및 정답률 산출 방식 개선
- **제출 데이터 영속성 강화**: `SubmissionEntity`의 유니크 제약 조건을 제거하여 사용자의 모든 시도(오답, 컴파일 에러 포함)를 개별 데이터로 보존하도록 변경.
- **정확한 통계 산출**: 모든 제출 시도를 `submissionCount`에 반영하고, 모든 테스트 케이스를 통과한 경우에만 `acceptedCount`를 증가시켜 실질적인 정답률 산출.
- **이력 조회 최적화**: 최신 제출 코드를 불러오기 위해 `SubmissionRepository`에 정렬 기반의 단일 건 조회 메서드 추가.

### 10. 마이페이지 사용자 활동 통합 및 UI 최적화
- **커뮤니티 활동 통합 조회**: '내가 쓴 게시글', '좋아요 한 게시글', '내가 쓴 댓글' 확인 기능을 마이페이지 내 단일 섹션('커뮤니티 활동')으로 통합 구현.
- **개인정보 노출 제어**: 타인 프로필 방문 시 포인트 정보, 충전 버튼, 고객 지원 등 소유자 전용 UI가 노출되지 않도록 Thymeleaf 조건부 렌더링 적용.
- **알림 기능 삭제**: 사용성이 낮고 UI 집중도를 분산시키던 '알림' 섹션을 제거하여 단순화.
- **URL 표준화**: 마이페이지 내 게시글 링크가 게시판 표준 형식인 `/posts/{id}/view`를 따르도록 일괄 수정.
- **활동 데이터 페이징**: 게시글/댓글 내역 등에 페이징(5개 단위)을 적용하여 성능 및 가독성 개선.

### 11. 네비게이션 및 에러 페이지 UX 개선
- **대회(Contest) 메뉴 추가**: 네비게이션 바에 향후 확장성을 고려한 '대회' 버튼(placeholder) 추가.
- **에러 페이지 정제**: 404 에러 페이지에서 불필요한 네비게이션 바를 제거하고 에러 메시지에 집중하도록 템플릿 수정.

### 12. 서비스 의존성 및 안정성 복구
- **누락된 의존성 복구**: 리팩토링 과정에서 실수로 삭제된 `SocialAccountRepository` 등의 임포트 문을 복구하여 서버 가동 중단 오류(`UnsatisfiedDependencyException`) 해결.
