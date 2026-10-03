import { useState } from "react";

function CopyButton({ text }) {
  const [done, setDone] = useState(false);
  return (
    <button
      className="copy"
      onClick={() => {
        navigator.clipboard.writeText(text);
        setDone(true);
        setTimeout(() => setDone(false), 1200);
      }}
    >
      {done ? "Copied ✓" : "Copy"}
    </button>
  );
}

export default function ResultCard({ result, note }) {
  const sev = ["low", "medium", "high"].includes(result.severity) ? result.severity : "medium";
  const list = (a) => (Array.isArray(a) ? a : []);

  return (
    <div className="res">
      {note && <div className="err">{note}</div>}
      <h3>{result.title}</h3>
      <span className="pill">{result.tool}</span>
      <span className={`pill sev-${sev}`}>severity: {sev}</span>
      <span className="pill">confidence: {result.confidence}%</span>

      <h4>What happened</h4>
      <div>{result.summary}</div>

      <h4>Likely root cause</h4>
      <div>{result.root_cause}</div>

      {list(result.evidence).length > 0 && (
        <>
          <h4>Read from your screenshot</h4>
          {list(result.evidence).map((x, i) => (
            <div className="quote" key={i}>{x}</div>
          ))}
        </>
      )}

      <h4>How to fix it</h4>
      <ol>
        {list(result.fix_steps).map((x, i) => (
          <li key={i}>{x}</li>
        ))}
      </ol>

      {list(result.commands).length > 0 && (
        <>
          <h4>Commands</h4>
          {list(result.commands).map((x, i) => (
            <pre key={i}>
              <CopyButton text={x} />
              {x}
            </pre>
          ))}
        </>
      )}

      {result.prevention && (
        <>
          <h4>Prevent it next time</h4>
          <div>{result.prevention}</div>
        </>
      )}
    </div>
  );
}
