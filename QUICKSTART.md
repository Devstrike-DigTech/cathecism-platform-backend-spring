# Quick Start Guide - Catechism Platform Phase 1

Get up and running in 5 minutes.

## Prerequisites

- Java 21 (JDK)
- Docker & Docker Compose (for PostgreSQL)
- Your favorite IDE (IntelliJ IDEA recommended)

## Step 1: Start PostgreSQL

```bash
docker-compose up -d
```

This starts PostgreSQL on `localhost:5432` with:
- Database: `catechism_db`
- User: `catechism_user`
- Password: `changeme`

## Step 2: Run the Application

```bash
./gradlew bootRun
```

Or if you're on Windows:
```bash
gradlew.bat bootRun
```

The application will:
1. Connect to PostgreSQL
2. Run Flyway migrations (create all tables)
3. Seed a default admin user
4. Start on port 8080

## Step 3: Access GraphiQL

Open your browser to:
```
http://localhost:8080/graphiql
```

## Step 4: Login and Get JWT Token

In GraphiQL, run this mutation:

```graphql
mutation {
  login(email: "admin@catechism.com", password: "admin123") {
    token
    user {
      id
      name
      role
    }
  }
}
```

Copy the token from the response.

## Step 5: Add Token to Headers

In GraphiQL, click "Headers" and add:

```json
{
  "Authorization": "Bearer YOUR_TOKEN_HERE"
}
```

Replace `YOUR_TOKEN_HERE` with the actual token.

## Step 6: Create Your First Booklet

```graphql
mutation {
  createBooklet(input: {
    name: "Baltimore Catechism"
    version: "1.0"
    diocese: "Baltimore"
    languageDefault: "en"
  }) {
    id
    name
  }
}
```

Copy the `id` from the response.

## Step 7: Create Your First Question

```graphql
mutation {
  createQuestion(input: {
    bookletId: "PASTE_BOOKLET_ID_HERE"
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

## Step 8: Add a CCC Paragraph

```graphql
mutation {
  addCCCParagraph(input: {
    paragraphNumber: 355
    edition: "2nd Edition"
    language: "en"
    paragraphText: "God created man in his own image, in the image of God he created him; male and female he created them. Man occupies a unique place in creation..."
    licensed: false
  }) {
    id
    paragraphNumber
  }
}
```

## Step 9: Link Question to CCC Paragraph

```graphql
mutation {
  linkQuestionToCCC(
    questionId: "PASTE_QUESTION_ID_HERE"
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

## Step 10: Query the Full Question

```graphql
query {
  question(id: "PASTE_QUESTION_ID_HERE", language: "en") {
    id
    number
    text
    answer
    category
    cccReferences {
      paragraphNumber
      text
      edition
    }
  }
}
```

## You're Done! 🎉

You now have:
- ✅ A running Spring Boot + GraphQL API
- ✅ PostgreSQL database with proper schema
- ✅ One booklet with a question
- ✅ CCC paragraph linked to the question
- ✅ JWT authentication working

## Next Steps

1. Add more questions
2. Add Bible references
3. Test the multilingual support by adding French translations
4. Explore the codebase
5. Start building the frontend (Next.js)

## Troubleshooting

### Can't connect to database?
```bash
# Check if PostgreSQL is running
docker ps

# Check logs
docker logs catechism-postgres
```

### Application won't start?
```bash
# Check Java version
java -version  # Should be 21

# Clean build
./gradlew clean build
```

### GraphQL schema errors?
- Make sure the schema file exists at: `src/main/resources/graphql/schema.graphqls`
- Restart the application

## Development Workflow

```bash
# Start database
docker-compose up -d

# Run app in dev mode (auto-reload)
./gradlew bootRun --continuous

# Run tests
./gradlew test

# Stop database
docker-compose down
```

## IDE Setup (IntelliJ IDEA)

1. Open `build.gradle.kts`
2. Wait for Gradle sync
3. Right-click `CatechismPlatformApplication.kt`
4. Select "Run"

---

**You're now ready to build theology infrastructure! 🙏**
