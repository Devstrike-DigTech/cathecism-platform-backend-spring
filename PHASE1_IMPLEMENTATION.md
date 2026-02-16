# Phase 1: Foundation (MVP) - Implementation Plan

**Duration**: 6-8 weeks  
**Goal**: Basic question/answer retrieval with CCC and Bible linking

---

## Phase 1 Milestones

### Week 1-2: Project Setup & Database
- [ ] Initialize Spring Boot project
- [ ] Configure PostgreSQL
- [ ] Create database schema
- [ ] Set up Flyway migrations
- [ ] Create Kotlin entities

### Week 3-4: GraphQL API
- [ ] Configure Spring for GraphQL
- [ ] Create GraphQL schema
- [ ] Implement resolvers
- [ ] Add authentication (JWT)
- [ ] Test queries

### Week 5-6: Frontend (Web)
- [ ] Initialize Next.js project
- [ ] Configure Apollo Client
- [ ] Create UI components
- [ ] Implement question browsing
- [ ] Add authentication UI

### Week 7-8: Admin Panel & Testing
- [ ] Admin dashboard (add questions)
- [ ] Admin: manage CCC paragraphs
- [ ] Admin: manage Bible references
- [ ] End-to-end testing
- [ ] Deploy to staging

---

## Database Schema (Phase 1)

### Core Tables

#### 1. catechism_booklet
```sql
CREATE TABLE catechism_booklet (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    diocese VARCHAR(255),
    version VARCHAR(50) NOT NULL,
    language_default VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booklet_diocese ON catechism_booklet(diocese);
```

#### 2. catechism_question
```sql
CREATE TABLE catechism_question (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booklet_id UUID NOT NULL REFERENCES catechism_booklet(id) ON DELETE CASCADE,
    question_number INT NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(booklet_id, question_number)
);

CREATE INDEX idx_question_booklet ON catechism_question(booklet_id);
CREATE INDEX idx_question_category ON catechism_question(category);
```

#### 3. catechism_question_translation
```sql
CREATE TABLE catechism_question_translation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES catechism_question(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    question_text TEXT NOT NULL,
    answer_text TEXT NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(question_id, language_code)
);

CREATE INDEX idx_question_translation_lang ON catechism_question_translation(language_code);
CREATE INDEX idx_question_translation_question ON catechism_question_translation(question_id);
```

#### 4. ccc_paragraph
```sql
CREATE TABLE ccc_paragraph (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paragraph_number INT NOT NULL UNIQUE,
    edition VARCHAR(50) NOT NULL DEFAULT '2nd Edition',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ccc_paragraph_number ON ccc_paragraph(paragraph_number);
```

#### 5. ccc_paragraph_translation
```sql
CREATE TABLE ccc_paragraph_translation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ccc_id UUID NOT NULL REFERENCES ccc_paragraph(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    paragraph_text TEXT NOT NULL,
    licensed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(ccc_id, language_code)
);

CREATE INDEX idx_ccc_translation_lang ON ccc_paragraph_translation(language_code);
CREATE INDEX idx_ccc_translation_ccc ON ccc_paragraph_translation(ccc_id);
```

#### 6. question_ccc_reference (Many-to-Many)
```sql
CREATE TABLE question_ccc_reference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES catechism_question(id) ON DELETE CASCADE,
    ccc_id UUID NOT NULL REFERENCES ccc_paragraph(id) ON DELETE CASCADE,
    reference_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(question_id, ccc_id)
);

CREATE INDEX idx_qccc_question ON question_ccc_reference(question_id);
CREATE INDEX idx_qccc_ccc ON question_ccc_reference(ccc_id);
```

#### 7. bible_reference
```sql
CREATE TABLE bible_reference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book VARCHAR(50) NOT NULL,
    chapter INT NOT NULL,
    verse_start INT NOT NULL,
    verse_end INT,
    translation VARCHAR(50) NOT NULL DEFAULT 'RSV-CE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(book, chapter, verse_start, verse_end, translation)
);

CREATE INDEX idx_bible_book ON bible_reference(book);
CREATE INDEX idx_bible_chapter ON bible_reference(book, chapter);
```

#### 8. bible_reference_translation
```sql
CREATE TABLE bible_reference_translation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bible_reference_id UUID NOT NULL REFERENCES bible_reference(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    verse_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(bible_reference_id, language_code)
);

CREATE INDEX idx_bible_translation_lang ON bible_reference_translation(language_code);
CREATE INDEX idx_bible_translation_ref ON bible_reference_translation(bible_reference_id);
```

#### 9. question_bible_reference (Many-to-Many)
```sql
CREATE TABLE question_bible_reference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES catechism_question(id) ON DELETE CASCADE,
    bible_reference_id UUID NOT NULL REFERENCES bible_reference(id) ON DELETE CASCADE,
    reference_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(question_id, bible_reference_id)
);

CREATE INDEX idx_qbible_question ON question_bible_reference(question_id);
CREATE INDEX idx_qbible_bible ON question_bible_reference(bible_reference_id);
```

#### 10. app_user (Authentication)
```sql
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'PUBLIC_USER',
    verified BOOLEAN NOT NULL DEFAULT false,
    verification_notes TEXT,
    diocese VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_email ON app_user(email);
CREATE INDEX idx_user_role ON app_user(role);

-- Roles: PUBLIC_USER, CATECHIST, PRIEST, THEOLOGY_REVIEWER, ADMIN
```

---

## Kotlin Entities

### CatechismBooklet.kt
```kotlin
package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catechism_booklet")
data class CatechismBooklet(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    @Column
    val diocese: String? = null,

    @Column(nullable = false)
    val version: String,

    @Column(name = "language_default", nullable = false)
    val languageDefault: String = "en",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "booklet", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val questions: List<CatechismQuestion> = emptyList()
)
```

### CatechismQuestion.kt
```kotlin
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
    val category: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val translations: List<CatechismQuestionTranslation> = emptyList(),

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val cccReferences: List<QuestionCCCReference> = emptyList(),

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val bibleReferences: List<QuestionBibleReference> = emptyList()
)
```

### CatechismQuestionTranslation.kt
```kotlin
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
    val questionText: String,

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    val answerText: String,

    @Column(name = "is_official", nullable = false)
    val isOfficial: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
```

### CCCParagraph.kt
```kotlin
package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ccc_paragraph")
data class CCCParagraph(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "paragraph_number", nullable = false, unique = true)
    val paragraphNumber: Int,

    @Column(nullable = false)
    val edition: String = "2nd Edition",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "cccParagraph", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val translations: List<CCCParagraphTranslation> = emptyList()
)
```

### CCCParagraphTranslation.kt
```kotlin
package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "ccc_paragraph_translation",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["ccc_id", "language_code"])
    ]
)
data class CCCParagraphTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ccc_id", nullable = false)
    val cccParagraph: CCCParagraph,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(name = "paragraph_text", nullable = false, columnDefinition = "TEXT")
    val paragraphText: String,

    @Column(nullable = false)
    val licensed: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
```

### BibleReference.kt
```kotlin
package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "bible_reference",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["book", "chapter", "verse_start", "verse_end", "translation"])
    ]
)
data class BibleReference(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val book: String,

    @Column(nullable = false)
    val chapter: Int,

    @Column(name = "verse_start", nullable = false)
    val verseStart: Int,

    @Column(name = "verse_end")
    val verseEnd: Int? = null,

    @Column(nullable = false)
    val translation: String = "RSV-CE",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "bibleReference", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val translations: List<BibleReferenceTranslation> = emptyList()
)
```

### BibleReferenceTranslation.kt
```kotlin
package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "bible_reference_translation",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["bible_reference_id", "language_code"])
    ]
)
data class BibleReferenceTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bible_reference_id", nullable = false)
    val bibleReference: BibleReference,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(name = "verse_text", nullable = false, columnDefinition = "TEXT")
    val verseText: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
```

### QuestionCCCReference.kt (Junction)
```kotlin
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
    val referenceOrder: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
```

### QuestionBibleReference.kt (Junction)
```kotlin
package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "question_bible_reference",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["question_id", "bible_reference_id"])
    ]
)
data class QuestionBibleReference(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    val question: CatechismQuestion,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bible_reference_id", nullable = false)
    val bibleReference: BibleReference,

    @Column(name = "reference_order", nullable = false)
    val referenceOrder: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
```

### AppUser.kt
```kotlin
package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_user")
data class AppUser(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.PUBLIC_USER,

    @Column(nullable = false)
    val verified: Boolean = false,

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    val verificationNotes: String? = null,

    @Column
    val diocese: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class UserRole {
    PUBLIC_USER,
    CATECHIST,
    PRIEST,
    THEOLOGY_REVIEWER,
    ADMIN
}
```

---

## GraphQL Schema (Phase 1)

### schema.graphqls
```graphql
scalar UUID
scalar DateTime

type Query {
  # Get a single question by ID
  question(id: UUID!, language: String = "en"): Question
  
  # Get all questions in a booklet
  questions(bookletId: UUID!, language: String = "en"): [Question!]!
  
  # Get a CCC paragraph
  cccParagraph(number: Int!, language: String = "en"): CCCParagraph
  
  # Get a Bible reference
  bibleReference(book: String!, chapter: Int!, verse: Int!, language: String = "en"): BibleReference
  
  # Get all booklets
  booklets: [CatechismBooklet!]!
  
  # Current user info
  me: User
}

type Mutation {
  # Authentication
  login(email: String!, password: String!): AuthPayload!
  register(input: RegisterInput!): AuthPayload!
}

type CatechismBooklet {
  id: UUID!
  name: String!
  diocese: String
  version: String!
  languageDefault: String!
  questions(language: String = "en"): [Question!]!
  createdAt: DateTime!
}

type Question {
  id: UUID!
  number: Int!
  text: String!
  answer: String!
  category: String
  booklet: CatechismBooklet!
  cccReferences: [CCCParagraph!]!
  bibleReferences: [BibleReference!]!
  translationOfficial: Boolean!
  availableLanguages: [String!]!
}

type CCCParagraph {
  id: UUID!
  paragraphNumber: Int!
  text: String!
  edition: String!
  licensed: Boolean!
}

type BibleReference {
  id: UUID!
  reference: String!  # "Genesis 1:27" or "Genesis 1:27-28"
  text: String!
  book: String!
  chapter: Int!
  verseStart: Int!
  verseEnd: Int
  translation: String!
}

type User {
  id: UUID!
  email: String!
  name: String!
  role: UserRole!
  verified: Boolean!
  diocese: String
}

enum UserRole {
  PUBLIC_USER
  CATECHIST
  PRIEST
  THEOLOGY_REVIEWER
  ADMIN
}

type AuthPayload {
  token: String!
  user: User!
}

input RegisterInput {
  email: String!
  password: String!
  name: String!
  diocese: String
}
```

---

## Next Steps

1. **Set up Spring Boot project structure**
2. **Configure dependencies** (Spring Data JPA, Spring GraphQL, PostgreSQL, Flyway, Spring Security)
3. **Create Flyway migrations** for all tables
4. **Implement repositories** (Spring Data JPA)
5. **Create GraphQL resolvers**
6. **Implement authentication** (JWT)
7. **Set up Next.js frontend**
8. **Create admin panel**

Ready to generate the full Spring Boot project structure?
