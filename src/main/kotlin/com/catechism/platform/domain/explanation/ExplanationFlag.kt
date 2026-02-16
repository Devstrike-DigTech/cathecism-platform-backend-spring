package com.catechism.platform.domain.explanation

import com.catechism.platform.domain.AppUser
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "explanation_flag")
data class ExplanationFlag(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "explanation_id", nullable = false)
    val explanation: ExplanationSubmission,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flagger_id", nullable = false)
    val flagger: AppUser,

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_reason", nullable = false, length = 50)
    val flagReason: FlagReason,

    @Column(name = "flag_details", columnDefinition = "TEXT")
    val flagDetails: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_status", nullable = false, length = 20)
    var flagStatus: FlagStatus = FlagStatus.OPEN,

    // Moderator response
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    var moderator: AppUser? = null,

    @Column(name = "moderator_notes", columnDefinition = "TEXT")
    var moderatorNotes: String? = null,

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    /**
     * Resolve the flag with moderator notes
     */
    fun resolve(moderator: AppUser, notes: String, status: FlagStatus) {
        require(status in listOf(FlagStatus.RESOLVED, FlagStatus.DISMISSED)) {
            "Can only resolve to RESOLVED or DISMISSED status"
        }
        this.moderator = moderator
        this.moderatorNotes = notes
        this.flagStatus = status
        this.resolvedAt = Instant.now()
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExplanationFlag) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class FlagReason {
    INACCURATE,
    INAPPROPRIATE,
    MISLEADING,
    POOR_QUALITY,
    DUPLICATE,
    OFF_TOPIC,
    OTHER
}

enum class FlagStatus {
    OPEN,
    REVIEWED,
    RESOLVED,
    DISMISSED
}