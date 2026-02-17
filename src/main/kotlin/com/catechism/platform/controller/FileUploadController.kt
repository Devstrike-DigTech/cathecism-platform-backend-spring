package com.catechism.platform.controller

import com.catechism.platform.domain.explanation.FileUpload
import com.catechism.platform.domain.explanation.FileUploadType
import com.catechism.platform.domain.explanation.ProcessingStatus
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.service.FileUploadService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/files")
class FileUploadController(
    private val fileUploadService: FileUploadService,
    private val userRepository: AppUserRepository
) {

    /**
     * Upload an audio file
     * POST /api/files/audio
     */
    @PostMapping("/audio", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun uploadAudio(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FileUploadResponse> {
        val userId = getCurrentUserId()

        val upload = fileUploadService.uploadFile(
            file = file,
            uploaderId = userId,
            uploadType = FileUploadType.AUDIO
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(upload.toResponse())
    }

    /**
     * Upload a video file
     * POST /api/files/video
     */
    @PostMapping("/video", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun uploadVideo(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FileUploadResponse> {
        val userId = getCurrentUserId()

        val upload = fileUploadService.uploadFile(
            file = file,
            uploaderId = userId,
            uploadType = FileUploadType.VIDEO
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(upload.toResponse())
    }

    /**
     * Upload an image file
     * POST /api/files/image
     */
    @PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FileUploadResponse> {
        val userId = getCurrentUserId()

        val upload = fileUploadService.uploadFile(
            file = file,
            uploaderId = userId,
            uploadType = FileUploadType.IMAGE
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(upload.toResponse())
    }

    /**
     * Get file metadata by ID
     * GET /api/files/{id}
     */
    @GetMapping("/{id}")
    fun getFileMetadata(@PathVariable id: UUID): ResponseEntity<FileUploadResponse> {
        val file = fileUploadService.getFileById(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(file.toResponse())
    }

    /**
     * Serve file content
     * GET /api/files/{id}/content
     */
    @GetMapping("/{id}/content")
    fun getFileContent(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val file = fileUploadService.getFileById(id)
            ?: return ResponseEntity.notFound().build()

        // Check if file is public or belongs to current user
        if (!file.isPublic) {
            val userId = runCatching { getCurrentUserId() }.getOrNull()
            if (userId == null || file.uploader.id != userId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }

        val content = runCatching {
            fileUploadService.getFileContent(id)
        }.getOrElse {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        } ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.mimeType))
            .contentLength(content.size.toLong())
            .body(content)
    }

    /**
     * Get current user's uploaded files
     * GET /api/files/my
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    fun getMyFiles(): ResponseEntity<List<FileUploadResponse>> {
        val userId = getCurrentUserId()
        val files = fileUploadService.getFilesByUploader(userId)
        return ResponseEntity.ok(files.map { it.toResponse() })
    }

    /**
     * Delete a file
     * DELETE /api/files/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun deleteFile(@PathVariable id: UUID): ResponseEntity<DeleteResponse> {
        val userId = getCurrentUserId()

        val file = fileUploadService.getFileById(id)
            ?: return ResponseEntity.notFound().build()

        // Only allow uploader or admin to delete
        val currentUser = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (file.uploader.id != userId && currentUser.role.name != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val deleted = fileUploadService.deleteFile(id)
        return ResponseEntity.ok(DeleteResponse(deleted, "File deleted successfully"))
    }

    /**
     * Admin: Get storage statistics
     * GET /api/files/admin/stats
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    fun getStorageStats(): ResponseEntity<StorageStatsResponse> {
        val stats = fileUploadService.getStorageStatistics()
        return ResponseEntity.ok(
            StorageStatsResponse(
                totalFiles = stats.totalFiles,
                totalSizeBytes = stats.totalSizeBytes,
                totalSizeFormatted = formatBytes(stats.totalSizeBytes),
                audioFiles = stats.audioFiles,
                audioSizeBytes = stats.audioSizeBytes,
                videoFiles = stats.videoFiles,
                videoSizeBytes = stats.videoSizeBytes,
                imageFiles = stats.imageFiles,
                imageSizeBytes = stats.imageSizeBytes
            )
        )
    }

    /**
     * Admin: Update processing status
     * PUT /api/files/{id}/processing-status
     */
    @PutMapping("/{id}/processing-status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateProcessingStatus(
        @PathVariable id: UUID,
        @RequestBody body: UpdateStatusRequest
    ): ResponseEntity<FileUploadResponse> {
        val status = runCatching {
            ProcessingStatus.valueOf(body.status)
        }.getOrElse {
            return ResponseEntity.badRequest().build()
        }

        val file = fileUploadService.updateProcessingStatus(id, status)
        return ResponseEntity.ok(file.toResponse())
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.principal as? String
            ?: throw IllegalStateException("User not authenticated")

        return userRepository.findByEmail(email)?.id
            ?: throw IllegalStateException("User not found")
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            kb >= 1 -> "%.2f KB".format(kb)
            else -> "$bytes bytes"
        }
    }

    private fun FileUpload.toResponse() = FileUploadResponse(
        id = this.id,
        fileName = this.fileName,
        fileSizeBytes = this.fileSizeBytes,
        fileSizeFormatted = this.getFormattedFileSize(),
        mimeType = this.mimeType,
        uploadType = this.uploadType.name,
        processingStatus = this.processingStatus.name,
        virusScanStatus = this.virusScanStatus.name,
        isPublic = this.isPublic,
        uploadedAt = this.uploadedAt.toString(),
        contentUrl = "/api/files/${this.id}/content"
    )
}

// =====================================================
// Response DTOs
// =====================================================

data class FileUploadResponse(
    val id: UUID,
    val fileName: String,
    val fileSizeBytes: Long,
    val fileSizeFormatted: String,
    val mimeType: String,
    val uploadType: String,
    val processingStatus: String,
    val virusScanStatus: String,
    val isPublic: Boolean,
    val uploadedAt: String,
    val contentUrl: String
)

data class DeleteResponse(
    val success: Boolean,
    val message: String
)

data class StorageStatsResponse(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val totalSizeFormatted: String,
    val audioFiles: Int,
    val audioSizeBytes: Long,
    val videoFiles: Int,
    val videoSizeBytes: Long,
    val imageFiles: Int,
    val imageSizeBytes: Long
)

data class UpdateStatusRequest(
    val status: String
)