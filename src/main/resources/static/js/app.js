let currentActiveScanId = null;
let currentProjectId = null;
let currentPredictions = [];
let riskChart = null;

document.addEventListener("DOMContentLoaded", () => {
  initChart();
  loadDashboard();
});

function switchView(viewName) {
  const views = ["dashboard", "projects", "scans", "predictions", "remediation"];
  views.forEach(v => {
    const el = document.getElementById(`view-${v}`);
    const nav = document.getElementById(`nav-${v}`);
    if (el) el.style.display = (v === viewName) ? "block" : "none";
    if (nav) {
      if (v === viewName) nav.classList.add("active");
      else nav.classList.remove("active");
    }
  });

  const titleMap = {
    dashboard: "Risk Overview & Analytics",
    projects: "Project Codebase Management",
    scans: "Code Scans & AST Execution",
    predictions: "Defect Predictions & Evidence",
    remediation: "Remediation & Testing Priorities"
  };
  document.getElementById("pageTitle").innerText = titleMap[viewName] || "BugPredict AI";

  if (viewName === "projects") loadProjectsTable();
  if (viewName === "scans") loadScansList();
  if (viewName === "predictions") renderAllPredictions();
  if (viewName === "remediation") loadRemediationTasks();
}

function initChart() {
  const ctx = document.getElementById("riskDistributionChart");
  if (!ctx) return;
  riskChart = new Chart(ctx, {
    type: "doughnut",
    data: {
      labels: ["Low Risk", "Medium Risk", "High Risk", "Critical Risk"],
      datasets: [{
        data: [0, 0, 0, 0],
        backgroundColor: ["#3E885B", "#D9822B", "#D96B43", "#8C1D18"]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: "right" }
      }
    }
  });
}

function updateChart(low, med, high, crit) {
  if (!riskChart) return;
  riskChart.data.datasets[0].data = [low, med, high, crit];
  riskChart.update();
}

async function loadDashboard() {
  try {
    const res = await fetch("/api/projects");
    if (!res.ok) return;
    const projects = await res.json();
    document.getElementById("statProjects").innerText = projects.length;

    if (projects.length === 0) {
      resetScanDisplay();
      return;
    }

    if (!currentProjectId && projects.length > 0) {
      currentProjectId = projects[0].id;
    }
    if (currentProjectId) {
      loadProjectScans(currentProjectId);
    }
  } catch (err) {
    console.error("Failed to load dashboard:", err);
  }
}

function resetScanDisplay() {
  document.getElementById("statFiles").innerText = "0";
  document.getElementById("statHighRisk").innerText = "0";
  document.getElementById("statAvgRisk").innerText = "0.0";
  updateChart(0, 0, 0, 0);
  document.getElementById("dashboardPredictionsBody").innerHTML =
    `<tr><td colspan="6" style="text-align:center; padding: 30px; color: var(--text-muted);">No scan executed yet. Upload a ZIP or run a scan to inspect your codebase.</td></tr>`;
  document.getElementById("btnExportCsv").style.display = "none";
  document.getElementById("btnDownloadCorrectedZip").style.display = "none";
}

async function loadProjectsTable() {
  try {
    const res = await fetch("/api/projects");
    if (!res.ok) return;
    const projects = await res.json();
    const tbody = document.getElementById("projectsTableBody");

    if (projects.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 25px; color: var(--text-muted);">No projects found. Click "+ Upload ZIP" to get started.</td></tr>`;
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
          <button class="btn-primary" style="padding: 4px 10px; font-size: 12px;" onclick="triggerScan(${p.id})">Scan & Correct</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    console.error(err);
  }
}

async function loadScansList() {
  if (!currentProjectId) {
    document.getElementById("scansTableBody").innerHTML =
      `<tr><td colspan="7" style="text-align:center; padding: 25px; color: var(--text-muted);">No active project selected.</td></tr>`;
    return;
  }
  try {
    const res = await fetch(`/api/scans/project/${currentProjectId}`);
    if (!res.ok) return;
    const scans = await res.json();
    const tbody = document.getElementById("scansTableBody");

    if (scans.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 25px; color: var(--text-muted);">No scans run for this project yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = scans.map(s => `
      <tr>
        <td>#${s.id}</td>
        <td><strong>${escapeHtml(s.project.name)}</strong></td>
        <td>${s.scannedFiles} / ${s.totalFiles}</td>
        <td><span class="badge ${s.status === 'COMPLETED' ? 'badge-low' : 'badge-high'}">${s.status}</span></td>
        <td><strong>${(s.avgRiskScore || 0).toFixed(1)}</strong></td>
        <td><span style="color: var(--accent-crimson); font-weight: 700;">${(s.highRiskCount || 0) + (s.criticalRiskCount || 0)}</span></td>
        <td>
          <button class="btn-primary" style="padding: 3px 8px; font-size: 11px;" onclick="window.location.href='/api/reports/scan/${s.id}/download-corrected-zip'">⬇️ ZIP</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    console.error(err);
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
      document.getElementById("scanStatusBadge").innerHTML =
        `<strong>Latest Scan #${latestScan.id}</strong>: ${latestScan.status} (${latestScan.scannedFiles} files scanned across any language).`;

      document.getElementById("btnDownloadCorrectedZip").style.display = "inline-block";
      document.getElementById("btnExportCsv").style.display = "inline-block";

      updateChart(latestScan.lowRiskCount || 0, latestScan.mediumRiskCount || 0, latestScan.highRiskCount || 0, latestScan.criticalRiskCount || 0);
      loadPredictions(latestScan.id);
    } else {
      resetScanDisplay();
    }
  } catch (err) {
    console.error(err);
  }
}

async function loadPredictions(scanId) {
  try {
    const res = await fetch(`/api/analysis/scan/${scanId}/predictions`);
    if (!res.ok) return;
    currentPredictions = await res.json();
    renderPredictionsTable(currentPredictions);
  } catch (err) {
    console.error(err);
  }
}

function renderPredictionsTable(predictions) {
  const tbody = document.getElementById("dashboardPredictionsBody");

  if (predictions.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 25px; color: var(--text-muted);">No predictions for this scan.</td></tr>`;
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
        <td><small><span class="badge badge-low">${escapeHtml(p.projectFile.packageName || "Source")}</span> ${escapeHtml(p.projectFile.relativePath)}</small></td>
        <td><strong>${p.riskScore.toFixed(1)}</strong></td>
        <td><span class="badge ${badgeClass}">${p.riskCategory}</span></td>
        <td><small style="color: #4A5568;">${escapeHtml(p.explanation)}</small></td>
        <td>
          <button class="btn-secondary" style="padding: 4px 8px; font-size: 11px;" onclick="createRemediationTask('${escapeHtml(p.projectFile.fileName)}', '${escapeHtml(p.explanation)}')">Remediate</button>
        </td>
      </tr>
    `;
  }).join("");
}

function renderAllPredictions() {
  const tbody = document.getElementById("allPredictionsBody");
  if (!currentPredictions || currentPredictions.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 25px; color: var(--text-muted);">No predictions available. Run a scan first.</td></tr>`;
    return;
  }

  tbody.innerHTML = currentPredictions.map(p => {
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
        <td>${(p.confidenceScore * 100).toFixed(0)}%</td>
        <td><small>${escapeHtml(p.explanation)}</small></td>
        <td>
          <button class="btn-secondary" style="padding: 3px 8px; font-size: 11px;" onclick="createRemediationTask('${escapeHtml(p.projectFile.fileName)}', '${escapeHtml(p.explanation)}')">Task</button>
        </td>
      </tr>
    `;
  }).join("");
}

function filterPredictions() {
  const query = document.getElementById("filterInput").value.toLowerCase();
  const filtered = currentPredictions.filter(p => p.projectFile.fileName.toLowerCase().includes(query) || p.projectFile.relativePath.toLowerCase().includes(query));
  
  const tbody = document.getElementById("allPredictionsBody");
  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 20px;">No matching files found.</td></tr>`;
    return;
  }
  tbody.innerHTML = filtered.map(p => {
    let badgeClass = (p.riskCategory === "CRITICAL") ? "badge-critical" : (p.riskCategory === "HIGH") ? "badge-high" : (p.riskCategory === "MEDIUM") ? "badge-medium" : "badge-low";
    return `
      <tr>
        <td><strong>${escapeHtml(p.projectFile.fileName)}</strong></td>
        <td><small>${escapeHtml(p.projectFile.relativePath)}</small></td>
        <td><strong>${p.riskScore.toFixed(1)}</strong></td>
        <td><span class="badge ${badgeClass}">${p.riskCategory}</span></td>
        <td>${(p.confidenceScore * 100).toFixed(0)}%</td>
        <td><small>${escapeHtml(p.explanation)}</small></td>
        <td>
          <button class="btn-secondary" style="padding: 3px 8px; font-size: 11px;" onclick="createRemediationTask('${escapeHtml(p.projectFile.fileName)}', '${escapeHtml(p.explanation)}')">Task</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function triggerScan(projectId) {
  try {
    const res = await fetch(`/api/scans/project/${projectId}/start`, { method: "POST" });
    if (res.ok) {
      const data = await res.json();
      currentActiveScanId = data.scanId;
      currentProjectId = projectId;
      document.getElementById("scanStatusBadge").innerHTML = `<em>Scan #${data.scanId} running in background & auto-correcting code...</em>`;
      pollScanProgress(data.scanId);
    }
  } catch (err) {
    alert("Scan trigger error: " + err.message);
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
        loadProjectScans(scan.project.id);
        alert(`Scan completed for ${scan.project.name}! Scanned and corrected ${scan.scannedFiles} code files.`);
      }
    } catch (err) {
      clearInterval(interval);
    }
  }, 1500);
}

function openUploadZipModal() {
  document.getElementById("zipFileInput").click();
}

async function handleZipUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  const projectName = prompt("Enter project name for uploaded archive:", file.name.replace(/\.[^/.]+$/, ""));
  if (!projectName) return;

  const formData = new FormData();
  formData.append("name", projectName);
  formData.append("file", file);

  document.getElementById("scanStatusBadge").innerHTML = `<em>Uploading and extracting ${file.name}...</em>`;

  try {
    const res = await fetch("/api/projects/upload-zip", {
      method: "POST",
      body: formData
    });
    if (res.ok) {
      const p = await res.json();
      alert(`Project "${p.name}" uploaded successfully! Starting scan and bug detection...`);
      currentProjectId = p.id;
      triggerScan(p.id);
    } else {
      const err = await res.json();
      alert("Upload failed: " + (err.error || "Unknown error"));
    }
  } catch (e) {
    alert("Upload error: " + e.message);
  } finally {
    event.target.value = "";
  }
}

function downloadCorrectedZip() {
  if (currentActiveScanId) {
    window.location.href = `/api/reports/scan/${currentActiveScanId}/download-corrected-zip`;
  }
}

function exportCsvReport() {
  if (currentActiveScanId) {
    window.location.href = `/api/reports/scan/${currentActiveScanId}/csv`;
  }
}

async function loadRemediationTasks() {
  if (!currentProjectId) return;
  try {
    const res = await fetch(`/api/remediation/project/${currentProjectId}`);
    if (!res.ok) return;
    const tasks = await res.json();
    const tbody = document.getElementById("remediationTableBody");

    if (tasks.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 25px; color: var(--text-muted);">No remediation tasks created yet. Click "Remediate" on any file finding to add one.</td></tr>`;
      return;
    }

    tbody.innerHTML = tasks.map(t => `
      <tr>
        <td>#${t.id}</td>
        <td><strong>${escapeHtml(t.title)}</strong></td>
        <td><span class="badge badge-high">${t.priority}</span></td>
        <td><span class="badge ${t.status === 'RESOLVED' ? 'badge-low' : 'badge-medium'}">${t.status}</span></td>
        <td><small>${escapeHtml(t.recommendationText)}</small></td>
        <td>
          <select onchange="updateTaskStatus(${t.id}, this.value)" style="padding: 4px; font-size: 12px; border-radius: 4px; border: 1px solid var(--border-subtle);">
            <option value="OPEN" ${t.status === 'OPEN' ? 'selected' : ''}>OPEN</option>
            <option value="IN_PROGRESS" ${t.status === 'IN_PROGRESS' ? 'selected' : ''}>IN_PROGRESS</option>
            <option value="RESOLVED" ${t.status === 'RESOLVED' ? 'selected' : ''}>RESOLVED</option>
            <option value="DISMISSED" ${t.status === 'DISMISSED' ? 'selected' : ''}>DISMISSED</option>
          </select>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    console.error(err);
  }
}

async function updateTaskStatus(taskId, status) {
  try {
    await fetch(`/api/remediation/${taskId}/status?status=${status}`, { method: "PATCH" });
    loadRemediationTasks();
  } catch (err) {
    alert("Failed to update status: " + err.message);
  }
}

function createRemediationTask(fileName, explanation) {
  if (!currentProjectId) {
    alert("Please select or scan a project first.");
    return;
  }
  fetch(`/api/remediation/project/${currentProjectId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      title: "Remediate defect risk: " + fileName,
      description: explanation,
      recommendation: "Refactor complex branching, isolate monolithic methods, and add regression unit tests."
    })
  }).then(r => r.json()).then(t => {
    alert("Remediation task created: #" + t.id);
  });
}

function openNewProjectModal() {
  const name = prompt("Project Name:", "My Codebase");
  if (!name) return;
  const path = prompt("Absolute Source Directory:", "C:\\Users\\WINDOWS\\.gemini\\antigravity\\scratch\\bugpredict-ai\\src\\main\\java");
  if (!path) return;

  fetch("/api/projects", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name: name, description: "Source repository", sourcePath: path })
  }).then(r => r.json()).then(p => {
    alert("Project created: " + p.name);
    currentProjectId = p.id;
    loadDashboard();
  });
}

function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
