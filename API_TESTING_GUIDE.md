# API Testing Guide - Catechism Platform

Complete guide for testing the GraphQL API using GraphiQL, Postman, and automated tests.

---

## 📋 Table of Contents

1. [Testing with GraphiQL](#testing-with-graphiql)
2. [Testing with Postman](#testing-with-postman)
3. [Testing with cURL](#testing-with-curl)
4. [Authentication Flow](#authentication-flow)
5. [Complete Test Scenarios](#complete-test-scenarios)
6. [Error Handling](#error-handling)
7. [Integration Tests (Code)](#integration-tests)

---

## 🎨 Testing with GraphiQL

GraphiQL is built-in and accessible at `http://localhost:8080/graphiql`

### Setup GraphiQL

1. **Start the application**
   ```bash
   docker-compose up -d
   ./gradlew bootRun
   ```

2. **Open GraphiQL**
   ```
   http://localhost:8080/graphiql
   ```

3. **Set Headers (for authenticated requests)**
   Click "Headers" tab at bottom and add:
   ```json
   {
     "Authorization": "Bearer YOUR_JWT_TOKEN_HERE"
   }
   ```

### GraphiQL Features

- **Auto-completion**: Press `Ctrl+Space` for suggestions
- **Documentation**: Click "Docs" button to see schema
- **History**: Previous queries are saved
- **Prettify**: Click "Prettify" to format your query
- **Variables**: Use the "Variables" tab for dynamic values

---

## 🔐 Authentication Flow

### Step 1: Register a New User

```graphql
mutation RegisterUser {
  register(input: {
    email: "test@example.com"
    password: "SecurePassword123!"
    name: "Test User"
    diocese: "Test Diocese"
  }) {
    token
    user {
      id
      email
      name
      role
      verified
      diocese
    }
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "register": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "test@example.com",
        "name": "Test User",
        "role": "PUBLIC_USER",
        "verified": false,
        "diocese": "Test Diocese"
      }
    }
  }
}
```

### Step 2: Login with Existing User

```graphql
mutation LoginUser {
  login(
    email: "admin@catechism.com"
    password: "admin123"
  ) {
    token
    user {
      id
      email
      name
      role
      verified
    }
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "login": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": "650e8400-e29b-41d4-a716-446655440001",
        "email": "admin@catechism.com",
        "name": "System Administrator",
        "role": "ADMIN",
        "verified": true
      }
    }
  }
}
```

### Step 3: Test Authentication

```graphql
query GetCurrentUser {
  me {
    id
    email
    name
    role
    verified
    diocese
  }
}
```

**With valid token:**
```json
{
  "data": {
    "me": {
      "id": "650e8400-e29b-41d4-a716-446655440001",
      "email": "admin@catechism.com",
      "name": "System Administrator",
      "role": "ADMIN",
      "verified": true,
      "diocese": null
    }
  }
}
```

**Without token or invalid token:**
```json
{
  "errors": [
    {
      "message": "Unauthorized",
      "path": ["me"]
    }
  ],
  "data": {
    "me": null
  }
}
```

---

## 📚 Complete Test Scenarios

### Scenario 1: Create Complete Booklet Structure

#### 1.1 Create Booklet

```graphql
mutation CreateBooklet {
  createBooklet(input: {
    name: "Baltimore Catechism No. 1"
    diocese: "Baltimore"
    version: "1.0"
    languageDefault: "en"
  }) {
    id
    name
    version
    diocese
    languageDefault
    createdAt
  }
}
```

**Save the booklet ID from response!**

#### 1.2 Create Act 1

```graphql
mutation CreateAct1 {
  createAct(input: {
    bookletId: "PASTE_BOOKLET_ID_HERE"
    actNumber: 1
    displayOrder: 10
    language: "en"
    title: "The Creed"
    description: "What we believe"
  }) {
    id
    actNumber
    title
    description
    displayOrder
    availableLanguages
  }
}
```

**Save the act ID from response!**

#### 1.3 Create Subtitle Under Act 1

```graphql
mutation CreateSubtitle1 {
  createSubtitle(input: {
    actId: "PASTE_ACT_ID_HERE"
    subtitleNumber: 1
    displayOrder: 10
    language: "en"
    title: "God the Father Almighty"
    description: "Creator of heaven and earth"
  }) {
    id
    subtitleNumber
    title
    description
    displayOrder
    act {
      title
    }
  }
}
```

**Save the subtitle ID from response!**

#### 1.4 Create Question

```graphql
mutation CreateQuestion1 {
  createQuestion(input: {
    bookletId: "PASTE_BOOKLET_ID_HERE"
    subtitleId: "PASTE_SUBTITLE_ID_HERE"
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
    category
    translationOfficial
    booklet {
      name
    }
    subtitle {
      title
      act {
        title
      }
    }
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "createQuestion": {
      "id": "750e8400-e29b-41d4-a716-446655440003",
      "number": 1,
      "text": "Who made us?",
      "answer": "God made us.",
      "category": "Creation",
      "translationOfficial": true,
      "booklet": {
        "name": "Baltimore Catechism No. 1"
      },
      "subtitle": {
        "title": "God the Father Almighty",
        "act": {
          "title": "The Creed"
        }
      }
    }
  }
}
```

**Save the question ID from response!**

#### 1.5 Add CCC Paragraph

```graphql
mutation AddCCCParagraph355 {
  addCCCParagraph(input: {
    paragraphNumber: 355
    edition: "2nd Edition"
    language: "en"
    paragraphText: "God created man in his own image, in the image of God he created him; male and female he created them. Man occupies a unique place in creation: he is 'in the image of God'; in his own nature he unites the spiritual and material worlds; he is created 'male and female'; God established him in his friendship."
    licensed: false
  }) {
    id
    paragraphNumber
    text
    edition
    licensed
  }
}
```

#### 1.6 Link Question to CCC

```graphql
mutation LinkQuestionToCCC {
  linkQuestionToCCC(
    questionId: "PASTE_QUESTION_ID_HERE"
    cccParagraphNumber: 355
    order: 0
  ) {
    id
    text
    answer
    cccReferences {
      paragraphNumber
      text
      edition
    }
  }
}
```

#### 1.7 Add Bible Reference

```graphql
mutation AddBibleReference {
  addBibleReference(input: {
    book: "Genesis"
    chapter: 1
    verseStart: 27
    verseEnd: null
    translation: "RSV-CE"
    language: "en"
    verseText: "So God created man in his own image, in the image of God he created him; male and female he created them."
  }) {
    id
    reference
    text
    book
    chapter
    verseStart
    translation
  }
}
```

**Save the bible reference ID from response!**

#### 1.8 Link Question to Bible

```graphql
mutation LinkQuestionToBible {
  linkQuestionToBible(
    questionId: "PASTE_QUESTION_ID_HERE"
    bibleReferenceId: "PASTE_BIBLE_REF_ID_HERE"
    order: 0
  ) {
    id
    text
    answer
    bibleReferences {
      reference
      text
      book
      chapter
    }
  }
}
```

---

### Scenario 2: Query Complete Structure

#### 2.1 Get All Booklets

```graphql
query GetAllBooklets {
  booklets {
    id
    name
    diocese
    version
    languageDefault
    createdAt
  }
}
```

#### 2.2 Get All Acts in Booklet

```graphql
query GetAllActs {
  acts(bookletId: "PASTE_BOOKLET_ID_HERE", language: "en") {
    id
    actNumber
    title
    description
    displayOrder
    availableLanguages
  }
}
```

#### 2.3 Get Subtitles for Act

```graphql
query GetSubtitles {
  subtitles(actId: "PASTE_ACT_ID_HERE", language: "en") {
    id
    subtitleNumber
    title
    description
    displayOrder
    act {
      title
      actNumber
    }
  }
}
```

#### 2.4 Get Questions in Subtitle

```graphql
query GetQuestionsInSubtitle {
  questionsBySubtitle(
    subtitleId: "PASTE_SUBTITLE_ID_HERE"
    language: "en"
  ) {
    id
    number
    text
    answer
    category
  }
}
```

#### 2.5 Get Complete Question Details

```graphql
query GetCompleteQuestion {
  question(id: "PASTE_QUESTION_ID_HERE", language: "en") {
    id
    number
    text
    answer
    category
    translationOfficial
    availableLanguages
    
    booklet {
      name
      version
      diocese
    }
    
    subtitle {
      title
      subtitleNumber
      act {
        title
        actNumber
      }
    }
    
    cccReferences {
      paragraphNumber
      text
      edition
      licensed
    }
    
    bibleReferences {
      reference
      text
      book
      chapter
      verseStart
      verseEnd
      translation
    }
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "question": {
      "id": "750e8400-e29b-41d4-a716-446655440003",
      "number": 1,
      "text": "Who made us?",
      "answer": "God made us.",
      "category": "Creation",
      "translationOfficial": true,
      "availableLanguages": ["en"],
      "booklet": {
        "name": "Baltimore Catechism No. 1",
        "version": "1.0",
        "diocese": "Baltimore"
      },
      "subtitle": {
        "title": "God the Father Almighty",
        "subtitleNumber": 1,
        "act": {
          "title": "The Creed",
          "actNumber": 1
        }
      },
      "cccReferences": [
        {
          "paragraphNumber": 355,
          "text": "God created man in his own image...",
          "edition": "2nd Edition",
          "licensed": false
        }
      ],
      "bibleReferences": [
        {
          "reference": "Genesis 1:27",
          "text": "So God created man in his own image...",
          "book": "Genesis",
          "chapter": 1,
          "verseStart": 27,
          "verseEnd": null,
          "translation": "RSV-CE"
        }
      ]
    }
  }
}
```

#### 2.6 Get Full Booklet Hierarchy

```graphql
query GetFullHierarchy {
  booklet(id: "PASTE_BOOKLET_ID_HERE") {
    id
    name
    version
    diocese
  }
  
  acts(bookletId: "PASTE_BOOKLET_ID_HERE", language: "en") {
    actNumber
    title
    description
    displayOrder
    
    subtitles(language: "en") {
      subtitleNumber
      title
      description
      displayOrder
      
      questions(language: "en") {
        number
        text
        answer
        category
      }
    }
  }
}
```

---

### Scenario 3: Update Operations

#### 3.1 Update Question

```graphql
mutation UpdateQuestion {
  updateQuestion(
    id: "PASTE_QUESTION_ID_HERE"
    input: {
      language: "en"
      questionText: "Who created us?"
      answerText: "God created us."
      category: "Creation and Fall"
    }
  ) {
    id
    text
    answer
    category
  }
}
```

#### 3.2 Move Question to Different Subtitle

```graphql
mutation MoveQuestion {
  updateQuestion(
    id: "PASTE_QUESTION_ID_HERE"
    input: {
      subtitleId: "NEW_SUBTITLE_ID_HERE"
    }
  ) {
    id
    text
    subtitle {
      title
      act {
        title
      }
    }
  }
}
```

#### 3.3 Update Act

```graphql
mutation UpdateAct {
  updateAct(
    id: "PASTE_ACT_ID_HERE"
    input: {
      language: "en"
      title: "The Holy Creed"
      description: "What Catholics believe"
      displayOrder: 5
    }
  ) {
    id
    title
    description
    displayOrder
  }
}
```

#### 3.4 Update Subtitle

```graphql
mutation UpdateSubtitle {
  updateSubtitle(
    id: "PASTE_SUBTITLE_ID_HERE"
    input: {
      language: "en"
      title: "God the Creator"
      description: "About God's creation"
      displayOrder: 15
    }
  ) {
    id
    title
    description
    displayOrder
  }
}
```

---

### Scenario 4: Multilingual Support

#### 4.1 Add French Translation for Act

```graphql
mutation AddFrenchActTranslation {
  updateAct(
    id: "PASTE_ACT_ID_HERE"
    input: {
      language: "fr"
      title: "Le Credo"
      description: "Ce que nous croyons"
    }
  ) {
    id
    title
    availableLanguages
  }
}
```

#### 4.2 Query Act in French

```graphql
query GetActInFrench {
  act(id: "PASTE_ACT_ID_HERE", language: "fr") {
    title
    description
    availableLanguages
  }
}
```

#### 4.3 Add French Question Translation

```graphql
mutation AddFrenchQuestionTranslation {
  createQuestion(input: {
    bookletId: "PASTE_BOOKLET_ID_HERE"
    subtitleId: "PASTE_SUBTITLE_ID_HERE"
    questionNumber: 1
    language: "fr"
    questionText: "Qui nous a créés?"
    answerText: "Dieu nous a créés."
    isOfficial: false
  }) {
    id
    text
    answer
    translationOfficial
    availableLanguages
  }
}
```

---

### Scenario 5: Delete Operations

#### 5.1 Delete Question

```graphql
mutation DeleteQuestion {
  deleteQuestion(id: "PASTE_QUESTION_ID_HERE")
}
```

**Expected Response:**
```json
{
  "data": {
    "deleteQuestion": true
  }
}
```

#### 5.2 Delete Subtitle

```graphql
mutation DeleteSubtitle {
  deleteSubtitle(id: "PASTE_SUBTITLE_ID_HERE")
}
```

**Note:** Questions are NOT deleted, just unlinked.

#### 5.3 Delete Act

```graphql
mutation DeleteAct {
  deleteAct(id: "PASTE_ACT_ID_HERE")
}
```

**Note:** Deletes all subtitles. Questions are unlinked but not deleted.

---

## 🚨 Error Handling

### Common Errors and Solutions

#### 1. Unauthorized Error

**Error:**
```json
{
  "errors": [
    {
      "message": "Unauthorized",
      "path": ["createBooklet"]
    }
  ]
}
```

**Solution:**
- Make sure you're logged in
- Add Authorization header with valid JWT token
- Check token hasn't expired (24 hours)

#### 2. Unique Constraint Violation

**Error:**
```json
{
  "errors": [
    {
      "message": "Question number must be unique within booklet"
    }
  ]
}
```

**Solution:**
- Check existing question numbers
- Use a different question number

#### 3. Foreign Key Violation

**Error:**
```json
{
  "errors": [
    {
      "message": "Referenced entity not found"
    }
  ]
}
```

**Solution:**
- Verify the ID you're referencing exists
- Check if entity was deleted
- Query to confirm entity exists first

#### 4. Validation Error

**Error:**
```json
{
  "errors": [
    {
      "message": "Validation failed: questionText cannot be blank"
    }
  ]
}
```

**Solution:**
- Provide required fields
- Check field formats
- Review input constraints

---

## 📮 Testing with Postman

### Setup Postman Collection

1. **Create New Request**
   - Method: `POST`
   - URL: `http://localhost:8080/graphql`
   - Headers:
     ```
     Content-Type: application/json
     Authorization: Bearer YOUR_JWT_TOKEN
     ```

2. **Body (raw JSON)**
   ```json
   {
     "query": "mutation { login(email: \"admin@catechism.com\", password: \"admin123\") { token user { name role } } }"
   }
   ```

### Postman Collection Example

**Login Request:**
```json
POST http://localhost:8080/graphql

Headers:
{
  "Content-Type": "application/json"
}

Body:
{
  "query": "mutation Login($email: String!, $password: String!) { login(email: $email, password: $password) { token user { name role } } }",
  "variables": {
    "email": "admin@catechism.com",
    "password": "admin123"
  }
}
```

**Create Question with Variables:**
```json
POST http://localhost:8080/graphql

Headers:
{
  "Content-Type": "application/json",
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Body:
{
  "query": "mutation CreateQuestion($input: CreateQuestionInput!) { createQuestion(input: $input) { id text answer } }",
  "variables": {
    "input": {
      "bookletId": "550e8400-e29b-41d4-a716-446655440000",
      "subtitleId": "660e8400-e29b-41d4-a716-446655440001",
      "questionNumber": 1,
      "language": "en",
      "questionText": "Who made us?",
      "answerText": "God made us.",
      "isOfficial": true
    }
  }
}
```

---

## 🔧 Testing with cURL

### Login
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { login(email: \"admin@catechism.com\", password: \"admin123\") { token user { name role } } }"
  }'
```

### Create Booklet (Authenticated)
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "query": "mutation { createBooklet(input: { name: \"Test Catechism\", version: \"1.0\", languageDefault: \"en\" }) { id name } }"
  }'
```

### Query with Variables
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query GetQuestion($id: UUID!, $lang: String!) { question(id: $id, language: $lang) { text answer } }",
    "variables": {
      "id": "750e8400-e29b-41d4-a716-446655440003",
      "lang": "en"
    }
  }'
```

---

## ✅ Testing Checklist

### Functional Tests
- [ ] User registration works
- [ ] User login returns valid JWT
- [ ] JWT authentication protects admin endpoints
- [ ] Booklet creation works
- [ ] Act creation works
- [ ] Subtitle creation works
- [ ] Question creation works
- [ ] Question links to Subtitle correctly
- [ ] CCC paragraph can be added
- [ ] Bible reference can be added
- [ ] Question links to CCC
- [ ] Question links to Bible
- [ ] Query returns full hierarchy
- [ ] Multilingual translations work
- [ ] Update operations work
- [ ] Delete operations work
- [ ] Language fallback works

### Security Tests
- [ ] Public endpoints accessible without auth
- [ ] Admin endpoints require ADMIN role
- [ ] Invalid JWT is rejected
- [ ] Expired JWT is rejected
- [ ] Non-admin users cannot create content

### Data Integrity Tests
- [ ] Unique constraints enforced
- [ ] Foreign key constraints enforced
- [ ] Cascading deletes work correctly
- [ ] Orphaned questions remain valid
- [ ] Display order allows gaps

---

## 📝 Sample Test Suite (Manual)

### Test 1: Complete CRUD Flow
1. Login as admin
2. Create booklet
3. Create act
4. Create subtitle
5. Create question
6. Update question
7. Delete question
8. Verify deletion

### Test 2: Hierarchical Navigation
1. Create complete structure
2. Query full hierarchy
3. Navigate Act → Subtitle → Question
4. Verify all links work

### Test 3: Multilingual
1. Create content in English
2. Add French translation
3. Query in French
4. Query in English
5. Test language fallback

### Test 4: References
1. Create question
2. Add CCC paragraph
3. Link question to CCC
4. Add Bible verse
5. Link question to Bible
6. Query question with references

---

**Ready to test your API thoroughly!** 🧪
