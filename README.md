# 🔎 ErrorLens — Best Use of Gemma 4

Screenshot an error → **Gemma 4 (via the Gemini API)** reads the image and returns a plain-English
explanation, root cause, quoted evidence, fix steps and copy-paste commands.

**Stack:** React (Vite) UI · Spring Boot 3 backend (Java 17) · Gemma 4 through the Gemini API.
The API key lives only on the server — the browser never sees it.

```
browser (React) ──multipart /api/explain──▶ Spring Boot ──generateContent──▶ Gemini API (gemma-4-*)
                ◀────────── JSON ───────────             ◀──── JSON text ────
```

## Prerequisites
- Java 17+ and Maven 3.9+ (or open `backend/` in IntelliJ / VS Code and run `ErrorLensApplication`)
- Node 18+
- A Gemini API key (Google AI Studio). Confirm with the organizers that it can call Gemma 4.

## Run it (two terminals)

**1. Backend**
```bash
cd backend
export GEMINI_API_KEY=AIza...        # Windows PowerShell: $env:GEMINI_API_KEY="AIza..."
mvn spring-boot:run
# -> http://localhost:8080/api/health  should show apiKeyConfigured: true
```

**2. Frontend**
```bash
cd frontend
npm install
npm run dev
# -> http://localhost:5173
```

Click **Load sample error** → **Explain with Gemma 4**. Or paste (Ctrl/Cmd+V) any error screenshot.

## Where Gemma 4 is called (for judges)
- `backend/src/main/java/com/errorlens/service/GeminiService.java` — builds the
  `generateContent` request (image as `inline_data` + prompt), calls
  `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
- Models (see `application.properties`): `gemma-4-26b-a4b-it` (default), `gemma-4-31b-it`
- Change the default with `GEMINI_MODEL=gemma-4-31b-it`

## API
| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/api/health` | – | `{status, model, apiKeyConfigured}` |
| POST | `/api/explain` | multipart: `image` (file), `context` (opt), `model` (opt) | JSON explanation, or `{error}` |

## Demo tips (2 minutes)
1. One sentence: "Beginners can't read stack traces — Gemma reads the screenshot and tells them what to do."
2. Show the sample, click Explain, point at **"Read from your screenshot"** (proves the multimodal read).
3. Paste a *real* error from your machine.
4. Fallback: if the API/wifi fails, the **sample** shows a pre-recorded result (with a visible note).
   Keep a backup screenshot + a screen recording just in case.

## Troubleshooting
- `GEMINI_API_KEY is not set` → export it in the same terminal that runs `mvn spring-boot:run`.
- `404 model not found` → check available Gemma 4 IDs in AI Studio; edit `gemini.allowed-models`
  in `application.properties` and the `MODELS` array in `frontend/src/App.jsx`.
- `Model did not return JSON` → just retry; temperature is already low (0.2).
- Port clash → change `server.port` and the proxy target in `frontend/vite.config.js`.
- Never commit your key. `.env*` files are git-ignored.

## Ideas if you have time left
Follow-up chat about the error, multi-screenshot input, history sidebar, "apply fix" diff for code screenshots.
