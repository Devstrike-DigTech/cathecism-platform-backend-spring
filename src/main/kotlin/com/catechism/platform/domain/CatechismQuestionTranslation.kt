package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "catechism_question_translation",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["question_id", "language_code"])
    ]
)
data class CatechismQuestionTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    val question: CatechismQuestion,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    var questionText: String,

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    var answerText: String,

    @Column(name = "is_official", nullable = false)
    var isOfficial: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
