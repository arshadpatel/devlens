// Talks to the Spring Boot backend (proxied by Vite in dev).
export async function explainScreenshot({ file, context, model }) {
  const form = new FormData();
  form.append("image", file);
  if (context) form.append("context", context);
  if (model) form.append("model", model);

  const res = await fetch("/api/explain", { method: "POST", body: form });
  let data = null;
  try {
    data = await res.json();
  } catch {
    /* non-JSON error body */
  }
  if (!res.ok) {
    throw new Error(data?.error || `Request failed (${res.status})`);
  }
  return data;
}

export async function getHealth() {
  const res = await fetch("/api/health");
  if (!res.ok) throw new Error("Backend not reachable");
  return res.json();
}
