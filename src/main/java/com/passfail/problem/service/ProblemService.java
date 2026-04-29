package com.passfail.problem.service;

import com.passfail.entity.*;
import com.passfail.enums.*;
import com.passfail.member.repository.MemberRepository;
import com.passfail.problem.dto.*;
import com.passfail.problem.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private static final Logger log = LoggerFactory.getLogger(ProblemService.class);

    private final ProblemRepository problemRepository;
    private final ProblemTagRepository problemTagRepository;
    private final TestCaseRepository testCaseRepository;
    private final MemberRepository memberRepository;
    private final SolvedProblemRepository solvedProblemRepository;
    private final SubmissionRepository submissionRepository;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<String, List<String>> relatedKeywordMap = createRelatedKeywordMap();

    // Methods from the original ProblemService (User interaction)

    public List<ProblemResponse> getActiveProblems() {
        return problemRepository.findByStatus(ProblemStatus.PUBLISHED).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ProblemResponse getProblemResponse(Long id) {
        ProblemEntity problem = problemRepository.findByIdWithTestCases(id)
                .orElseThrow(() -> new EntityNotFoundException("문제를 찾을 수 없습니다."));
        return convertToResponse(problem);
    }

    public ProblemEntity getProblemEntity(Long id) {
        return problemRepository.findByIdWithTestCases(id)
                .orElseThrow(() -> new EntityNotFoundException("문제를 찾을 수 없습니다."));
    }

    @Transactional
    public SubmissionEntity submitSolution(String username, Long problemId, String code, ProgrammingLanguage language) {
        MemberEntity member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        ProblemEntity problem = getProblemEntity(problemId);

        SubmissionEntity submission = submissionRepository.findByMemberIdAndProblemId(member.getMemberId(), problemId)
                .orElse(SubmissionEntity.builder()
                        .memberId(member.getMemberId())
                        .problemId(problem.getProblemId())
                        .build());
        
        submission.setCode(code);
        submission.setLanguage(language);
        submission.setStatus(SubmissionStatus.ACCEPTED);
        submission.setExecutionTimeMs(100);
        submission.setMemoryUsedKb(1024);
        
        submission = submissionRepository.save(submission);

        solvedProblemRepository.findByMemberId(member.getMemberId()).stream()
                .filter(sp -> sp.getProblemId().equals(problemId))
                .findFirst()
                .ifPresentOrElse(
                    sp -> sp.setTryCount(sp.getTryCount() + 1),
                    () -> {
                        int score = 100;
                        SolvedProblemEntity solvedProblem = SolvedProblemEntity.builder()
                                .memberId(member.getMemberId())
                                .problemId(problemId)
                                .scoreEarned(score)
                                .tryCount(1)
                                .build();
                        solvedProblemRepository.save(solvedProblem);
                        
                        member.setTotalScore(member.getTotalScore() + score);
                        member.setTier(Tier.fromScore(member.getTotalScore()));
                        memberRepository.save(member);
                    }
                );

        problem.setSubmissionCount(problem.getSubmissionCount() + 1);
        problem.setAcceptedCount(problem.getAcceptedCount() + 1);
        problem.setAcceptanceRate((double) problem.getAcceptedCount() / (double) problem.getSubmissionCount() * 100.0);
        problemRepository.save(problem);
        
        return submission;
    }

    @Transactional(readOnly = true)
    public String getPreviousSolution(String username, Long problemId) {
        MemberEntity member = memberRepository.findByUsername(username).orElse(null);
        if (member == null) return null;
        
        return submissionRepository.findByMemberIdAndProblemId(member.getMemberId(), problemId)
                .map(SubmissionEntity::getCode)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isSolved(String username, Long problemId) {
        MemberEntity member = memberRepository.findByUsername(username).orElse(null);
        if (member == null) return false;
        
        return solvedProblemRepository.findByMemberId(member.getMemberId()).stream()
                .anyMatch(sp -> sp.getProblemId().equals(problemId));
    }

    private ProblemResponse convertToResponse(ProblemEntity entity) {
        return ProblemResponse.builder()
                .problemId(entity.getProblemId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .difficulty(entity.getDifficulty().name())
                .category(entity.getCategory())
                .timeLimitMs(entity.getTimeLimitMs())
                .memoryLimitMb(entity.getMemoryLimitMb())
                .acceptanceRate(entity.getAcceptanceRate())
                .testCases(entity.getTest_cases() != null ? entity.getTest_cases().stream()
                        .map(tc -> TestCaseResponse.builder()
                                .caseId(tc.getCaseId())
                                .inputData(tc.getInputData())
                                .expectedOutput(tc.getExpectedOutput())
                                .isSample(tc.getIsSample())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .build();
    }

    // Methods from ProblemService1 (Admin / Problem Management)

    @Transactional
    public Long createProblem(ProblemDTO problemDTO) {
        validate(problemDTO);

        ProblemEntity problem = new ProblemEntity();
        applyProblemFields(problem, problemDTO);
        problem.setCreatedBy(resolveCreatedBy(problemDTO.getCreatedBy()));

        ProblemEntity savedProblem = problemRepository.save(problem);
        replaceTags(savedProblem.getProblemId(), problemDTO.getTags());
        replaceTests(
            savedProblem.getProblemId(),
            problemDTO.getSampleInputs(),
            problemDTO.getSampleOutputs(),
            problemDTO.getTestInputs(),
            problemDTO.getTestOutputs()
        );

        return savedProblem.getProblemId();
    }

    @Transactional
    public Long updateProblem(Long problemId, ProblemDTO problemDTO) {
        validate(problemDTO);

        ProblemEntity problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new EntityNotFoundException("문제를 찾을 수 없습니다. id=" + problemId));

        applyProblemFields(problem, problemDTO);
        problemRepository.save(problem);
        replaceTags(problemId, problemDTO.getTags());
        replaceTests(
            problemId,
            problemDTO.getSampleInputs(),
            problemDTO.getSampleOutputs(),
            problemDTO.getTestInputs(),
            problemDTO.getTestOutputs()
        );

        return problemId;
    }

    @Transactional(readOnly = true)
    public List<ProblemDTO> getProblemList() {
        List<ProblemEntity> problems = problemRepository.findAll().stream()
            .sorted((left, right) -> Long.compare(right.getProblemId(), left.getProblemId()))
            .collect(Collectors.toList());

        log.debug("Loaded {} problems via JPA. First IDs: {}", problems.size(),
            problems.stream().limit(5).map(ProblemEntity::getProblemId).collect(Collectors.toList()));

        return problems.stream()
            .map(this::toProblemDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProblemDTO getProblemDetail(Long problemId) {
        ProblemEntity problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new EntityNotFoundException("문제를 찾을 수 없습니다. id=" + problemId));

        ProblemDTO dto = toProblemDto(problem);
        dto.setTags(problemTagRepository.findByProblemIdOrderByTagIdAsc(problemId).stream()
            .map(ProblemTagEntity::getTagName)
            .collect(Collectors.toList()));

        List<TestCaseEntity> sampleTests = testCaseRepository.findByProblemIdAndIsSampleTrueOrderByOrderNumAsc(problemId);
        dto.setSampleInputs(sampleTests.stream()
            .map(TestCaseEntity::getInputData)
            .collect(Collectors.toList()));
        dto.setSampleOutputs(sampleTests.stream()
            .map(TestCaseEntity::getExpectedOutput)
            .collect(Collectors.toList()));

        List<TestCaseEntity> testCases = testCaseRepository.findByProblemIdAndIsSampleFalseOrderByOrderNumAsc(problemId);
        dto.setTestInputs(testCases.stream()
            .map(TestCaseEntity::getInputData)
            .collect(Collectors.toList()));
        dto.setTestOutputs(testCases.stream()
            .map(TestCaseEntity::getExpectedOutput)
            .collect(Collectors.toList()));
        return dto;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProblemDebugInfo() {
        Map<String, Object> debugInfo = new LinkedHashMap<>();
        List<ProblemEntity> problems = problemRepository.findAll();

        debugInfo.put("jpaCount", problems.size());
        debugInfo.put("jpaFirstIds", problems.stream()
            .limit(5)
            .map(ProblemEntity::getProblemId)
            .collect(Collectors.toList()));

        Object nativeCount = entityManager.createNativeQuery("select count(*) from problem")
            .getSingleResult();
        Object currentUser = entityManager.createNativeQuery("select user from dual")
            .getSingleResult();

        debugInfo.put("nativeCount", toLong(nativeCount));
        debugInfo.put("oracleUser", currentUser);

        try (Connection connection = dataSource.getConnection()) {
            debugInfo.put("jdbcUrl", connection.getMetaData().getURL());
            debugInfo.put("jdbcUser", connection.getMetaData().getUserName());
            debugInfo.put("schema", connection.getSchema());
        } catch (Exception ex) {
            debugInfo.put("connectionMetadataError", ex.getMessage());
        }

        return debugInfo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchProblems(String query) {
        List<String> expandedKeywords = expandKeywords(query);
        List<ProblemDTO> problems = getProblemList();

        if (!hasText(query)) {
            return Map.of(
                "query", "",
                "keywords", Collections.emptyList(),
                "problemCount", problems.size(),
                "problems", problems
            );
        }

        List<ProblemDTO> matchedProblems = problems.stream()
            .filter(problem -> matchesSearch(problem, expandedKeywords))
            .collect(Collectors.toList());

        return Map.of(
            "query", query.trim(),
            "keywords", expandedKeywords,
            "problemCount", matchedProblems.size(),
            "problems", matchedProblems
        );
    }

    @Transactional(readOnly = true)
    public Long resolveCreatedBy(String loginName) {
        if (!isBlank(loginName)) {
            Long memberId = findMemberIdByLoginName(loginName);
            if (memberId != null) {
                return memberId;
            }
        }
        return resolveCreatedBy((Long) null);
    }

    private void validate(ProblemDTO problemDTO) {
        if (problemDTO == null) throw new IllegalArgumentException("문제 데이터가 없습니다.");
        if (isBlank(problemDTO.getTitle())) throw new IllegalArgumentException("문제 제목은 필수입니다.");
        if (isBlank(problemDTO.getCategory())) throw new IllegalArgumentException("카테고리는 필수입니다.");
        if (problemDTO.getTimeLimitMs() == null || problemDTO.getTimeLimitMs() <= 0) throw new IllegalArgumentException("시간 제한은 1 이상이어야 합니다.");
        if (problemDTO.getMemoryLimitMb() == null || problemDTO.getMemoryLimitMb() <= 0) throw new IllegalArgumentException("메모리 제한은 1 이상이어야 합니다.");
        if (isBlank(problemDTO.getShortDescription()) && isBlank(problemDTO.getDescription())) throw new IllegalArgumentException("문제 설명을 입력해 주세요.");

        List<String> sampleInputs = problemDTO.getSampleInputs();
        List<String> sampleOutputs = problemDTO.getSampleOutputs();
        if (sampleInputs == null || sampleOutputs == null || sampleInputs.isEmpty() || sampleOutputs.isEmpty()) {
            throw new IllegalArgumentException("예제 입력과 출력은 최소 1개 이상 필요합니다.");
        }
        if (sampleInputs.size() != sampleOutputs.size()) throw new IllegalArgumentException("예제 입력과 출력 개수가 맞지 않습니다.");

        List<String> testInputs = problemDTO.getTestInputs();
        List<String> testOutputs = problemDTO.getTestOutputs();
        if (testInputs != null && testOutputs != null && testInputs.size() != testOutputs.size()) {
            throw new IllegalArgumentException("테스트케이스 입력과 출력 개수가 맞지 않습니다.");
        }
    }

    private ProblemDTO toProblemDto(ProblemEntity problem) {
        ProblemDTO dto = new ProblemDTO();
        dto.setProblemId(problem.getProblemId());
        dto.setCreatedBy(problem.getCreatedBy());
        dto.setTitle(problem.getTitle());
        dto.setDescription(problem.getDescription());
        dto.setShortDescription(problem.getDescription());
        dto.setDifficulty(problem.getDifficulty().name());
        dto.setCategory(problem.getCategory());
        dto.setTimeLimitMs(problem.getTimeLimitMs());
        dto.setMemoryLimitMb(problem.getMemoryLimitMb());
        dto.setStatus(problem.getStatus().name());
        dto.setAcceptanceRate(problem.getAcceptanceRate() != null ? problem.getAcceptanceRate() : 0.0);
        dto.setSubmissionCount(problem.getSubmissionCount() != null ? problem.getSubmissionCount() : 0);
        dto.setAcceptedCount(problem.getAcceptedCount() != null ? problem.getAcceptedCount() : 0);
        return dto;
    }

    private void applyProblemFields(ProblemEntity problem, ProblemDTO problemDTO) {
        problem.setTitle(problemDTO.getTitle().trim());
        problem.setDescription(buildDescription(problemDTO));
        problem.setDifficulty(parseDifficulty(problemDTO.getDifficulty()));
        problem.setCategory(problemDTO.getCategory().trim());
        problem.setTimeLimitMs(problemDTO.getTimeLimitMs());
        problem.setMemoryLimitMb(problemDTO.getMemoryLimitMb());
        problem.setStatus(parseStatus(problemDTO.getStatus()));
        problem.setAcceptanceRate(problemDTO.getAcceptanceRate() != null ? problemDTO.getAcceptanceRate() : 0.0);
        problem.setSubmissionCount(problemDTO.getSubmissionCount() != null ? problemDTO.getSubmissionCount() : 0);
        problem.setAcceptedCount(problemDTO.getAcceptedCount() != null ? problemDTO.getAcceptedCount() : 0);
    }

    private void replaceTags(Long problemId, List<String> tags) {
        problemTagRepository.deleteByProblemId(problemId);
        if (tags == null) return;
        for (String tagName : tags) {
            if (isBlank(tagName)) continue;
            ProblemTagEntity tag = new ProblemTagEntity();
            tag.setProblemId(problemId);
            tag.setTagName(tagName.trim());
            problemTagRepository.save(tag);
        }
    }

    private void replaceTests(Long problemId, List<String> sampleInputs, List<String> sampleOutputs, List<String> testInputs, List<String> testOutputs) {
        testCaseRepository.deleteByProblemId(problemId);
        saveTests(problemId, sampleInputs, sampleOutputs, true);
        saveTests(problemId, testInputs, testOutputs, false);
    }

    private void saveTests(Long problemId, List<String> inputs, List<String> outputs, boolean isSample) {
        if (inputs == null || outputs == null) return;
        
        // Use a proxy ProblemEntity to avoid fetching it from DB
        ProblemEntity problemProxy = ProblemEntity.builder().problemId(problemId).build();
        
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String output = outputs.get(i);
            if (isBlank(input) || isBlank(output)) continue;
            TestCaseEntity testCase = new TestCaseEntity();
            testCase.setProblem(problemProxy);
            testCase.setInputData(input.trim());
            testCase.setExpectedOutput(output.trim());
            testCase.setIsSample(isSample);
            testCase.setOrderNum(i + 1);
            testCaseRepository.save(testCase);
        }
    }

    private Long resolveCreatedBy(Long createdBy) {
        if (createdBy != null) return createdBy;
        List<?> rows = entityManager.createNativeQuery("select member_id from members order by member_id limit 1").getResultList();
        if (rows.isEmpty()) throw new IllegalStateException("문제를 등록할 회원 데이터가 없습니다.");
        return toLong(rows.get(0));
    }

    private Difficulty parseDifficulty(String difficulty) {
        try {
            return Difficulty.valueOf(isBlank(difficulty) ? "EASY" : difficulty.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("난이도 값이 올바르지 않습니다.");
        }
    }

    private ProblemStatus parseStatus(String status) {
        try {
            return ProblemStatus.valueOf(isBlank(status) ? "DRAFT" : status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("문제 상태 값이 올바르지 않습니다.");
        }
    }

    private String buildDescription(ProblemDTO problemDTO) {
        if (!isBlank(problemDTO.getDescription())) return problemDTO.getDescription().trim();
        return problemDTO.getShortDescription() != null ? problemDTO.getShortDescription().trim() : "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        return value == null ? null : Long.valueOf(value.toString());
    }

    private Long findMemberIdByLoginName(String loginName) {
        List<?> rows = entityManager.createNativeQuery("select member_id from members where username = :loginName limit 1").setParameter("loginName", loginName).getResultList();
        if (!rows.isEmpty()) return toLong(rows.get(0));
        rows = entityManager.createNativeQuery("select member_id from members where email = :loginName limit 1").setParameter("loginName", loginName).getResultList();
        if (!rows.isEmpty()) return toLong(rows.get(0));
        return null;
    }

    private boolean matchesSearch(ProblemDTO problem, List<String> keywords) {
        if (keywords.isEmpty()) return true;
        String searchableText = (defaultString(problem.getTitle()) + " " + defaultString(problem.getCategory()) + " " + defaultString(problem.getDescription()) + " " + String.join(" ", loadTagNames(problem.getProblemId()))).toLowerCase();
        for (String keyword : keywords) {
            if (searchableText.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    private List<String> loadTagNames(Long problemId) {
        if (problemId == null) return Collections.emptyList();
        return problemTagRepository.findByProblemIdOrderByTagIdAsc(problemId).stream().map(ProblemTagEntity::getTagName).collect(Collectors.toList());
    }

    private List<String> expandKeywords(String query) {
        if (!hasText(query)) return Collections.emptyList();
        Set<String> expanded = new HashSet<>();
        for (String token : tokenize(query)) {
            expanded.add(token);
            expanded.addAll(relatedKeywordMap.getOrDefault(token.toLowerCase(), Collections.emptyList()));
        }
        return expanded.stream().filter(this::hasText).sorted().collect(Collectors.toList());
    }

    private List<String> tokenize(String query) {
        return Arrays.stream(query.trim().split("\\s+")).map(String::trim).filter(this::hasText).collect(Collectors.toList());
    }

    private Map<String, List<String>> createRelatedKeywordMap() {
        Map<String, List<String>> keywordMap = new LinkedHashMap<>();
        keywordMap.put("배열", List.of("array", "리스트", "arraylist"));
        keywordMap.put("array", List.of("배열", "리스트"));
        keywordMap.put("문자열", List.of("string", "text", "char"));
        keywordMap.put("string", List.of("문자열", "char", "text"));
        keywordMap.put("정렬", List.of("sort", "sorting", "ordered"));
        keywordMap.put("sort", List.of("정렬", "sorting", "ordered"));
        keywordMap.put("그래프", List.of("graph", "tree", "node"));
        keywordMap.put("graph", List.of("그래프", "tree", "node"));
        keywordMap.put("구현", List.of("implementation", "simulate", "simulation"));
        keywordMap.put("implementation", List.of("구현", "simulate", "simulation"));
        keywordMap.put("bfs", List.of("너비우선탐색", "breadth first search", "graph"));
        keywordMap.put("dfs", List.of("깊이우선탐색", "depth first search", "graph"));
        return keywordMap;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
