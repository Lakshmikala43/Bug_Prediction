let currentActiveScanId = null;
let currentProjectId = null;

document.addEventListener("DOMContentLoaded", () => {
  loadDashboard();
});

async function loadDashboard() {
  try {
    const res = await fetch("/api/projects");
    if (!res.ok) return;
    const projects = await res.json();
    document.getElementById("statProjects").innerText = projects.length;

    const tbody = document.getElementById("projectsTableBody");
    if (projects.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 20px;">No projects yet. Click "+ New Project" to get started.</td></tr>`;
      return;
    }

    tbody.innerHTML = projects.map(p => `
      <tr>
        <td>#${p.id}</td>
        <td><strong>${escapeHtml(p.name)}</strong></td>
        <td><code>${escapeHtml(p.sourcePath || "N/A")}</code></td>
        <td>${p.fileCount || 0}</td>
        <td><span class="badge badge-low">Ready</span></td>
        <td>
          <button class="btn-primary" style="padding: 4px 10px; font-size: 12px;" onclick="triggerScan(${p.id})">Run Scan</button>
          <button class="btn-secondary" style="padding: 4px 8px; font-size: 12px; margin-left: 6px;" onclick="loadProjectScans(${p.id})">Scans</button>
        </td>
      </tr>
    `).join("");

    if (projects.length > 0 && !currentProjectId) {
      loadProjectScans(projects[0].id);
    }
  } catch (err) {
    console.error("Failed to load dashboard data:", err);
  }
}

async function triggerScan(projectId) {
  try {
    const res = await fetch(`/api/scans/project/${projectId}/start`, { method: "POST" });
    if (res.ok) {
      const data = await res.json();
      currentActiveScanId = data.scanId;
      alert(`Scan #${data.scanId} started successfully.`);
      pollScanProgress(data.scanId);
    } else {
      alert("Failed to start scan");
    }
  } catch (err) {
    alert("Error: " + err.message);
  }
}

async function loadProjectScans(projectId) {
  currentProjectId = projectId;
  try {
    const res = await fetch(`/api/scans/project/${projectId}`);
    if (!res.ok) return;
    const scans = await res.json();
    if (scans.length > 0) {
      const latestScan = scans[0];
      currentActiveScanId = latestScan.id;
      document.getElementById("statFiles").innerText = latestScan.scannedFiles || 0;
      document.getElementById("statHighRisk").innerText = (latestScan.highRiskCount || 0) + (latestScan.criticalRiskCount || 0);
      document.getElementById("statAvgRisk").innerText = (latestScan.avgRiskScore || 0).toFixed(1);
      loadPredictions(latestScan.id);
    }
  } catch (err) {
    console.error(err);
  }
}

async function loadPredictions(scanId) {
  try {
    const res = await fetch(`/api/analysis/scan/${scanId}/predictions`);
    if (!res.ok) return;
    const predictions = await res.json();
    const tbody = document.getElementById("predictionsTableBody");
    document.getElementById("btnExportCsv").style.display = predictions.length > 0 ? "inline-block" : "none";

    if (predictions.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 20px;">No predictions available for this scan.</td></tr>`;
      return;
    }

    tbody.innerHTML = predictions.map(p => {
      let badgeClass = "badge-low";
      if (p.riskCategory === "CRITICAL") badgeClass = "badge-critical";
      else if (p.riskCategory === "HIGH") badgeClass = "badge-high";
      else if (p.riskCategory === "MEDIUM") badgeClass = "badge-medium";

      return `
        <tr>
          <td><strong>${escapeHtml(p.projectFile.fileName)}</strong></td>
          <td><small>${escapeHtml(p.projectFile.relativePath)}</small></td>
          <td><strong>${p.riskScore.toFixed(1)}</strong></td>
          <td><span class="badge ${badgeClass}">${p.riskCategory}</span></td>
          <td><small>${escapeHtml(p.explanation)}</small></td>
          <td>
            <button class="btn-secondary" style="padding: 3px 8px; font-size: 11px;" onclick="createRemediationTask('${escapeHtml(p.projectFile.fileName)}', '${escapeHtml(p.explanation)}')">Remediate</button>
          </td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    console.error(err);
  }
}

async function pollScanProgress(scanId) {
  const interval = setInterval(async () => {
    try {
      const res = await fetch(`/api/scans/${scanId}`);
      if (!res.ok) return;
      const scan = await res.json();
      if (scan.status === "COMPLETED" || scan.status === "FAILED") {
        clearInterval(interval);
        loadDashboard();
        if (currentProjectId) loadProjectScans(currentProjectId);
      }
    } catch (err) {
      clearInterval(interval);
    }
  }, 2000);
}

function exportCsvReport() {
  if (currentActiveScanId) {
    window.location.href = `/api/reports/scan/${currentActiveScanId}/csv`;
  }
}

function openNewProjectModal() {
  const name = prompt("Enter project name:", "BugPredict Internal");
  if (!name) return;
  const path = prompt("Enter local Java source directory path:", window.location.origin.includes("localhost") ? "C:\\Users\\WINDOWS\\.gemini\\antigravity\\scratch\\bugpredict-ai\\src\\main\\java" : "");
  if (!path) return;

  fetch("/api/projects", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name: name, description: "Java source repository", sourcePath: path })
  }).then(r => r.json()).then(p => {
    alert("Project created: " + p.name);
    loadDashboard();
  });
}

function createRemediationTask(fileName, explanation) {
  if (!currentProjectId) return;
  fetch(`/api/remediation/project/${currentProjectId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      title: "Investigate defect risk in " + fileName,
      description: explanation,
      recommendation: "Refactor complex branching, isolate monolithic methods, and add regression unit tests."
    })
  }).then(r => r.json()).then(t => {
    alert("Remediation task created: " + t.title);
  });
}

function switchView(view) {
  console.log("Switching view to " + view);
}

function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
