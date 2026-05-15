package com.passfail.ai.service;

import com.passfail.ai.dto.AiChatMessageItemResponse;
import com.passfail.ai.dto.AiChatSessionDetailResponse;
import com.passfail.ai.dto.AiChatSessionItemResponse;
import com.passfail.ai.repository.AiChatMessageRepository;
import com.passfail.ai.repository.AiChatSessionRepository;
import com.passfail.entity.AiChatMessageEntity;
import com.passfail.entity.AiChatSessionEntity;
import com.passfail.entity.MemberEntity;
import com.passfail.enums.AiChatRole;
import com.passfail.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private static final String BLOCKED_MESSAGE =
        "저는 코딩테스트 학습 및 사이트 이용 안내 전용 AI입니다. "
            + "코딩, 알고리즘, 문제풀이, 사이트 기능 관련 질문만 답변할 수 있습니다.";

    private static final String GREETING_MESSAGE = "안녕하세요! 무엇을 도와드릴까요?";

    private static final List<String> ALLOWED_KEYWORDS = List.of(
        "코딩", "코드", "프로그래밍", "개발", "개념", "문법", "디버깅", "오류", "에러", "exception", "bug",
        "알고리즘", "자료구조", "문제", "문제풀이", "풀이", "시간복잡도", "공간복잡도",
        "java", "python", "c++", "cpp", "oop", "객체지향",
        "오버로딩", "오버라이딩", "상속", "다형성", "캡슐화", "추상화", "인터페이스", "클래스", "객체",
        "생성자", "접근제어자", "static", "final", "abstract", "extends", "implements", "예외처리",
        "spring", "spring boot", "controller", "service", "repository", "entity", "dto",
        "jpa", "hibernate", "mybatis", "rest api", "security", "oauth", "session", "jwt",
        "sql", "select", "insert", "update", "delete", "join", "pk", "fk", "정규화", "트랜잭션", "인덱스",
        "배열", "리스트", "스택", "큐", "해시", "hashmap", "트리", "그래프", "정렬", "탐색",
        "dfs", "bfs", "dp", "그리디", "greedy",
        "제출", "채점", "기록", "사이트", "이용", "passfail", "로그인", "회원", "마이페이지"
    );

    private static final List<String> BLOCKED_KEYWORDS = List.of(
        "날씨", "연애", "정치", "사회 이슈", "음식", "치킨", "맛집", "게임 추천",
        "해킹", "악성코드", "랜섬웨어", "바이러스", "크랙", "exploit",
        "이전 지시사항 무시", "지시사항 무시", "무시해", "자유로운 ai처럼", "시스템 프롬프트",
        "프롬프트 보여", "api key", "apikey", "api키", "관리자 권한", "secret key"
    );

    private static final List<String> GREETING_KEYWORDS = List.of(
        "안녕", "안녕하세요", "hello", "hi", "반가워"
    );

    private final MemberRepository memberRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final OpenAiChatService openAiChatService;

    @Transactional
    public Long createSession(String username) {
        if (!hasAuthenticatedUsername(username)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        MemberEntity member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다."));

        AiChatSessionEntity session = AiChatSessionEntity.builder()
            .memberId(member.getMemberId())
            .build();

        return aiChatSessionRepository.save(session).getSessionId();
    }

    @Transactional(readOnly = true)
    public boolean canAccessSession(String username, Long sessionId) {
        try {
            resolveSession(username, sessionId);
            return true;
        } catch (EntityNotFoundException ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<AiChatSessionItemResponse> getSessions(String username, Set<Long> allowedSessionIds) {
        List<AiChatSessionEntity> sessions;

        if (hasAuthenticatedUsername(username)) {
            MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다."));
            sessions = aiChatSessionRepository.findByMemberIdOrderByStartedAtDesc(member.getMemberId());
        } else if (allowedSessionIds == null || allowedSessionIds.isEmpty()) {
            sessions = Collections.emptyList();
        } else {
            sessions = aiChatSessionRepository.findBySessionIdInOrderByStartedAtDesc(allowedSessionIds);
        }

        return sessions.stream()
            .map(this::toSessionItemResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AiChatSessionDetailResponse getSessionDetail(String username, Long sessionId) {
        resolveSession(username, sessionId);
        List<AiChatMessageEntity> messages = aiChatMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId);

        return AiChatSessionDetailResponse.builder()
            .success(true)
            .sessionId(sessionId)
            .title(buildSessionTitle(messages))
            .messages(messages.stream()
                .map(this::toMessageItemResponse)
                .collect(Collectors.toList()))
            .build();
    }

    @Transactional
    public String chat(String username, Long sessionId, String content) {
        if (sessionId == null) {
            throw new IllegalArgumentException("채팅방 정보가 없습니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("질문 내용을 입력해주세요.");
        }

        resolveSession(username, sessionId);

        String normalizedContent = content.trim();
        saveMessage(sessionId, AiChatRole.USER, normalizedContent);

        String answer = generateAnswer(sessionId, normalizedContent);
        saveMessage(sessionId, AiChatRole.ASSISTANT, answer);
        return answer;
    }

    @Transactional
    public void deleteSession(String username, Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("채팅방 정보가 없습니다.");
        }

        if (!hasAuthenticatedUsername(username)) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        MemberEntity member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다."));

        AiChatSessionEntity session = aiChatSessionRepository.findBySessionIdAndMemberId(sessionId, member.getMemberId())
            .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));

        aiChatMessageRepository.deleteBySessionId(sessionId);
        aiChatSessionRepository.delete(session);
    }

    @Transactional
    public void deleteAnonymousSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("채팅방 정보가 없습니다.");
        }

        aiChatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));

        aiChatMessageRepository.deleteBySessionId(sessionId);
        aiChatSessionRepository.deleteById(sessionId);
    }

    private String generateAnswer(Long sessionId, String content) {
        if (isBlockedPrompt(content)) {
            return BLOCKED_MESSAGE;
        }

        if (isGreeting(content)) {
            return GREETING_MESSAGE;
        }

        if (!isAllowedQuestion(content)) {
            return BLOCKED_MESSAGE;
        }

        List<AiChatMessageEntity> history = aiChatMessageRepository.findTop20BySessionIdOrderBySentAtDesc(sessionId);
        Collections.reverse(history);
        return openAiChatService.ask(history, content);
    }

    private AiChatSessionEntity resolveSession(String username, Long sessionId) {
        if (hasAuthenticatedUsername(username)) {
            MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다."));

            return aiChatSessionRepository.findBySessionIdAndMemberId(sessionId, member.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));
        }

        return aiChatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));
    }

    private void saveMessage(Long sessionId, AiChatRole role, String content) {
        AiChatMessageEntity message = AiChatMessageEntity.builder()
            .sessionId(sessionId)
            .role(role)
            .content(content)
            .build();
        aiChatMessageRepository.save(message);
    }

    private boolean hasAuthenticatedUsername(String username) {
        return username != null && !username.isBlank() && !"anonymousUser".equals(username);
    }

    // The filter is intentionally relaxed: if the question is not explicitly blocked,
    // concept questions are allowed by default, and known programming/site keywords force-allow it.
    private boolean isAllowedQuestion(String content) {
        String normalized = normalize(content);

        if (ALLOWED_KEYWORDS.stream().anyMatch(normalized::contains)) {
            return true;
        }

        return !isClearlyNonTechnicalQuestion(normalized);
    }

    private boolean isBlockedPrompt(String content) {
        String normalized = normalize(content);
        return BLOCKED_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private boolean isGreeting(String content) {
        String normalized = normalize(content).replace(" ", "");
        return GREETING_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private boolean isClearlyNonTechnicalQuestion(String normalized) {
        return normalized.contains("오늘 뭐 먹") ||
            normalized.contains("맛집") ||
            normalized.contains("날씨") ||
            normalized.contains("연애") ||
            normalized.contains("정치") ||
            normalized.contains("치킨 추천") ||
            normalized.contains("게임 추천");
    }

    private String normalize(String content) {
        return content == null ? "" : content.toLowerCase(Locale.ROOT);
    }

    private AiChatSessionItemResponse toSessionItemResponse(AiChatSessionEntity session) {
        List<AiChatMessageEntity> messages = aiChatMessageRepository.findBySessionIdOrderBySentAtAsc(session.getSessionId());
        AiChatMessageEntity latestMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);

        LocalDateTime updatedAt = latestMessage != null && latestMessage.getSentAt() != null
            ? latestMessage.getSentAt()
            : session.getStartedAt();

        return AiChatSessionItemResponse.builder()
            .sessionId(session.getSessionId())
            .title(buildSessionTitle(messages))
            .preview(latestMessage != null ? latestMessage.getContent() : "새로운 질문을 시작해보세요.")
            .updatedAt(updatedAt != null ? updatedAt.toString() : null)
            .build();
    }

    private AiChatMessageItemResponse toMessageItemResponse(AiChatMessageEntity message) {
        return AiChatMessageItemResponse.builder()
            .role(message.getRole() != null ? message.getRole().name() : "")
            .content(message.getContent())
            .sentAt(message.getSentAt() != null ? message.getSentAt().toString() : null)
            .build();
    }

    private String buildSessionTitle(List<AiChatMessageEntity> messages) {
        return messages.stream()
            .filter(message -> message.getRole() == AiChatRole.USER)
            .map(AiChatMessageEntity::getContent)
            .filter(content -> content != null && !content.isBlank())
            .map(String::trim)
            .map(content -> content.length() > 24 ? content.substring(0, 24) + "..." : content)
            .findFirst()
            .orElse("새로운 채팅");
    }
}
