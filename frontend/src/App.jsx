import { useEffect, useRef, useState, useCallback } from "react";
import { explainScreenshot, getHealth } from "./api.js";
import { makeSampleFile, FALLBACK_RESULT } from "./sample.js";
import ResultCard from "./components/ResultCard.jsx";

const MODELS = [
  { id: "gemma-4-26b-a4b-it", label: "gemma-4-26b-a4b-it (faster, MoE)" },
  { id: "gemma-4-31b-it", label: "gemma-4-31b-it (larger)" },
];

export default function App() {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [isSample, setIsSample] = useState(false);
  const [context, setContext] = useState("");
  const [model, setModel] = useState(MODELS[0].id);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [note, setNote] = useState(null);
  const [error, setError] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const [health, setHealth] = useState(null);
  const inputRef = useRef(null);

  useEffect(() => {
    getHealth().then(setHealth).catch(() => setHealth({ status: "down" }));
  }, []);

  const loadFile = useCallback((f, sample = false) => {
    if (!f || !f.type.startsWith("image/")) return;
    setFile(f);
    setIsSample(sample);
    setResult(null);
    setError(null);
    setNote(null);
    setPreview((old) => {
      if (old) URL.revokeObjectURL(old);
      return URL.createObjectURL(f);
    });
  }, []);

  // Paste a screenshot straight from the clipboard
  useEffect(() => {
    const onPaste = (e) => {
      for (const item of e.clipboardData.items) {
        if (item.type.startsWith("image/")) {
          loadFile(item.getAsFile());
          break;
        }
      }
    };
    document.addEventListener("paste", onPaste);
    return () => document.removeEventListener("paste", onPaste);
  }, [loadFile]);

  const clear = () => {
    setFile(null);
    setPreview(null);
    setResult(null);
    setError(null);
    setNote(null);
    setIsSample(false);
    if (inputRef.current) inputRef.current.value = "";
  };

  const run = async () => {
    if (!file) {
      setError('Add a screenshot first (paste, drop, or "Load sample error").');
      return;
    }
    setLoading(true);
    setError(null);
    setNote(null);
    setResult(null);
    try {
      setResult(await explainScreenshot({ file, context: context.trim(), model }));
    } catch (e) {
      if (isSample) {
        setResult(FALLBACK_RESULT);
        setNote(`Live call failed (${e.message}). Showing a pre-recorded Gemma result for the sample.`);
      } else {
        setError(e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const keyMissing = health && health.status === "ok" && !health.apiKeyConfigured;
  const backendDown = health && health.status === "down";

  return (
    <>
      <header>
        <h1>
          🔎 Dev<span>Lens</span>{" "}
          <small>— screenshot an error, get the fix</small>
        </h1>
        <span className="badge">Gemma 4 · {model}</span>
      </header>

      {backendDown && (
        <div className="banner">Backend not reachable on :8080 — start the Spring Boot app.</div>
      )}
      {keyMissing && (
        <div className="banner">Backend is running but GEMINI_API_KEY is not set.</div>
      )}

      <main>
        <section className="card">
          <h2>1 · Input screenshot</h2>
          <div
            className={`drop ${dragOver ? "over" : ""}`}
            onClick={() => inputRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragOver(false);
              loadFile(e.dataTransfer.files[0]);
            }}
          >
            {preview ? (
              <img src={preview} alt="input screenshot" />
            ) : (
              <div>
                📋 <b>Paste</b> (Ctrl/Cmd+V), <b>drop</b>, or <b>click</b> to upload
                <br />a screenshot of an error
              </div>
            )}
          </div>
          <input
            ref={inputRef}
            type="file"
            accept="image/*"
            hidden
            onChange={(e) => loadFile(e.target.files[0])}
          />

          <label>Optional context (what were you doing? stack?)</label>
          <textarea
            rows={2}
            value={context}
            onChange={(e) => setContext(e.target.value)}
            placeholder="e.g. Running my React app after npm install on Windows"
          />

          <label>Model</label>
          <select value={model} onChange={(e) => setModel(e.target.value)}>
            {MODELS.map((m) => (
              <option key={m.id} value={m.id}>{m.label}</option>
            ))}
          </select>

          <div className="row">
            <button className="primary" onClick={run} disabled={loading}>
              {loading ? "Thinking…" : "Explain with Gemma 4"}
            </button>
            <button onClick={async () => loadFile(await makeSampleFile(), true)}>
              Load sample error
            </button>
            <button onClick={clear}>Clear</button>
          </div>
        </section>

        <section className="card">
          <h2>2 · Gemma 4 result</h2>
          {loading && (
            <div className="msg"><span className="spin" />Gemma 4 is reading your screenshot…</div>
          )}
          {error && <div className="err">{error}</div>}
          {result && <ResultCard result={result} note={note} />}
          {!loading && !error && !result && (
            <div className="msg">Your explanation will appear here.</div>
          )}
        </section>
      </main>

      <footer>
        Model: <code>{model}</code> via Gemini API · called server-side from Spring Boot
      </footer>
    </>
  );
}
