package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "catechism_act_subtitle",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["act_id", "subtitle_number"])
    ]
)
data class CatechismActSubtitle(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "act_id", nullable = false)
    val act: CatechismAct,

    @Column(name = "subtitle_number", nullable = false)
    val subtitleNumber: Int,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "subtitle", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val translations: MutableList<CatechismActSubtitleTranslation> = mutableListOf(),

    @OneToMany(mappedBy = "subtitle", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val questions: MutableList<CatechismQuestion> = mutableListOf()
)
