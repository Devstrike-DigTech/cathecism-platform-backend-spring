package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "question_ccc_reference",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["question_id", "ccc_id"])
    ]
)
data class QuestionCCCReference(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    val question: CatechismQuestion,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ccc_id", nullable = false)
    val cccParagraph: CCCParagraph,

    @Column(name = "reference_order", nullable = false)
    var referenceOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
