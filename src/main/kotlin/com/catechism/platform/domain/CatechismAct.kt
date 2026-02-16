package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "catechism_act",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["booklet_id", "act_number"])
    ]
)
data class CatechismAct(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booklet_id", nullable = false)
    val booklet: CatechismBooklet,

    @Column(name = "act_number", nullable = false)
    val actNumber: Int,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "act", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val translations: MutableList<CatechismActTranslation> = mutableListOf(),

    @OneToMany(mappedBy = "act", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val subtitles: MutableList<CatechismActSubtitle> = mutableListOf()
)
