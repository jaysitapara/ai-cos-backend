package com.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "content_question_states")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentQuestionStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private ContentThreadEntity thread;

    @Column(name = "question_key", nullable = false, length = 100)
    private String questionKey;

    @Column(name = "question_title", nullable = false, length = 255)
    private String questionTitle;

    @Column(name = "selected_option", length = 100)
    private String selectedOption;

    @Column(name = "custom_value", columnDefinition = "TEXT")
    private String customValue;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
        if (orderIndex == null) orderIndex = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
