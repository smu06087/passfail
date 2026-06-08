package com.passfail.problem.service;

import com.passfail.entity.MemberEntity;
import com.passfail.entity.ProblemEntity;
import com.passfail.entity.ProblemTagEntity;
import com.passfail.entity.SolvedProblemEntity;
import com.passfail.entity.SubmissionEntity;
import com.passfail.entity.TestCaseEntity;
import com.passfail.enums.Difficulty;
import com.passfail.enums.ProblemStatus;
import com.passfail.enums.ProgrammingLanguage;
import com.passfail.enums.SubmissionStatus;
import com.passfail.enums.Tier;
import com.passfail.member.repository.MemberRepository;
import com.passfail.problem.dto.ProblemDTO;
import com.passfail.problem.dto.ProblemResponse;
import com.passfail.problem.dto.TestCaseResponse;
import com.passfail.problem.repository.ProblemRepository;
import com.passfail.problem.repository.ProblemTagRepository;
import com.passfail.problem.repository.SolvedProblemRepository;
import com.passfail.problem.repository.SubmissionRepository;
import com.passfail.problem.repository.TestCaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<ProblemResponse> getActiveProblems() {
        return problemRepository.findByStatus(ProblemStatus.PUBLISHED).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public ProblemResponse getProblemResponse(Long id) {
        ProblemEntity problem = problemRepository.findByIdWithTestCases(id)
            .orElseThrow(() -> new EntityNotFoundException("Problem not found."));
        return convertToResponse(problem);
    }

    public ProblemEntity getProblemEntity(Long id) {
        return problemRepository.findByIdWithTestCases(id)
            .orElseThrow(() -> new EntityNotFoundException("Problem not found."));
    }

    @Transactional
    public SubmissionEntity recordSubmission(String username, Long problemId, String code, ProgrammingLanguage language, SubmissionStatus status) {
        return recordSubmission(username, problemId, code, language, status, 0, 0);
    }

    @Transactional
    public SubmissionEntity recordSubmission(String username, Long problemId, String code, ProgrammingLanguage language, SubmissionStatus status, Integer timeMs, Integer memoryKb) {
        MemberEntity member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("Member not found."));
        
        SubmissionEntity submission = SubmissionEntity.builder()
                .memberId(member.getMemberId())
                .problemId(problemId)
                .code(code)
                .language(language)
                .status(status)
                .executionTimeMs(timeMs != null ? timeMs : 0)
                .memoryUsedKb(memoryKb != null ? memoryKb : 0)
                .build();

        submission = submissionRepository.save(submission);
        
        finalizeSubmission(submission, status);
        
        return submission;
    }

    /**
     * OnDocker: 이미 저장된 SubmissionEntity의 결과를 업데이트하고 관련 통계/점수를 처리합니다.
     */
    @Transactional
    public void recordSubmissionResult(Long submissionId, SubmissionStatus status, int timeMs, int memoryKb) {
        SubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found: " + submissionId));

        submission.setStatus(status);
        submission.setExecutionTimeMs(timeMs);
        submission.setMemoryUsedKb(memoryKb);
        submissionRepository.save(submission);

        finalizeSubmission(submission, status);
    }

    private void finalizeSubmission(SubmissionEntity submission, SubmissionStatus status) {
        ProblemEntity problem = problemRepository.findById(submission.getProblemId())
                .orElseThrow(() -> new EntityNotFoundException("Problem not found."));
        MemberEntity member = memberRepository.findById(submission.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Member not found."));

        // 문제의 총 제출 수 증가 (성공/실패 여부 상관없이)
        problem.setSubmissionCount((problem.getSubmissionCount() != null ? problem.getSubmissionCount() : 0) + 1);

        if (status == SubmissionStatus.ACCEPTED) {
            // 정답인 경우에만 해결 문제로 기록 및 점수 부여
            solvedProblemRepository.findByMemberId(member.getMemberId()).stream()
                .filter(sp -> sp.getProblemId().equals(problem.getProblemId()))
                .findFirst()
                .ifPresentOrElse(
                    sp -> sp.setTryCount(sp.getTryCount() + 1),
                    () -> {
                        int score = 100;
                        SolvedProblemEntity solvedProblem = SolvedProblemEntity.builder()
                            .memberId(member.getMemberId())
                            .problemId(problem.getProblemId())
                            .scoreEarned(score)
                            .tryCount(1)
                            .build();
                        solvedProblemRepository.save(solvedProblem);

                        int currentScore = member.getTotalScore() != null ? member.getTotalScore() : 0;
                        member.setTotalScore(currentScore + score);
                        member.setTier(Tier.fromScore(member.getTotalScore()));
                        memberRepository.save(member);
                    }
                );

            problem.setAcceptedCount((problem.getAcceptedCount() != null ? problem.getAcceptedCount() : 0) + 1);
        } else {
            // 틀린 경우에도 시도 횟수는 증가시킴
            solvedProblemRepository.findByMemberId(member.getMemberId()).stream()
                .filter(sp -> sp.getProblemId().equals(problem.getProblemId()))
                .findFirst()
                .ifPresent(sp -> sp.setTryCount(sp.getTryCount() + 1));
        }

        // 정답률 업데이트
        double subCount = problem.getSubmissionCount();
        double accCount = problem.getAcceptedCount() != null ? problem.getAcceptedCount() : 0;
        problem.setAcceptanceRate(subCount > 0 ? (accCount / subCount * 100.0) : 0.0);
        problemRepository.save(problem);
    }

    @Transactional(readOnly = true)
    public SubmissionEntity getPreviousSolution(String username, Long problemId) {
        MemberEntity member = memberRepository.findByUsername(username).orElse(null);
        if (member == null) return null;

        return submissionRepository.findFirstByMemberIdAndProblemIdOrderBySubmittedAtDesc(member.getMemberId(), problemId)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isSolved(String username, Long problemId) {
        MemberEntity member = memberRepository.findByUsername(username).orElse(null);
        if (member == null) return false;

        return solvedProblemRepository.findByMemberId(member.getMemberId()).stream()
            .anyMatch(sp -> sp.getProblemId().equals(problemId));
    }

    /**
     * 배틀 모드(ROGUE)용 결정론적 문제 선택 로직
     */
    @Transactional(readOnly = true)
    public ProblemResponse getBattleProblem(Long roomId, int floor, Long seed, String nodeId) {
        Difficulty targetDifficulty;
        if (floor < 3) targetDifficulty = Difficulty.EASY;
        else if (floor < 7) targetDifficulty = Difficulty.MEDIUM;
        else targetDifficulty = Difficulty.HARD;

        List<ProblemEntity> problems = problemRepository.findByDifficulty(targetDifficulty).stream()
            .filter(p -> p.getStatus() == ProblemStatus.PUBLISHED)
            .toList();

        if (problems.isEmpty()) {
            problems = problemRepository.findByStatus(ProblemStatus.PUBLISHED);
        }

        if (problems.isEmpty()) {
            throw new EntityNotFoundException("No problems available for battle.");
        }

        // 시드 기반 결정론적 선택 (모든 참가자가 동일한 문제를 받도록 함)
        long combinedSeed = seed + roomId + floor + (nodeId != null ? nodeId.hashCode() : 0);
        com.passfail.battle.util.Mulberry32 random = com.passfail.battle.util.Mulberry32.getInstance(combinedSeed, true);
        int index = random.getRandomInt(0, problems.size());

        return convertToResponse(problems.get(index));
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
            .orElseThrow(() -> new EntityNotFoundException("Problem not found. id=" + problemId));

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

    @Transactional
    public void deleteProblem(Long problemId) {
        ProblemEntity problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new EntityNotFoundException("Problem not found. id=" + problemId));

        problemRepository.delete(problem);
    }

    @Transactional(readOnly = true)
    public List<ProblemDTO> getProblemList() {
        return getProblemList(true);
    }

    @Transactional(readOnly = true)
    public List<ProblemDTO> getProblemList(boolean includeDrafts) {
        return getProblemList(includeDrafts, null);
    }

    @Transactional(readOnly = true)
    public List<ProblemDTO> getProblemList(boolean includeDrafts, String loginName) {
        List<ProblemEntity> problems = problemRepository.findAll().stream()
            .filter(problem -> includeDrafts || problem.getStatus() == ProblemStatus.PUBLISHED)
            .sorted((left, right) -> Long.compare(right.getProblemId(), left.getProblemId()))
            .collect(Collectors.toList());

        log.debug("Loaded {} problems via JPA. First IDs: {}", problems.size(),
            problems.stream().limit(5).map(ProblemEntity::getProblemId).collect(Collectors.toList()));

        Set<Long> solvedProblemIds = findSolvedProblemIds(loginName);
        return problems.stream()
            .map(problem -> toProblemDto(problem, solvedProblemIds.contains(problem.getProblemId())))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProblemDTO getProblemDetail(Long problemId) {
        ProblemEntity problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new EntityNotFoundException("Problem not found. id=" + problemId));

        ProblemDTO dto = toProblemDto(problem);
        dto.setTags(problemTagRepository.findByProblemIdOrderByTagIdAsc(problemId).stream()
            .map(ProblemTagEntity::getTagName)
            .collect(Collectors.toList()));

        List<TestCaseEntity> sampleTests = testCaseRepository.findByProblem_ProblemIdAndIsSampleTrueOrderByOrderNumAsc(problemId);
        dto.setSampleInputs(sampleTests.stream().map(TestCaseEntity::getInputData).collect(Collectors.toList()));
        dto.setSampleOutputs(sampleTests.stream().map(TestCaseEntity::getExpectedOutput).collect(Collectors.toList()));

        List<TestCaseEntity> testCases = testCaseRepository.findByProblem_ProblemIdAndIsSampleFalseOrderByOrderNumAsc(problemId);
        dto.setTestInputs(testCases.stream().map(TestCaseEntity::getInputData).collect(Collectors.toList()));
        dto.setTestOutputs(testCases.stream().map(TestCaseEntity::getExpectedOutput).collect(Collectors.toList()));
        return dto;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProblemDebugInfo() {
        Map<String, Object> debugInfo = new LinkedHashMap<>();
        List<ProblemEntity> problems = problemRepository.findAll();

        debugInfo.put("jpaCount", problems.size());
        debugInfo.put("jpaFirstIds", problems.stream().limit(5).map(ProblemEntity::getProblemId).collect(Collectors.toList()));

        Object nativeCount = entityManager.createNativeQuery("select count(*) from problem").getSingleResult();
        Object currentUser = entityManager.createNativeQuery("select user from dual").getSingleResult();

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
        return searchProblems(query, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchProblems(String query, boolean includeDrafts) {
        return searchProblems(query, includeDrafts, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchProblems(String query, boolean includeDrafts, String loginName) {
        List<String> expandedKeywords = expandKeywords(query);
        List<ProblemDTO> problems = getProblemList(includeDrafts, loginName);

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
        if (problemDTO == null) throw new IllegalArgumentException("Problem data is required.");

        boolean draft = isDraft(problemDTO);
        if (!draft) {
            if (isBlank(problemDTO.getTitle())) throw new IllegalArgumentException("Title is required.");
            if (isBlank(problemDTO.getCategory())) throw new IllegalArgumentException("Category is required.");
            if (problemDTO.getTimeLimitMs() == null || problemDTO.getTimeLimitMs() <= 0) throw new IllegalArgumentException("Time limit must be greater than 0.");
            if (problemDTO.getMemoryLimitMb() == null || problemDTO.getMemoryLimitMb() <= 0) throw new IllegalArgumentException("Memory limit must be greater than 0.");
            if (isBlank(problemDTO.getDescription())) throw new IllegalArgumentException("Description is required.");
        }

        List<String> sampleInputs = problemDTO.getSampleInputs();
        List<String> sampleOutputs = problemDTO.getSampleOutputs();
        if (!draft && (sampleInputs == null || sampleOutputs == null || sampleInputs.isEmpty() || sampleOutputs.isEmpty())) {
            throw new IllegalArgumentException("At least one sample input/output is required.");
        }
        if (sampleInputs != null && sampleOutputs != null && sampleInputs.size() != sampleOutputs.size()) {
            throw new IllegalArgumentException("Sample input and output counts must match.");
        }

        List<String> testInputs = problemDTO.getTestInputs();
        List<String> testOutputs = problemDTO.getTestOutputs();
        if (testInputs != null && testOutputs != null && testInputs.size() != testOutputs.size()) {
            throw new IllegalArgumentException("Test input and output counts must match.");
        }
    }

    private ProblemDTO toProblemDto(ProblemEntity problem) {
        return toProblemDto(problem, false);
    }

    private ProblemDTO toProblemDto(ProblemEntity problem, boolean solved) {
        ProblemDTO dto = new ProblemDTO();
        dto.setProblemId(problem.getProblemId());
        dto.setCreatedBy(problem.getCreatedBy());
        dto.setTitle(problem.getTitle());
        dto.setDescription(problem.getDescription());
        dto.setDifficulty(problem.getDifficulty().name());
        dto.setCategory(problem.getCategory());
        dto.setTimeLimitMs(problem.getTimeLimitMs());
        dto.setMemoryLimitMb(problem.getMemoryLimitMb());
        dto.setStatus(problem.getStatus().name());
        dto.setAcceptanceRate(problem.getAcceptanceRate() != null ? problem.getAcceptanceRate() : 0.0);
        dto.setSubmissionCount(problem.getSubmissionCount() != null ? problem.getSubmissionCount() : 0);
        dto.setAcceptedCount(problem.getAcceptedCount() != null ? problem.getAcceptedCount() : 0);
        dto.setSolved(solved);
        dto.setCreatedAt(problem.getCreatedAt());
        return dto;
    }

    private void applyProblemFields(ProblemEntity problem, ProblemDTO problemDTO) {
        ProblemStatus status = parseStatus(problemDTO.getStatus());
        boolean draft = status == ProblemStatus.DRAFT;
        problem.setTitle(normalizeTitle(problemDTO.getTitle(), draft));
        problem.setDescription(buildDescription(problemDTO, draft));
        problem.setDifficulty(parseDifficulty(problemDTO.getDifficulty()));
        problem.setCategory(normalizeCategory(problemDTO.getCategory(), draft));
        problem.setTimeLimitMs(normalizePositiveInteger(problemDTO.getTimeLimitMs(), 2000));
        problem.setMemoryLimitMb(normalizePositiveInteger(problemDTO.getMemoryLimitMb(), 256));
        problem.setStatus(status);
        if (problem.getAcceptanceRate() == null) {
            problem.setAcceptanceRate(problemDTO.getAcceptanceRate() != null ? problemDTO.getAcceptanceRate() : 0.0);
        }
        if (problem.getSubmissionCount() == null) {
            problem.setSubmissionCount(problemDTO.getSubmissionCount() != null ? problemDTO.getSubmissionCount() : 0);
        }
        if (problem.getAcceptedCount() == null) {
            problem.setAcceptedCount(problemDTO.getAcceptedCount() != null ? problemDTO.getAcceptedCount() : 0);
        }
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
        testCaseRepository.deleteByProblem_ProblemId(problemId);
        saveTests(problemId, sampleInputs, sampleOutputs, true);
        saveTests(problemId, testInputs, testOutputs, false);
    }

    private void saveTests(Long problemId, List<String> inputs, List<String> outputs, boolean isSample) {
        if (inputs == null || outputs == null) return;

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
        return memberRepository.findTopByOrderByMemberIdAsc()
            .map(MemberEntity::getMemberId)
            .orElseThrow(() -> new IllegalStateException("No author account available for problem creation."));
    }

    private Set<Long> findSolvedProblemIds(String loginName) {
        if (isBlank(loginName)) {
            return Collections.emptySet();
        }

        Long memberId = findMemberIdByLoginName(loginName);
        if (memberId == null) {
            return Collections.emptySet();
        }

        return solvedProblemRepository.findByMemberId(memberId).stream()
            .filter(solvedProblem -> solvedProblem.getProblem() != null)
            .map(solvedProblem -> solvedProblem.getProblem().getProblemId())
            .collect(Collectors.toSet());
    }

    private Difficulty parseDifficulty(String difficulty) {
        try {
            return Difficulty.valueOf(isBlank(difficulty) ? "EASY" : difficulty.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid difficulty value.");
        }
    }

    private ProblemStatus parseStatus(String status) {
        try {
            return ProblemStatus.valueOf(isBlank(status) ? "DRAFT" : status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid problem status value.");
        }
    }

    private String buildDescription(ProblemDTO problemDTO, boolean draft) {
        if (!isBlank(problemDTO.getDescription())) return problemDTO.getDescription().trim();
        return draft ? " " : "";
    }

    private boolean isDraft(ProblemDTO problemDTO) {
        return parseStatus(problemDTO.getStatus()) == ProblemStatus.DRAFT;
    }

    private String normalizeTitle(String title, boolean draft) {
        if (!isBlank(title)) return title.trim();
        return draft ? "[DRAFT]" : "";
    }

    private String normalizeCategory(String category, boolean draft) {
        if (!isBlank(category)) return category.trim();
        return draft ? "UNCATEGORIZED" : "";
    }

    private Integer normalizePositiveInteger(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        return value == null ? null : Long.valueOf(value.toString());
    }

    private Long findMemberIdByLoginName(String loginName) {
        return memberRepository.findByUsername(loginName)
            .map(MemberEntity::getMemberId)
            .or(() -> memberRepository.findByEmail(loginName).map(MemberEntity::getMemberId))
            .orElse(null);
    }

    private boolean matchesSearch(ProblemDTO problem, List<String> keywords) {
        if (keywords.isEmpty()) return true;
        String searchableText = (
            defaultString(problem.getTitle()) + " " +
            defaultString(problem.getCategory()) + " " +
            defaultString(problem.getDescription()) + " " +
            String.join(" ", loadTagNames(problem.getProblemId()))
        ).toLowerCase();

        for (String keyword : keywords) {
            if (searchableText.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    private List<String> loadTagNames(Long problemId) {
        if (problemId == null) return Collections.emptyList();
        return problemTagRepository.findByProblemIdOrderByTagIdAsc(problemId).stream()
            .map(ProblemTagEntity::getTagName)
            .collect(Collectors.toList());
    }

    private List<String> expandKeywords(String query) {
        if (!hasText(query)) return Collections.emptyList();
        Set<String> expanded = new java.util.HashSet<>();
        for (String token : tokenize(query)) {
            expanded.add(token);
            expanded.addAll(relatedKeywordMap.getOrDefault(token.toLowerCase(), Collections.emptyList()));
        }
        return expanded.stream().filter(this::hasText).sorted().collect(Collectors.toList());
    }

    private List<String> tokenize(String query) {
        return Arrays.stream(query.trim().split("\\s+"))
            .map(String::trim)
            .filter(this::hasText)
            .collect(Collectors.toList());
    }

    private Map<String, List<String>> createRelatedKeywordMap() {
        Map<String, List<String>> keywordMap = new LinkedHashMap<>();
        keywordMap.put("array", List.of("list", "arraylist"));
        keywordMap.put("list", List.of("array", "arraylist"));
        keywordMap.put("string", List.of("text", "char"));
        keywordMap.put("text", List.of("string", "char"));
        keywordMap.put("sort", List.of("sorting", "ordered"));
        keywordMap.put("sorting", List.of("sort", "ordered"));
        keywordMap.put("graph", List.of("tree", "node"));
        keywordMap.put("tree", List.of("graph", "node"));
        keywordMap.put("implementation", List.of("simulate", "simulation"));
        keywordMap.put("simulation", List.of("implementation", "simulate"));
        keywordMap.put("bfs", List.of("breadth first search", "graph"));
        keywordMap.put("dfs", List.of("depth first search", "graph"));
        return keywordMap;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
