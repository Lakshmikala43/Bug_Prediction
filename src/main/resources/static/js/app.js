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
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 20px;">No projects yet. Click "+ New Project" or "Upload ZIP" to get started.</td></tr>`;
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
    const res = await fetch(`/api/scans/project/${projectId}/start-sync`, { method: "POST" });
    if (res.ok) {
      const data = await res.json();
      currentActiveScanId = data.scanId;
      currentProjectId = projectId;
      alert(`Scan completed successfully!`);
      loadProjectScans(projectId);
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

      const zipBtn = document.getElementById("btnDownloadCorrectedZip");
      if (zipBtn) zipBtn.style.display = "inline-block";

      loadPredictions(latestScan.id);
    }
  } catch (err) {
    console.error(err);
  }
}

async function loadPredictions(scanId) {
  try {
    const res = await fetch(`/api/workflow/scan/${scanId}/details`);
    if (!res.ok) return;
    const data = await res.json();
    const tbody = document.getElementById("predictionsTableBody");
    document.getElementById("btnExportCsv").style.display = data.files.length > 0 ? "inline-block" : "none";

    const correctBtn = document.getElementById("btnCorrectBugsHeader");
    if (correctBtn) correctBtn.style.display = data.files.length > 0 ? "inline-block" : "none";

    if (data.files.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 20px;">No predictions available for this scan.</td></tr>`;
      return;
    }

    tbody.innerHTML = data.files.map(p => {
      let badgeClass = "badge-low";
      if (p.riskCategory === "CRITICAL") badgeClass = "badge-critical";
      else if (p.riskCategory === "HIGH") badgeClass = "badge-high";
      else if (p.riskCategory === "MEDIUM") badgeClass = "badge-medium";

      let bugsText = "";
      if (p.bugs && p.bugs.length > 0) {
        bugsText = p.bugs.map((b, i) => `<div style="margin-bottom:4px;"><strong>Line ${b.lineNumber}</strong>: <span style="color:#C5221F;">${escapeHtml(b.ruleName)}</span> — ${escapeHtml(b.message)}</div>`).join("");
      } else {
        bugsText = `<span style="color:#137333;">✓ 0 bugs detected</span>`;
      }

      return `
        <tr>
          <td><strong>${escapeHtml(p.fileName)}</strong></td>
          <td><small>${escapeHtml(p.relativePath)}</small></td>
          <td><strong>${p.riskScore.toFixed(1)}</strong></td>
          <td><span class="badge ${badgeClass}">${p.riskCategory}</span></td>
          <td><small>${bugsText}</small></td>
          <td>
            <button class="btn-secondary" style="padding: 3px 8px; font-size: 11px;" onclick="createRemediationTask('${escapeHtml(p.fileName)}', '${escapeHtml(p.language)}')">Remediate</button>
          </td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    console.error(err);
  }
}

async function triggerCorrection() {
  if (!currentActiveScanId) return;
  const btn = document.getElementById("btnCorrectBugsHeader");
  if (btn) {
    btn.disabled = true;
    btn.innerText = "Applying Patches...";
  }

  try {
    const res = await fetch(`/api/workflow/scan/${currentActiveScanId}/apply-corrections`, { method: "POST" });
    if (res.ok) {
      const data = await res.json();
      alert(`Successfully corrected ${data.correctedBugsCount} bugs! Clean ZIP is ready.`);
      const zipBtn = document.getElementById("btnDownloadCorrectedZip");
      if (zipBtn) zipBtn.style.display = "inline-block";
    } else {
      alert("Failed to apply corrections");
    }
  } catch (err) {
    alert("Error: " + err.message);
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerText = "✨ Correct Bugs";
    }
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

function openNewProjectModal() {
  const name = prompt("Enter project name:", "BugPredict Codebase");
  if (!name) return;
  const path = prompt("Enter local source directory path:", "C:\\Users\\WINDOWS\\.gemini\\antigravity\\scratch\\bugpredict-ai\\src\\main\\java");
  if (!path) return;

  fetch("/api/projects", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name: name, description: "Source repository", sourcePath: path })
  }).then(r => r.json()).then(p => {
    alert("Project created: " + p.name);
    loadDashboard();
  });
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

  try {
    const res = await fetch("/api/projects/upload-zip", {
      method: "POST",
      body: formData
    });
    if (res.ok) {
      const p = await res.json();
      alert(`Project "${p.name}" uploaded successfully! Scanning now...`);
      currentProjectId = p.id;
      loadDashboard();
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

function createRemediationTask(fileName, lang) {
  if (!currentProjectId) return;
  fetch(`/api/remediation/project/${currentProjectId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      title: "Investigate defect risk in " + fileName,
      description: "Code analysis finding for " + lang,
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
