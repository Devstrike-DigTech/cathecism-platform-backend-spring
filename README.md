# Catechism Doctrine Platform - Phase 1

A structured, moderated, multilingual digital catechism platform built with Spring Boot (Kotlin) and GraphQL.

## 🎯 Phase 1 Goals

- ✅ Basic question/answer retrieval
- ✅ Single language support (English)
- ✅ CCC paragraph linking
- ✅ Bible verse linking
- ✅ Admin panel for content entry
- ✅ JWT authentication

## 🏗️ Architecture

### Backend Stack
- **Language**: Kotlin 1.9.22
- **Framework**: Spring Boot 3.2.2
- **Database**: PostgreSQL 16+
- **Migration**: Flyway
- **API**: GraphQL (Spring for GraphQL)
- **Auth**: JWT (jjwt 0.12.3)

### Key Features
- Multilingual translation support (separate translation tables)
- Role-based access control
- Structured cross-referencing (Question ↔ CCC ↔ Bible)
- Type-safe domain modeling with Kotlin

## 📁 Project Structure

```
catechism-platform/
├── build.gradle.kts                    # Gradle build configuration
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/com/catechism/platform/
│   │   │   ├── CatechismPlatformApplication.kt    # Main application
│   │   │   ├── domain/                             # Domain entities
│   │   │   │   ├── AppUser.kt
│   │   │   │   ├── UserRole.kt
│   │   │   │   ├── CatechismBooklet.kt
│   │   │   │   ├── CatechismQuestion.kt
│   │   │   │   ├── CatechismQuestionTranslation.kt
│   │   │   │   ├── CCCParagraph.kt
│   │   │   │   ├── CCCParagraphTranslation.kt
│   │   │   │   ├── BibleReference.kt
│   │   │   │   ├── BibleReferenceTranslation.kt
│   │   │   │   ├── QuestionCCCReference.kt
│   │   │   │   └── QuestionBibleReference.kt
│   │   │   ├── repository/                         # Data access
│   │   │   │   └── Repositories.kt
│   │   │   ├── service/                            # Business logic
│   │   │   ├── graphql/                            # GraphQL resolvers
│   │   │   ├── security/                           # JWT & Security
│   │   │   └── config/                             # Configuration
│   │   └── resources/
│   │       ├── application.yml                     # App configuration
│   │       ├── graphql/
│   │       │   └── schema.graphqls                 # GraphQL schema
│   │       └── db/migration/
│   │           └── V1__Initial_schema.sql          # Database schema
│   └── test/
│       └── kotlin/com/catechism/platform/
└── README.md
```

## 🗄️ Database Schema

### Core Tables

#### Booklets & Questions
- `catechism_booklet` - Booklet metadata
- `catechism_question` - Questions (language-agnostic)
- `catechism_question_translation` - Question text/answers per language

#### CCC References
- `ccc_paragraph` - CCC paragraph numbers
- `ccc_paragraph_translation` - CCC text per language

#### Bible References
- `bible_reference` - Bible verse metadata
- `bible_reference_translation` - Verse text per language

#### Junction Tables (Many-to-Many)
- `question_ccc_reference` - Links questions to CCC paragraphs
- `question_bible_reference` - Links questions to Bible verses

#### Users
- `app_user` - User accounts with roles

### Entity Relationships

```
CatechismBooklet
    ↓ 1:N
CatechismQuestion ← (translations) → CatechismQuestionTranslation
    ↓ M:N                                    ↓ M:N
CCCParagraph ← (translations) → CCCParagraphTranslation
BibleReference ← (translations) → BibleReferenceTranslation
```

## 🚀 Getting Started

### Prerequisites
- JDK 21
- PostgreSQL 16+
- Gradle 8.x (or use wrapper)

### Database Setup

```bash
# Create database
createdb catechism_db

# Create user
psql -c "CREATE USER catechism_user WITH PASSWORD 'changeme';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE catechism_db TO catechism_user;"
```

### Running the Application

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### GraphiQL Interface

Access the GraphQL playground at:
```
http://localhost:8080/graphiql
```

## 📝 GraphQL API

### Example Queries

#### Get a Question
```graphql
query GetQuestion {
  question(id: "uuid-here", language: "en") {
    id
    number
    text
    answer
    category
    cccReferences {
      paragraphNumber
      text
    }
    bibleReferences {
      reference
      text
    }
  }
}
```

#### Get All Booklets
```graphql
query GetBooklets {
  booklets {
    id
    name
    diocese
    version
    questions(language: "en") {
      id
      number
      text
    }
  }
}
```

#### Get CCC Paragraph
```graphql
query GetCCC {
  cccParagraph(number: 355, language: "en") {
    paragraphNumber
    text
    edition
  }
}
```

### Example Mutations

#### Register User
```graphql
mutation Register {
  register(input: {
    email: "user@example.com"
    password: "securepassword"
    name: "John Doe"
    diocese: "New York"
  }) {
    token
    user {
      id
      email
      name
      role
    }
  }
}
```

#### Login
```graphql
mutation Login {
  login(email: "user@example.com", password: "securepassword") {
    token
    user {
      id
      email
      role
    }
  }
}
```

#### Create Booklet (Admin)
```graphql
mutation CreateBooklet {
  createBooklet(input: {
    name: "Baltimore Catechism"
    version: "1.0"
    diocese: "Baltimore"
    languageDefault: "en"
  }) {
    id
    name
    version
  }
}
```

#### Create Question (Admin)
```graphql
mutation CreateQuestion {
  createQuestion(input: {
    bookletId: "booklet-uuid"
    questionNumber: 1
    category: "Creation"
    language: "en"
    questionText: "Who made us?"
    answerText: "God made us."
    isOfficial: true
  }) {
    id
    number
    text
    answer
  }
}
```

## 🔒 Security

### Roles
- `PUBLIC_USER` - Can view content
- `CATECHIST` - Can contribute explanations (Phase 2)
- `PRIEST` - Can contribute explanations (Phase 2)
- `THEOLOGY_REVIEWER` - Can review submissions (Phase 2)
- `ADMIN` - Can manage all content

### Default Admin Account
```
Email: admin@catechism.com
Password: admin123
```
**⚠️ CHANGE THIS IN PRODUCTION**

### JWT Configuration
Edit `application.yml`:
```yaml
app:
  jwt:
    secret: "your-256-bit-secret-key"
    expiration: 86400000  # 24 hours
```

## 🌍 Multilingual Support

### Supported Languages (Phase 1)
- `en` - English (default)
- `fr` - French
- `es` - Spanish
- `pt` - Portuguese
- `ig` - Igbo
- `yo` - Yoruba

### Translation Architecture

All user-facing text is stored in separate translation tables:
- Questions/answers → `catechism_question_translation`
- CCC paragraphs → `ccc_paragraph_translation`
- Bible verses → `bible_reference_translation`

**Language Fallback Logic**:
1. Try requested language
2. Fall back to booklet's default language
3. Fall back to English

### Official vs Community Translations

Each translation has an `is_official` flag:
- `true` - Officially approved translation
- `false` - Community-contributed translation

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## 📊 Performance Considerations

### Caching Strategy (Future)
- CCC paragraphs (rarely change)
- Bible verses (never change)
- Question translations (rarely change)

### Read Optimization
- Use `@BatchSize` for N+1 query prevention
- Fetch strategies tuned for GraphQL resolvers
- Read replicas for scaling

## 🔄 Migration Strategy

### Adding New Content

1. **Add CCC Paragraph**
```graphql
mutation {
  addCCCParagraph(input: {
    paragraphNumber: 355
    edition: "2nd Edition"
    language: "en"
    paragraphText: "..."
    licensed: false
  }) {
    id
    paragraphNumber
  }
}
```

2. **Add Bible Reference**
```graphql
mutation {
  addBibleReference(input: {
    book: "Genesis"
    chapter: 1
    verseStart: 27
    verseEnd: null
    translation: "RSV-CE"
    language: "en"
    verseText: "So God created man in his own image..."
  }) {
    id
    reference
  }
}
```

3. **Link Question to CCC**
```graphql
mutation {
  linkQuestionToCCC(
    questionId: "question-uuid"
    cccParagraphNumber: 355
    order: 0
  ) {
    id
    cccReferences {
      paragraphNumber
      text
    }
  }
}
```

## 🐛 Troubleshooting

### Database Connection Issues
```bash
# Check PostgreSQL is running
pg_isready

# Test connection
psql -h localhost -U catechism_user -d catechism_db
```

### Flyway Migration Errors
```bash
# Repair Flyway state
./gradlew flywayRepair

# Re-run migrations
./gradlew flywayMigrate
```

### GraphQL Schema Errors
- Check `src/main/resources/graphql/schema.graphqls`
- Ensure resolver methods match schema
- Restart application to reload schema

## 📈 Next Steps (Phase 2)

- [ ] Explanation submission system
- [ ] Moderation workflow
- [ ] Audio/video explanations
- [ ] Media file storage (S3/GCS)
- [ ] User verification system
- [ ] Email notifications

## 📚 Resources

- [Spring for GraphQL Docs](https://spring.io/projects/spring-graphql)
- [Kotlin Spring Guide](https://spring.io/guides/tutorials/spring-boot-kotlin/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [GraphQL Best Practices](https://graphql.org/learn/best-practices/)

## 📄 License

Copyright © 2026 Catechism Doctrine Platform

---

**Built with doctrinal integrity in mind.**
