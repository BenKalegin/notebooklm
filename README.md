# NotebookLM Clone

A full-stack RAG application grounded in uploaded documents, built with Java 21, Spring Boot 3, Spring AI, PostgreSQL (pgvector), and React.

## Prerequisites

- Java 21+
- Node.js 18+
- Docker & Docker Compose
- OpenAI API Key

## Setup & Run

### 1. Database
Start the PostgreSQL database with pgvector extension:
```bash
docker-compose up -d
```

### 2. Backend
Navigate to `backend` directory.
Configure your OpenAI API Key:
```bash
export OPENAI_API_KEY=sk-...
# Windows PowerShell:
# $env:OPENAI_API_KEY="sk-..."
```
Run the application:
```bash
./mvnw spring-boot:run
```
(Or use your IDE).
The backend runs on `http://localhost:8080`.

### 3. Frontend
Navigate to `frontend` directory.
Install dependencies and run:
```bash
npm install
npm run dev
```
The frontend runs on `http://localhost:5173`.

## Features
- **Upload**: Upload Markdown files. Deduplication via MD5.
- **Ingestion**: Automatic chunking (Heading-aware) and embedding (OpenAI Ada-002).
- **Chat**: Ask questions grounded in your documents.
- **Citations**: Responses include citations linking to specific document chunks.
- **Multi-tenancy**: Simple isolation by User ID (simulated via `X-User-Email` header).

## API Endpoints
- `POST /api/docs`: Upload file
- `GET /api/docs`: List files
- `POST /api/chats`: Create chat
- `POST /api/chats/{id}/messages`: Send message

## Architecture
- **Backend**: Spring Boot 3.2, Spring AI 0.8+, Spring Data JPA, Flyway.
- **DB**: PostgreSQL 15 with `vector` extension.
- **Frontend**: React, TypeScript, Vite.
