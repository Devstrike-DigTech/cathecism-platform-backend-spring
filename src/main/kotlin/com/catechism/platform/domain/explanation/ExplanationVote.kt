package com.catechism.platform.domain.explanation

import com.catechism.platform.domain.AppUser
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "explanation_vote",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["explanation_id", "user_id"])
    ]
)
data class ExplanationVote(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "explanation_id", nullable = false)
    val explanation: ExplanationSubmission,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: AppUser,

    @Column(name = "is_helpful", nullable = false)
    val isHelpful: Boolean,

    @Column(name = "vote_comment", columnDefinition = "TEXT")
    val voteComment: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExplanationVote) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}