# AI Study Assistant

An AI-powered study tool that lets students upload documents and ask questions answered exclusively from their own notes.

## Tech Stack

| Layer       | Technology                        |
|-------------|-----------------------------------|
| Frontend    | React 18, Vite, Tailwind CSS      |
| Backend     | Java 21, Spring Boot 3.x, Maven   |
| Database    | PostgreSQL (Supabase)             |
| File Storage| Supabase Storage                  |
| AI (future) | Provider-independent abstraction  |

---

## Prerequisites

- **Node.js** 18+
- **Java** 21+
- **Maven** 3.9+

---

## Frontend Setup

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Runs at: http://localhost:5173

---

## Backend Setup

```bash
cd backend
cp src/main/resources/application-example.properties src/main/resources/application-local.properties
# Edit application-local.properties with your values
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Runs at: http://localhost:8080

Health check: `GET http://localhost:8080/api/health`

---

## Project Structure

```
study-assistant/
├── frontend/          # React + Vite application
├── backend/           # Spring Boot application
└── README.md
```

---

## Module Status

| Module | Description              | Status      |
|--------|--------------------------|-------------|
| M01    | Project Foundation       | ✅ Complete  |
| M02    | Document Upload          | Pending      |
| M03    | AI Q&A                   | Pending      |
| M04    | Export & Admin           | Pending      |
