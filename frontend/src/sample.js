// Draws a fake "npm start" error screenshot on a canvas so the demo needs no asset file.
export function makeSampleFile() {
  const c = document.createElement("canvas");
  c.width = 900;
  c.height = 420;
  const g = c.getContext("2d");
  g.fillStyle = "#1e1e1e";
  g.fillRect(0, 0, 900, 420);
  g.fillStyle = "#333";
  g.fillRect(0, 0, 900, 30);
  g.fillStyle = "#ccc";
  g.font = "13px monospace";
  g.fillText("Terminal — my-app", 14, 20);

  const lines = [
    ["#6a9955", "$ npm start"],
    ["#ccc", ""],
    ["#ccc", "> my-app@0.1.0 start"],
    ["#ccc", "> react-scripts start"],
    ["#ccc", ""],
    ["#f14c4c", "Failed to compile."],
    ["#ccc", ""],
    ["#f14c4c", "Module not found: Error: Can't resolve 'axios' in '/Users/dev/my-app/src'"],
    ["#ccc", "  at ./src/api/client.js 1:0-26"],
    ["#ccc", ""],
    ["#dcdcaa", "ERROR in ./src/api/client.js"],
    ["#ccc", "import axios from 'axios';"],
    ["#ccc", "^^^^^^^^^^^^^^^^^^^^^^^^^^"],
  ];
  g.font = "16px monospace";
  lines.forEach(([color, text], i) => {
    g.fillStyle = color;
    g.fillText(text, 18, 62 + i * 26);
  });

  return new Promise((resolve) =>
    c.toBlob((blob) => resolve(new File([blob], "sample-error.png", { type: "image/png" })), "image/png")
  );
}

// Pre-recorded result: shown for the sample if the live API call fails (demo insurance).
export const FALLBACK_RESULT = {
  title: "Module not found: 'axios'",
  tool: "React (Create React App) / npm",
  severity: "low",
  summary:
    "Your app imports the 'axios' package, but it isn't installed in this project, so the build can't continue.",
  root_cause:
    "client.js imports 'axios' on line 1, but axios is missing from node_modules (never installed, or removed from package.json).",
  evidence: [
    "Module not found: Error: Can't resolve 'axios'",
    "ERROR in ./src/api/client.js",
    "import axios from 'axios';",
  ],
  fix_steps: [
    "Open a terminal in the project root (the folder with package.json).",
    "Install the missing package.",
    "Restart the dev server.",
    "If it still fails, delete node_modules and reinstall.",
  ],
  commands: ["npm install axios", "npm start", "rm -rf node_modules package-lock.json && npm install"],
  prevention: "Always run npm install axios --save (or npm i) so dependencies are recorded in package.json.",
  confidence: 96,
};
