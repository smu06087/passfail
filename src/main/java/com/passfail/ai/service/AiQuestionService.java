package com.passfail.ai.service;

import com.passfail.ai.dto.AiChatMessageItemResponse;
import com.passfail.ai.dto.AiChatSessionDetailResponse;
import com.passfail.ai.dto.AiChatSessionItemResponse;
import com.passfail.ai.repository.AiChatMessageRepository;
import com.passfail.ai.repository.AiChatSessionRepository;
import com.passfail.entity.AiChatMessageEntity;
import com.passfail.entity.AiChatSessionEntity;
import com.passfail.entity.MemberEntity;
import com.passfail.enums.AiChatHandoffStatus;
import com.passfail.enums.AiChatRole;
import com.passfail.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private static final String BLOCKED_MESSAGE =
        "PassFail AI는 코딩 테스트 학습, 디버깅, 알고리즘, 자료구조, 그리고 PassFail 사이트 사용 안내만 답변합니다.";
    private static final String GREETING_MESSAGE =
        "안녕하세요. 코딩 테스트나 PassFail 사용법을 질문해 주세요.";
    private static final String IMAGE_ONLY_PROMPT =
        "첨부 이미지를 보고 코딩 테스트 학습, 디버깅, 알고리즘, 자료구조 또는 PassFail 사이트 사용과 관련된 내용만 설명해 주세요.";
    private static final String IMAGE_MESSAGE_SUFFIX = "[이미지 첨부]";
    private static final String HANDOFF_OFFER_MESSAGE =
        "정확한 안내를 위해 상담원에게 연결해 드릴까요? 원하시면 \"상담원 연결\"이라고 말씀해 주세요.";
    private static final String HANDOFF_REQUESTED_MESSAGE =
        "잠시만 기다려 주세요! 빠르게 답변 받을 수 있도록 질문을 먼저 보내주세요. 관리자가 확인 후 답변드릴 수 있게 전달하겠습니다.";
    private static final String HANDOFF_ALREADY_REQUESTED_MESSAGE =
        "상담원 연결 요청이 이미 접수되어 있습니다. 추가로 남길 내용이 있으면 이어서 보내주세요.";
    private static final String ADMIN_REPLY_PREFIX = "[상담원 답변] ";
    private static final String ADMIN_NAME_PREFIX = "[상담원:";
    private static final String ADMIN_CLOSE_MESSAGE = "[상담 종료] 상담이 종료되었습니다.";

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/webp",
        "image/gif"
    );

    private static final List<String> ALLOWED_KEYWORDS = List.of(
        "코딩", "코드", "개발", "문법", "에러", "오류", "exception", "bug",
        "알고리즘", "자료구조", "문제", "풀이", "시간복잡도", "공간복잡도",
        "java", "python", "c++", "cpp", "oop", "객체지향",
        "오버로딩", "오버라이딩", "상속", "캡슐화", "추상화", "인터페이스", "클래스", "객체",
        "생성자", "접근제어자", "static", "final", "abstract", "extends", "implements", "예외처리",
        "spring", "spring boot", "controller", "service", "repository", "entity", "dto",
        "jpa", "hibernate", "mybatis", "rest api", "security", "oauth", "session", "jwt",
        "sql", "select", "insert", "update", "delete", "join", "pk", "fk", "정규화", "트랜잭션", "인덱스",
        "배열", "리스트", "스택", "큐", "캐시", "hashmap", "트리", "그래프", "정렬", "탐색",
        "dfs", "bfs", "dp", "그리디", "greedy",
        "제출", "채점", "기록", "사이트", "이용", "passfail", "로그인", "회원", "마이페이지"
    );

    private static final List<String> BLOCKED_KEYWORDS = List.of(
        "주식", "정치", "사회 이슈", "맛집", "게임 추천",
        "해킹", "악성코드", "랜섬웨어", "바이러스", "크랙", "exploit",
        "이전 지시사항 무시", "지시사항 무시", "자유로운 ai처럼", "시스템 프롬프트",
        "프롬프트 보여", "api key", "apikey", "관리자 권한", "secret key"
    );

    private static final List<String> GREETING_KEYWORDS = List.of(
        "안녕", "안녕하세요", "hello", "hi", "반가워"
    );

    private static final List<String> HANDOFF_FAILURE_MESSAGES = List.of(
        "ai 응답을 가져오지 못했습니다.",
        "ai 응답 생성 중 오류가 발생했습니다.",
        "ai 채팅 설정이 아직 완료되지 않았습니다."
    );

    private static final List<String> HANDOFF_UNCERTAIN_MESSAGES = List.of(
        "잘 모르겠습니다",
        "알 수 없습니다",
        "확인할 수 없습니다",
        "정확한 답변이 어렵습니다",
        "정확히 안내드리기 어렵습니다",
        "제공된 정보만으로는",
        "확실하지 않습니다",
        "도와드리기 어렵습니다"
    );

    private static final List<String> HANDOFF_AGENT_KEYWORDS = List.of(
        "상담원", "상담사", "관리자", "직원", "사람"
    );

    private static final List<String> HANDOFF_CONNECT_KEYWORDS = List.of(
        "연결", "상담", "문의", "답변", "도움", "불러", "불러줘", "불러주세요", "호출"
    );

    private static final List<String> HANDOFF_CONFIRM_KEYWORDS = List.of(
        "네", "예", "응", "좋아요", "부탁", "해주세요", "해줘", "연결"
    );

    private final MemberRepository memberRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final OpenAiChatService openAiChatService;

    @Transactional
    public Long createSession(String username) {
        if (!hasAuthenticatedUsername(username)) {
            AiChatSessionEntity anonymousSession = AiChatSessionEntity.builder().build();
            return aiChatSessionRepository.save(anonymousSession).getSessionId();
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

        return sessions.stream().map(this::toSessionItemResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AiChatSessionDetailResponse getSessionDetail(String username, Long sessionId) {
        resolveSession(username, sessionId);
        List<AiChatMessageEntity> messages = aiChatMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId);

        return AiChatSessionDetailResponse.builder()
            .success(true)
            .sessionId(sessionId)
            .title(buildSessionTitle(messages))
            .handoffStatus(resolveHandoffStatus(messages).name())
            .messages(messages.stream().map(this::toMessageItemResponse).collect(Collectors.toList()))
            .build();
    }

    @Transactional
    public String chat(String username, Long sessionId, String content, MultipartFile image) {
        if (sessionId == null) {
            throw new IllegalArgumentException("채팅방 정보가 없습니다.");
        }

        String normalizedContent = content == null ? "" : content.trim();
        boolean hasImage = image != null && !image.isEmpty();

        if (normalizedContent.isEmpty() && !hasImage) {
            throw new IllegalArgumentException("질문 내용이나 이미지를 하나 이상 입력해 주세요.");
        }

        if (hasImage) {
            validateImage(image);
        }

        resolveSession(username, sessionId);
        saveMessage(sessionId, AiChatRole.USER, buildStoredUserMessage(normalizedContent, hasImage));

        String answer = generateAnswer(sessionId, normalizedContent, image);
        saveMessage(sessionId, AiChatRole.ASSISTANT, answer);
        return answer;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRequestedHandoffs() {
        return aiChatSessionRepository.findAll().stream()
            .map(session -> Map.entry(session, aiChatMessageRepository.findBySessionIdOrderBySentAtAsc(session.getSessionId())))
            .filter(entry -> isVisibleHandoffStatus(resolveHandoffStatus(entry.getValue())))
            .sorted(Comparator.comparing(
                entry -> extractRequestedAt(entry.getValue()),
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .map(entry -> toHandoffSummary(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AiChatSessionDetailResponse getHandoffDetail(Long sessionId) {
        aiChatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));
        List<AiChatMessageEntity> messages = aiChatMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId);

        return AiChatSessionDetailResponse.builder()
            .success(true)
            .sessionId(sessionId)
            .title(buildSessionTitle(messages))
            .handoffStatus(resolveHandoffStatus(messages).name())
            .messages(messages.stream().map(this::toMessageItemResponse).collect(Collectors.toList()))
            .build();
    }

    @Transactional
    public void replyToHandoff(Long sessionId, String reply, String adminName) {
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("답변 내용을 입력해 주세요.");
        }

        aiChatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));

        saveMessage(sessionId, AiChatRole.ASSISTANT, buildAdminReplyMessage(reply.trim(), adminName));
    }

    @Transactional
    public void closeHandoff(Long sessionId) {
        aiChatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다."));

        saveMessage(sessionId, AiChatRole.ASSISTANT, ADMIN_CLOSE_MESSAGE);
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

    private String generateAnswer(Long sessionId, String content, MultipartFile image) {
        List<AiChatMessageEntity> messages = aiChatMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
        AiChatHandoffStatus status = resolveHandoffStatus(messages);

        if (isHandoffRequest(content, status)) {
            return isVisibleHandoffStatus(status)
                ? HANDOFF_ALREADY_REQUESTED_MESSAGE
                : HANDOFF_REQUESTED_MESSAGE;
        }

        if (isVisibleHandoffStatus(status)) {
            return HANDOFF_ALREADY_REQUESTED_MESSAGE;
        }

        String effectivePrompt = content == null || content.isBlank() ? IMAGE_ONLY_PROMPT : content;

        if (isBlockedPrompt(effectivePrompt)) {
            return HANDOFF_OFFER_MESSAGE;
        }

        if ((image == null || image.isEmpty()) && isGreeting(effectivePrompt)) {
            return GREETING_MESSAGE;
        }

        if (!isAllowedQuestion(effectivePrompt)) {
            return HANDOFF_OFFER_MESSAGE;
        }

        List<AiChatMessageEntity> history = aiChatMessageRepository.findTop20BySessionIdOrderBySentAtDesc(sessionId);
        Collections.reverse(history);

        String answer = openAiChatService.ask(history, effectivePrompt, image);
        return shouldOfferHandoff(answer) ? HANDOFF_OFFER_MESSAGE : answer;
    }

    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("PNG, JPG, WEBP, GIF 이미지만 첨부할 수 있습니다.");
        }
    }

    private String buildStoredUserMessage(String content, boolean hasImage) {
        if (!hasImage) {
            return content;
        }
        if (content == null || content.isBlank()) {
            return IMAGE_MESSAGE_SUFFIX;
        }
        return content + System.lineSeparator() + System.lineSeparator() + IMAGE_MESSAGE_SUFFIX;
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

    private String buildAdminReplyMessage(String reply, String adminName) {
        if (adminName == null || adminName.isBlank()) {
            return ADMIN_REPLY_PREFIX + reply;
        }
        return ADMIN_REPLY_PREFIX + ADMIN_NAME_PREFIX + adminName.trim() + "] " + reply;
    }

    private boolean hasAuthenticatedUsername(String username) {
        return username != null && !username.isBlank() && !"anonymousUser".equals(username);
    }

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
        return GREETING_KEYWORDS.stream().anyMatch(keyword -> normalized.contains(normalize(keyword)));
    }

    private boolean shouldOfferHandoff(String answer) {
        if (answer == null || answer.isBlank()) {
            return true;
        }

        String normalized = normalize(answer);
        if (normalized.equals(normalize(BLOCKED_MESSAGE))) {
            return true;
        }

        return HANDOFF_FAILURE_MESSAGES.stream().anyMatch(message -> normalized.contains(normalize(message)))
            || HANDOFF_UNCERTAIN_MESSAGES.stream().anyMatch(message -> normalized.contains(normalize(message)));
    }

    private boolean isHandoffRequest(String content, AiChatHandoffStatus status) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return false;
        }

        boolean containsAgentKeyword = HANDOFF_AGENT_KEYWORDS.stream()
            .anyMatch(keyword -> normalized.contains(normalize(keyword)));
        boolean containsConnectKeyword = HANDOFF_CONNECT_KEYWORDS.stream()
            .anyMatch(keyword -> normalized.contains(normalize(keyword)));

        if (containsAgentKeyword && containsConnectKeyword) {
            return true;
        }

        if (status == AiChatHandoffStatus.OFFERED) {
            return HANDOFF_CONFIRM_KEYWORDS.stream()
                .anyMatch(keyword -> normalized.contains(normalize(keyword)));
        }

        return false;
    }

    private boolean isClearlyNonTechnicalQuestion(String normalized) {
        return normalized.contains("오늘 뭐 먹")
            || normalized.contains("맛집")
            || normalized.contains("주식")
            || normalized.contains("영화")
            || normalized.contains("정치")
            || normalized.contains("게임 추천");
    }

    private String normalize(String content) {
        return content == null ? "" : content.toLowerCase(Locale.ROOT);
    }

    private AiChatHandoffStatus resolveHandoffStatus(List<AiChatMessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return AiChatHandoffStatus.NONE;
        }

        boolean closed = messages.stream()
            .map(AiChatMessageEntity::getContent)
            .anyMatch(ADMIN_CLOSE_MESSAGE::equals);
        if (closed) {
            return AiChatHandoffStatus.CLOSED;
        }

        boolean handled = messages.stream()
            .map(AiChatMessageEntity::getContent)
            .anyMatch(content -> content != null && content.startsWith(ADMIN_REPLY_PREFIX));
        if (handled) {
            return AiChatHandoffStatus.HANDLED;
        }

        boolean requested = messages.stream()
            .map(AiChatMessageEntity::getContent)
            .anyMatch(HANDOFF_REQUESTED_MESSAGE::equals);
        if (requested) {
            return AiChatHandoffStatus.REQUESTED;
        }

        String lastAssistantMessage = messages.stream()
            .filter(message -> message.getRole() == AiChatRole.ASSISTANT)
            .map(AiChatMessageEntity::getContent)
            .reduce((first, second) -> second)
            .orElse("");
        if (HANDOFF_OFFER_MESSAGE.equals(lastAssistantMessage)) {
            return AiChatHandoffStatus.OFFERED;
        }

        return AiChatHandoffStatus.NONE;
    }

    private boolean isVisibleHandoffStatus(AiChatHandoffStatus status) {
        return status == AiChatHandoffStatus.REQUESTED || status == AiChatHandoffStatus.HANDLED;
    }

    private LocalDateTime extractRequestedAt(List<AiChatMessageEntity> messages) {
        return messages.stream()
            .filter(message -> message.getRole() == AiChatRole.ASSISTANT)
            .filter(message -> HANDOFF_REQUESTED_MESSAGE.equals(message.getContent()))
            .map(AiChatMessageEntity::getSentAt)
            .filter(value -> value != null)
            .findFirst()
            .orElse(null);
    }

    private Map<String, Object> toHandoffSummary(AiChatSessionEntity session, List<AiChatMessageEntity> messages) {
        String latestQuestion = messages.stream()
            .filter(message -> message.getRole() == AiChatRole.USER)
            .map(AiChatMessageEntity::getContent)
            .filter(value -> value != null && !value.isBlank())
            .reduce((first, second) -> second)
            .orElse("");

        LocalDateTime requestedAt = extractRequestedAt(messages);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("memberName", session.getMember() != null ? session.getMember().getUsername() : "anonymous");
        result.put("requestedAt", requestedAt != null ? requestedAt.toString() : null);
        result.put("title", buildSessionTitle(messages));
        result.put("latestQuestion", latestQuestion);
        result.put("messageCount", messages.size());
        return result;
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
            .preview(latestMessage != null ? latestMessage.getContent() : "첫 질문을 시작해 보세요.")
            .updatedAt(updatedAt != null ? updatedAt.toString() : null)
            .latestRole(latestMessage != null && latestMessage.getRole() != null ? latestMessage.getRole().name() : "")
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
            .orElse("새 채팅");
    }
}
