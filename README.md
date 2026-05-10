# backend-elearning

## Over View
A comprehensive E-Learning backend platform enabling teachers and students to manage classes, learning materials, online exams, question banks, and AI-powered learning support.

---

## Technologies
Backend: Spring Boot 3, Java 21, JPA/Hibernate, PostgreSQL, Redis, MapStruct, Spring Security, JWT, Docker, WebSocket, MinIO, OpenAI API, MailGun.

---

## Features

### User & Authentication
- Register & login
- JWT authentication
- Refresh token
- Role-based access (Admin, Teacher, Student)
- Forgot password (OTP via email)

---

### Class Management
- Create & manage classes
- Manage students
- Join classes via links

---

### Learning Materials
- Upload/download documents
- Store files on MinIO
---

### Exam & Quiz System
- Create online exams
- Multiple question types
- Auto grading
- Exam history & score tracking
- Exam statistics & analytics
- Export exam results to Excel
- Share exams via links

---

### Question Bank
- Create & manage question banks
- Categorize by subject, topic, difficulty
- Search & filter questions
- Reuse questions across exams

---

### AI Features
- Generate questions using AI
- Generate quizzes from uploaded files
- AI-powered grading & explanations
- AI chatbot for learning support
- AI assistant for explaining answers and concepts

---

## How to run

### Create `.env` file

```env
DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASSWORD=

REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# ================== OPENAI / GEMINI ==================
API_KEY=

# ================== MAIL ==================
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=

# ================== JWT ==================
JWT_ACCESS_SECRET=
JWT_REFRESH_SECRET=
JWT_ACCESS_TOKEN_EXPIRATION=
JWT_REFRESH_TOKEN_EXPIRATION=

# ================== MINIO ==================
ENDPOINT=
MINIO_ACCESS_KEY=
MINIO_SECRET_KEY=
MINIO_BUCKET=
MINIO_KEY_PREFIX=
MINIO_PRESIGN_EXPIRY_SECONDS=
MINIO_MAKE_BUCKET_PUBLIC=

# ================== GOOGLE SHEET ==================
GOOGLE_SHEET_ID=

#=================== AGENT AI ======================
AGENT_BASE_URL=
AGENT_ACCESS_KEY=

CORS_ALLOWED_ORIGINS=

ADMIN_EMAIL=
ADMIN_USERNAME=
ADMIN_PASSWORD=

MAILGUN_API_KEY=
MAILGUN_DOMAIN=
MAILGUN_FROM=
MAILGUN_FROM_NAME=
MAILGUN_BASE_URL=
```

---

### Clone the repository

```bash
git clone https://github.com/DVM55/Auto_Grade.git
cd Auto_Grade
```

---

### Build Docker image

```bash
docker build -t manh2003ptc/elearning:latest .
```

---

### Run with Docker Compose

```bash
docker-compose up -d
```