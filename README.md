# 🔎 DevLens

DevLens is an AI-powered tool that analyzes error screenshots and explains the error in simple terms.

It uses **Gemma 4 through the Gemini API** to identify the error, explain the root cause, provide evidence from the screenshot, and suggest possible fixes.

## Tech Stack

- **Frontend:** React + Vite
- **Backend:** Spring Boot 3 + Java 17
- **AI:** Gemma 4 via Gemini API
- **Logging:** SLF4J

## How It Works

```text
React UI
   ↓
Spring Boot REST API
   ↓
Gemini API
   ↓
Gemma 4
   ↓
Error explanation
   ↓
React UI
```

The Gemini API key is kept on the backend and is never exposed to the frontend.

## Features

- Upload or paste an error screenshot
- Analyze screenshots using Gemma 4
- Provide additional error context
- Select between supported Gemma 4 models
- Get root cause, evidence, and suggested fixes
- Copy suggested commands
- Backend health check
- SLF4J debug logging for API requests and processing

## Running the Project

### Backend

```bash
cd backend
mvn spring-boot:run
```

Set your Gemini API key before starting the backend.

**Windows PowerShell**

```powershell
$env:GEMINI_API_KEY="YOUR_API_KEY"
```

**Linux / macOS**

```bash
export GEMINI_API_KEY="YOUR_API_KEY"
```

Backend runs on:

```text
http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on:

```text
http://localhost:5173
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Check backend status |
| POST | `/api/explain` | Analyze an error screenshot |

## Screenshots

### Application UI
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/dd47e64f-8954-46ba-ae65-1402b6a53d19" />

### Backend Logs
<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/573f2066-dfd9-455f-87a2-e07a6e5b09aa" />


