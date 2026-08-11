package com.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ProjectCreationSessionEntity session;

    @Column(name = "question_key", nullable = false, length = 100)
    private String questionKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "selected_option", length = 100)
    private String selectedOption;

    @Column(name = "custom_value", columnDefinition = "TEXT")
    private String customValue;

    @Column(name = "is_recommended")
    private Boolean isRecommended;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (orderIndex == null) {
            orderIndex = 0;
        }
        if (isRecommended == null) {
            isRecommended = false;
        }
    }
}
