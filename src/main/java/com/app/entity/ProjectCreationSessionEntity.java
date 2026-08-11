package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project_creation_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreationSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @Column(name = "initial_prompt", nullable = false, columnDefinition = "TEXT")
    private String initialPrompt;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "current_step")
    private Integer currentStep;

    @Column(name = "total_steps")
    private Integer totalSteps;

    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private List<ProjectQuestionEntity> questions = new ArrayList<>();

    /**
     * Custom setter that mutates the existing collection in-place instead of
     * replacing the reference, protecting Hibernate's managed PersistentBag wrapper
     * from orphanRemoval collection replacement exceptions.
     */
    public void setQuestions(List<ProjectQuestionEntity> newQuestions) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        } else {
            this.questions.clear();
        }
        if (newQuestions != null) {
            this.questions.addAll(newQuestions);
        }
    }

    public void addQuestion(ProjectQuestionEntity question) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        question.setSession(this);
        this.questions.add(question);
    }

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = "ANALYZING";
        }
        if (currentStep == null) {
            currentStep = 1;
        }
        if (totalSteps == null) {
            totalSteps = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
