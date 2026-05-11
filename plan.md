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

# # 수정 및 진행 내역 (최종 업데이트: 2026-04-29)

### 1. 패키지 통합 및 구조 정리 (Problem 패키지)
- **파일 병합**: 분산되어 있던 `ProblemController1`, `ProblemRepository1`, `ProblemService1`, `TestCaseRepository1`을 원본 파일로 통합하여 프로젝트 구조 단일화.
- **오타 수정**: 잘못 지정된 패키지 경로(`com.passfail.condingtest.dto`)를 `com.passfail.problem.dto`로 일괄 수정.
- **테스트 코드 제거**: 테스트 목적으로 생성되었던 중복 컨트롤러 및 템플릿(`codingtest/list.html`)을 삭제하여 관리자/사용자 기능 분리.

### 2. JPA 및 DB 쿼리 호환성 해결
- **SQL 문법 수정**: `ProblemService` 내의 Native Query에서 Oracle 방식(`FETCH FIRST`)을 MySQL/MariaDB 방식(`LIMIT 1`)으로 변경하여 쿼리 오류 해결.
- **레포지토리 메서드 매핑**: `TestCaseRepository`에서 `ProblemEntity`의 `id` 필드 참조 오류를 실제 필드명인 `problemId`로 매핑되도록 `@Query`를 통해 명시적으로 수정.
- **의존성 주입 수정**: `MypageController`에서 누락된 `MemberRepository`를 주입하여 런타임 오류 방지.

### 3. 리다이렉트 및 인코딩 오류 수정 (마이페이지)
- **닉네임 기반 경로 이동**: 소셜 로그인 시 고유 ID(Provider ID)가 아닌 실제 사용자의 닉네임을 조회하여 `/mypage/{username}`으로 정확히 리다이렉트되도록 개선.
- **한글 닉네임 지원 (Unicode)**: 리다이렉트 URL에 한글이 포함될 경우 발생하는 `IllegalArgumentException`을 방지하기 위해 `URLEncoder`를 사용한 UTF-8 인코딩 적용.
- **템플릿 보안 제약 해결**: Thymeleaf 3.1+의 보안 정책으로 인해 `th:onclick` 내에서 변수 사용 시 발생하는 오류를 표준 `th:href` 링크 방식으로 변경하여 해결.

### 4. 프론트엔드 경로 및 편의성 개선
- **정적 자원 경로 수정**: `problemList.html`, `problemCreate.html`에서 CSS/JS 파일을 찾지 못하던 경로 문제(`/problem/` 하위 폴더 누락)를 해결.
- **네비게이션 연동**: 메인 화면의 문제 카드 및 네비게이션 메뉴가 통합된 문제 목록 페이지(`/problem/problemList`)와 문제 풀이 화면(`/codingtest/{id}`)으로 정확히 연결되도록 수정.
- **에디터 UI 개선**: 문제 풀이 화면(`editor.html`)에서 '나가기' 시 목록으로 이동하고, 로고 클릭 시 메인으로 이동하는 링크 보완.

