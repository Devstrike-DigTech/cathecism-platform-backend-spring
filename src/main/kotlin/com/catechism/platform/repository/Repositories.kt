package com.catechism.platform.repository

import com.catechism.platform.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AppUserRepository : JpaRepository<AppUser, UUID> {
    fun findByEmail(email: String): AppUser?
    fun existsByEmail(email: String): Boolean
}

@Repository
interface CatechismBookletRepository : JpaRepository<CatechismBooklet, UUID> {
    fun findByDiocese(diocese: String): List<CatechismBooklet>
}

@Repository
interface CatechismQuestionRepository : JpaRepository<CatechismQuestion, UUID> {
    fun findByBookletId(bookletId: UUID): List<CatechismQuestion>
    fun findByBookletIdAndQuestionNumber(bookletId: UUID, questionNumber: Int): CatechismQuestion?


    @Query("""
        SELECT q FROM CatechismQuestion q
        LEFT JOIN FETCH q.translations
        WHERE q.id = :id
    """)
    fun findByIdWithTranslations(@Param("id") id: UUID): CatechismQuestion?
    
    @Query("""
        SELECT q FROM CatechismQuestion q
        LEFT JOIN FETCH q.cccReferences
        WHERE q.id = :id
    """)
    fun findByIdWithCCCReferences(@Param("id") id: UUID): CatechismQuestion?
    
    @Query("""
        SELECT q FROM CatechismQuestion q
        LEFT JOIN FETCH q.bibleReferences
        WHERE q.id = :id
    """)
    fun findByIdWithBibleReferences(@Param("id") id: UUID): CatechismQuestion?
}

@Repository
interface CatechismQuestionTranslationRepository : JpaRepository<CatechismQuestionTranslation, UUID> {
    fun findByQuestionIdAndLanguageCode(questionId: UUID, languageCode: String): CatechismQuestionTranslation?
    fun findByQuestionId(questionId: UUID): List<CatechismQuestionTranslation>
}

@Repository
interface CCCParagraphRepository : JpaRepository<CCCParagraph, UUID> {
    fun findByParagraphNumber(paragraphNumber: Int): CCCParagraph?
    
    @Query("""
        SELECT c FROM CCCParagraph c
        LEFT JOIN FETCH c.translations
        WHERE c.paragraphNumber = :number
    """)
    fun findByParagraphNumberWithTranslations(@Param("number") number: Int): CCCParagraph?
}

@Repository
interface CCCParagraphTranslationRepository : JpaRepository<CCCParagraphTranslation, UUID> {
    fun findByCccParagraphIdAndLanguageCode(cccParagraphId: UUID, languageCode: String): CCCParagraphTranslation?
}

@Repository
interface BibleReferenceRepository : JpaRepository<BibleReference, UUID> {
    fun findByBookAndChapterAndVerseStartAndVerseEndAndTranslation(
        book: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int?,
        translation: String
    ): BibleReference?
    
    @Query("""
        SELECT b FROM BibleReference b
        LEFT JOIN FETCH b.translations
        WHERE b.id = :id
    """)
    fun findByIdWithTranslations(@Param("id") id: UUID): BibleReference?
}

@Repository
interface BibleReferenceTranslationRepository : JpaRepository<BibleReferenceTranslation, UUID> {
    fun findByBibleReferenceIdAndLanguageCode(bibleReferenceId: UUID, languageCode: String): BibleReferenceTranslation?
}

@Repository
interface QuestionCCCReferenceRepository : JpaRepository<QuestionCCCReference, UUID> {
    fun findByQuestionIdOrderByReferenceOrder(questionId: UUID): List<QuestionCCCReference>
}

@Repository
interface QuestionBibleReferenceRepository : JpaRepository<QuestionBibleReference, UUID> {
    fun findByQuestionIdOrderByReferenceOrder(questionId: UUID): List<QuestionBibleReference>
}

@Repository
interface CatechismActRepository : JpaRepository<CatechismAct, UUID> {
    fun findByBookletIdOrderByDisplayOrder(bookletId: UUID): List<CatechismAct>
    
    @Query("""
        SELECT a FROM CatechismAct a
        LEFT JOIN FETCH a.translations
        WHERE a.id = :id
    """)
    fun findByIdWithTranslations(@Param("id") id: UUID): CatechismAct?
    
    @Query("""
        SELECT a FROM CatechismAct a
        LEFT JOIN FETCH a.subtitles
        WHERE a.booklet.id = :bookletId
        ORDER BY a.displayOrder
    """)
    fun findByBookletIdWithSubtitles(@Param("bookletId") bookletId: UUID): List<CatechismAct>
}

@Repository
interface CatechismActTranslationRepository : JpaRepository<CatechismActTranslation, UUID> {
    fun findByActIdAndLanguageCode(actId: UUID, languageCode: String): CatechismActTranslation?
    fun findByActId(actId: UUID): List<CatechismActTranslation>
}

@Repository
interface CatechismActSubtitleRepository : JpaRepository<CatechismActSubtitle, UUID> {
    fun findByActIdOrderByDisplayOrder(actId: UUID): List<CatechismActSubtitle>
    
    @Query("""
        SELECT s FROM CatechismActSubtitle s
        LEFT JOIN FETCH s.translations
        WHERE s.id = :id
    """)
    fun findByIdWithTranslations(@Param("id") id: UUID): CatechismActSubtitle?
    
    @Query("""
        SELECT s FROM CatechismActSubtitle s
        LEFT JOIN FETCH s.questions
        WHERE s.id = :id
    """)
    fun findByIdWithQuestions(@Param("id") id: UUID): CatechismActSubtitle?
}

@Repository
interface CatechismActSubtitleTranslationRepository : JpaRepository<CatechismActSubtitleTranslation, UUID> {
    fun findBySubtitleIdAndLanguageCode(subtitleId: UUID, languageCode: String): CatechismActSubtitleTranslation?
    fun findBySubtitleId(subtitleId: UUID): List<CatechismActSubtitleTranslation>
}
