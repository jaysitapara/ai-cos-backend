package com.app.service;

import com.app.entity.ProjectCreationSessionEntity;
import com.app.entity.ProjectEntity;
import com.app.entity.ProjectQuestionEntity;
import com.app.entity.UserEntity;
import com.app.exception.UnauthorizedException;
import com.app.repository.ProjectCreationSessionRepository;
import com.app.repository.ProjectQuestionRepository;
import com.app.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Requirement Question Engine Test Suite
 *
 * Covers:
 *  - LazyInitializationException regression (Issue 2)
 *  - Answer save correctness and question ordering (Issue 3)
 *  - Duplicate request prevention (Issue 4)
 *  - User ownership isolation (Issue 9)
 *  - No N+1 queries on session load
 *  - Correct step progression
 *  - REVIEW_READY transition
 */
@DisplayName("Requirement Question Engine Tests")
class RequirementQuestionEngineTest {

    @Mock private TechnologyCatalogService catalogService;
    @Mock private AIService aiService;
    @Mock private ProjectCreationSessionRepository sessionRepository;
    @Mock private ProjectQuestionRepository questionRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private RequirementQuestionEngine engine;

    private UserEntity userA;
    private UserEntity userB;
    private UUID sessionPublicId;
    private ProjectCreationSessionEntity session;
    private List<ProjectQuestionEntity> questions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use real ObjectMapper — no need to mock JSON serialization
        org.springframework.test.util.ReflectionTestUtils.setField(engine, "objectMapper", new ObjectMapper());

        userA = UserEntity.builder().id(1L).email("alice@example.com").build();
        userB = UserEntity.builder().id(2L).email("bob@example.com").build();
        sessionPublicId = UUID.randomUUID();

        questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            questions.add(ProjectQuestionEntity.builder()
                .id((long) i)
                .questionKey("key" + i)
                .title("Question " + i)
                .description("Desc " + i)
                .category("GENERAL")
                .optionsJson("[]")
                .selectedOption("option" + i)
                .status("PENDING")
                .orderIndex(i)
                .isRecommended(false)
                .recommendationReason("")
                .build());
        }

        session = ProjectCreationSessionEntity.builder()
            .id(10L)
            .publicId(sessionPublicId)
            .user(userA)
            .initialPrompt("build something")
            .status("IN_PROGRESS")
            .currentStep(1)
            .totalSteps(5)
            .questions(questions)    // pre-populated, simulating JOIN FETCH result
            .build();
    }

    // =========================================================================
    // REGRESSION: LazyInitializationException (Issue 2)
    // =========================================================================

    @Test
    @DisplayName("Regression: getSession returns session with questions already initialized (JOIN FETCH)")
    void getSessionReturnsQuestionsEagerlyLoaded() {
        // findByPublicIdWithQuestions is the JOIN FETCH variant — used instead of findByPublicId
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));

        ProjectCreationSessionEntity result = engine.getSession(sessionPublicId, userA);

        assertNotNull(result);
        // Questions list must be accessible without a Hibernate session open
        // (simulated here since we pre-populate the list — in production this comes from JOIN FETCH)
        List<ProjectQuestionEntity> qs = result.getQuestions();
        assertNotNull(qs, "Questions must not be null — LazyInitializationException regression");
        assertEquals(5, qs.size(), "All 5 questions must be loaded");

        // The repository method used must be findByPublicIdWithQuestions (JOIN FETCH)
        verify(sessionRepository, times(1)).findByPublicIdWithQuestions(sessionPublicId);
        verify(sessionRepository, never()).findByPublicId(any());
    }

    @Test
    @DisplayName("Regression: saveAnswers accesses questions list without LazyInitializationException")
    void saveAnswersLoadsQuestionsWithoutLazyException() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));
        when(questionRepository.findBySessionIdOrderByOrderIndexAsc(10L))
            .thenReturn(new ArrayList<>(questions));
        when(sessionRepository.save(any())).thenReturn(session);

        Map<String, String> answers = Map.of("key1", "react", "key2", "springboot");

        // Must NOT throw LazyInitializationException
        assertDoesNotThrow(() -> {
            ProjectCreationSessionEntity saved = engine.saveAnswers(sessionPublicId, userA, answers, null);
            // Questions must be returned and accessible
            assertNotNull(saved.getQuestions(), "Questions must be accessible after saveAnswers()");
        });
    }

    @Test
    @DisplayName("ProjectCreationSessionEntity: setQuestions mutates existing list reference in-place for Hibernate safety")
    void setQuestionsMutatesInPlace() {
        ProjectCreationSessionEntity s = new ProjectCreationSessionEntity();
        List<ProjectQuestionEntity> originalList = s.getQuestions();
        assertNotNull(originalList);

        List<ProjectQuestionEntity> newQuestions = List.of(
            ProjectQuestionEntity.builder().questionKey("test").build()
        );

        s.setQuestions(newQuestions);

        // Reference MUST NOT change (preserves PersistentBag for Hibernate)
        assertSame(originalList, s.getQuestions(), "List reference must be preserved across setQuestions calls");
        assertEquals(1, s.getQuestions().size());
        assertEquals("test", s.getQuestions().get(0).getQuestionKey());
    }

    // =========================================================================
    // Answer Persistence (Issue 3)
    // =========================================================================

    @Test
    @DisplayName("saveAnswers: persists correct answer values to question entities")
    void saveAnswersPersistsCorrectValues() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));
        List<ProjectQuestionEntity> mutableQuestions = new ArrayList<>(questions);
        when(questionRepository.findBySessionIdOrderByOrderIndexAsc(10L))
            .thenReturn(mutableQuestions);
        when(questionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepository.save(any())).thenReturn(session);

        Map<String, String> answers = Map.of("key1", "nextjs", "key3", "mongodb");

        engine.saveAnswers(sessionPublicId, userA, answers, null);

        ProjectQuestionEntity q1 = mutableQuestions.stream().filter(q -> "key1".equals(q.getQuestionKey())).findFirst().orElseThrow();
        ProjectQuestionEntity q3 = mutableQuestions.stream().filter(q -> "key3".equals(q.getQuestionKey())).findFirst().orElseThrow();
        ProjectQuestionEntity q2 = mutableQuestions.stream().filter(q -> "key2".equals(q.getQuestionKey())).findFirst().orElseThrow();

        assertEquals("nextjs", q1.getSelectedOption(), "Answer for key1 must be saved");
        assertEquals("ANSWERED", q1.getStatus(), "key1 status must be ANSWERED");
        assertEquals("mongodb", q3.getSelectedOption(), "Answer for key3 must be saved");
        assertEquals("ANSWERED", q3.getStatus(), "key3 status must be ANSWERED");
        assertEquals("PENDING", q2.getStatus(), "Unanswered key2 must remain PENDING");
    }

    @Test
    @DisplayName("saveAnswers: custom value is persisted when provided")
    void saveAnswersPersistsCustomValue() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));
        List<ProjectQuestionEntity> mutableQuestions = new ArrayList<>(questions);
        when(questionRepository.findBySessionIdOrderByOrderIndexAsc(10L))
            .thenReturn(mutableQuestions);
        when(questionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepository.save(any())).thenReturn(session);

        Map<String, String> answers = Map.of("key2", "other");
        Map<String, String> customs = Map.of("key2", "MyCustomFramework");

        engine.saveAnswers(sessionPublicId, userA, answers, customs);

        ProjectQuestionEntity q2 = mutableQuestions.stream().filter(q -> "key2".equals(q.getQuestionKey())).findFirst().orElseThrow();
        assertEquals("other", q2.getSelectedOption());
        assertEquals("MyCustomFramework", q2.getCustomValue(), "Custom value must be persisted");
    }

    // =========================================================================
    // Step Progression (Issue 3)
    // =========================================================================

    @Test
    @DisplayName("saveAnswers: currentStep increments correctly after 2 answers")
    void stepIncrementsByAnsweredCount() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));
        List<ProjectQuestionEntity> mutableQuestions = new ArrayList<>(questions);
        when(questionRepository.findBySessionIdOrderByOrderIndexAsc(10L))
            .thenReturn(mutableQuestions);
        when(questionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepository.save(any())).thenAnswer(i -> {
            ProjectCreationSessionEntity s = i.getArgument(0);
            return s;
        });

        Map<String, String> answers = Map.of("key1", "react", "key2", "springboot");

        ProjectCreationSessionEntity result = engine.saveAnswers(sessionPublicId, userA, answers, null);

        // 2 answered → currentStep = min(2+1, 5) = 3
        assertEquals(3, result.getCurrentStep(), "currentStep must be answeredCount + 1");
    }

    @Test
    @DisplayName("saveAnswers: sets REVIEW_READY when all questions answered")
    void setsReviewReadyWhenAllAnswered() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));
        List<ProjectQuestionEntity> mutableQuestions = new ArrayList<>(questions);
        when(questionRepository.findBySessionIdOrderByOrderIndexAsc(10L))
            .thenReturn(mutableQuestions);
        when(questionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("key1", "react");
        answers.put("key2", "springboot");
        answers.put("key3", "postgresql");
        answers.put("key4", "jwt");
        answers.put("key5", "tailwind");

        ProjectCreationSessionEntity result = engine.saveAnswers(sessionPublicId, userA, answers, null);

        assertEquals("REVIEW_READY", result.getStatus(), "Status must be REVIEW_READY when all answered");
    }

    // =========================================================================
    // User Ownership Isolation (Issue 9)
    // =========================================================================

    @Test
    @DisplayName("getSession: throws UnauthorizedException when accessed by wrong user")
    void getSessionRejectsWrongUser() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session)); // session belongs to userA

        assertThrows(UnauthorizedException.class,
            () -> engine.getSession(sessionPublicId, userB),
            "User B must not access User A's session");
    }

    @Test
    @DisplayName("saveAnswers: throws UnauthorizedException when wrong user tries to save answers")
    void saveAnswersRejectsWrongUser() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session)); // session belongs to userA

        assertThrows(UnauthorizedException.class,
            () -> engine.saveAnswers(sessionPublicId, userB, Map.of("key1", "react"), null),
            "User B must not save answers to User A's session");
    }

    @Test
    @DisplayName("finalizeProjectConfiguration: throws UnauthorizedException when wrong user finalizes")
    void finalizeRejectsWrongUser() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));

        assertThrows(UnauthorizedException.class,
            () -> engine.finalizeProjectConfiguration(sessionPublicId, userB),
            "User B must not finalize User A's session");
    }

    // =========================================================================
    // finalizeProjectConfiguration (Issue 3 — Final Review)
    // =========================================================================

    @Test
    @DisplayName("finalizeProjectConfiguration: creates project with correct tech stack from answers")
    void finalizationCreatesProjectWithTechStack() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));

        // Return questions with answers set
        List<ProjectQuestionEntity> answeredQuestions = new ArrayList<>(questions);
        answeredQuestions.get(0).setSelectedOption("nextjs");
        answeredQuestions.get(1).setSelectedOption("springboot");
        answeredQuestions.get(2).setSelectedOption("postgresql");
        when(questionRepository.findBySessionIdOrderByOrderIndexAsc(10L))
            .thenReturn(answeredQuestions);

        ProjectEntity savedProject = ProjectEntity.builder()
            .publicId(UUID.randomUUID())
            .name("build something")
            .status("QUEUED")
            .build();
        when(projectRepository.save(any())).thenReturn(savedProject);
        when(sessionRepository.save(any())).thenReturn(session);

        ProjectEntity result = engine.finalizeProjectConfiguration(sessionPublicId, userA);

        assertNotNull(result);
        assertEquals("QUEUED", result.getStatus());
        verify(projectRepository, times(1)).save(any(ProjectEntity.class));
        verify(sessionRepository, times(1)).save(session);
    }

    // =========================================================================
    // No duplicate writes (Issue 4)
    // =========================================================================

    @Test
    @DisplayName("saveAnswers: questionRepository.saveAll called exactly once per invocation")
    void saveAnswersSavesExactlyOnce() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));
        when(questionRepository.findBySessionIdOrderByOrderIndexAsc(10L))
            .thenReturn(new ArrayList<>(questions));
        when(questionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepository.save(any())).thenReturn(session);

        engine.saveAnswers(sessionPublicId, userA, Map.of("key1", "react"), null);

        verify(questionRepository, times(1)).saveAll(any());
        verify(sessionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("getSession: uses findByPublicIdWithQuestions — never findByPublicId to prevent N+1")
    void getSessionUsesJoinFetchQueryNotSimpleLookup() {
        when(sessionRepository.findByPublicIdWithQuestions(sessionPublicId))
            .thenReturn(Optional.of(session));

        engine.getSession(sessionPublicId, userA);

        verify(sessionRepository, times(1)).findByPublicIdWithQuestions(sessionPublicId);
        verify(sessionRepository, never()).findByPublicId(sessionPublicId);
    }
}
