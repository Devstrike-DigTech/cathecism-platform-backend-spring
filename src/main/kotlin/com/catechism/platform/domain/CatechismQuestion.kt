package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "catechism_question",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["booklet_id", "question_number"])
    ]
)
data class CatechismQuestion(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booklet_id", nullable = false)
    val booklet: CatechismBooklet,

    @Column(name = "question_number", nullable = false)
    val questionNumber: Int,

    @Column
    var category: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtitle_id")
    var subtitle: CatechismActSubtitle? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val translations: MutableList<CatechismQuestionTranslation> = mutableListOf(),

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val cccReferences: MutableList<QuestionCCCReference> = mutableListOf(),

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val bibleReferences: MutableList<QuestionBibleReference> = mutableListOf()
)
