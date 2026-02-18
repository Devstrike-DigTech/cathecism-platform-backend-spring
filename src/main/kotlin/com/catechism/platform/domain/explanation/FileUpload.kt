package com.catechism.platform.domain.explanation

import com.catechism.platform.domain.AppUser
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "file_upload")
data class FileUpload(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    val uploader: AppUser,

    @Column(name = "file_name", nullable = false)
    val fileName: String,

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    val filePath: String,

    @Column(name = "file_size_bytes", nullable = false)
    val fileSizeBytes: Long,

    @Column(name = "mime_type", nullable = false, length = 100)
    val mimeType: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", nullable = false, length = 20)
    val uploadType: FileUploadType,

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    var processingStatus: ProcessingStatus = ProcessingStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "virus_scan_status", nullable = false, length = 20)
    var virusScanStatus: VirusScanStatus = VirusScanStatus.PENDING,

    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false,

    @Column(name = "access_count", nullable = false)
    var accessCount: Int = 0,

    @Column(name = "uploaded_at", nullable = false)
    val uploadedAt: Instant = Instant.now(),

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
     * Get human-readable file size
     */
    fun getFormattedFileSize(): String {
        val kb = fileSizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$fileSizeBytes bytes"
        }
    }

    /**
     * Check if file is safe to serve
     */
    fun isSafe(): Boolean {
        return virusScanStatus == VirusScanStatus.CLEAN &&
                processingStatus != ProcessingStatus.FAILED
    }

    /**
     * Increment access count
     */
    fun recordAccess() {
        accessCount++
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileUpload) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class FileUploadType {
    AUDIO,
    VIDEO,
    IMAGE,
    DOCUMENT
}

enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

enum class VirusScanStatus {
    PENDING,
    CLEAN,
    INFECTED,
    FAILED
}