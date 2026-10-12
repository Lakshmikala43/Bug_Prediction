let selectedFile = null;
let currentScanId = null;

function onFileSelected(event) {
  selectedFile = event.target.files[0];
  if (!selectedFile) return;

  document.getElementById("selectedFileName").innerText = selectedFile.name;
  document.getElementById("selectedFileSize").innerText = `(${(selectedFile.size / 1024).toFixed(1)} KB)`;
  document.getElementById("fileSelectedBar").style.display = "flex";
  document.getElementById("uploadPromptText").innerText = "Selected: " + selectedFile.name;
}

async function startUploadAndScan() {
  if (!selectedFile) {
    alert("Please select a .zip file first.");
    return;
  }

  setStep(2);

  const formData = new FormData();
  formData.append("name", selectedFile.name.replace(/\.[^/.]+$/, ""));
  formData.append("file", selectedFile);

  try {
    document.getElementById("scanStatusMsg").innerText = `Uploading and extracting ${selectedFile.name}...`;
    const uploadRes = await fetch("/api/projects/upload-zip", {
      method: "POST",
      body: formData
    });

    if (!uploadRes.ok) {
      const err = await uploadRes.json();
      alert("Upload failed: " + (err.error || "Unknown error"));
      setStep(1);
      return;
    }

    const project = await uploadRes.json();

    document.getElementById("scanStatusMsg").innerText = `Scanning files in parallel across all programming languages...`;
    const scanRes = await fetch(`/api/scans/project/${project.id}/start`, { method: "POST" });
    const scanData = await scanRes.json();
    currentScanId = scanData.scanId;

    pollScan(scanData.scanId);
  } catch (err) {
    alert("Error: " + err.message);
    setStep(1);
  }
}

function pollScan(scanId) {
  const interval = setInterval(async () => {
    try {
      const res = await fetch(`/api/scans/${scanId}`);
      if (!res.ok) return;
      const scan = await res.json();
      if (scan.status === "COMPLETED") {
        clearInterval(interval);
        loadDetectedBugs(scanId);
      } else if (scan.status === "FAILED") {
        clearInterval(interval);
        alert("Scan failed: " + scan.errorMessage);
        setStep(1);
      }
    } catch (e) {
      clearInterval(interval);
    }
  }, 1000);
}

async function loadDetectedBugs(scanId) {
  try {
    const res = await fetch(`/api/workflow/scan/${scanId}/details`);
    if (!res.ok) return;
    const data = await res.json();

    document.getElementById("resScannedFiles").innerText = data.scannedFiles;
    document.getElementById("resTotalBugs").innerText = data.totalBugs;
    document.getElementById("resAvgRisk").innerText = (data.avgRiskScore || 0).toFixed(1);
    document.getElementById("bugsDetectedTitle").innerText = `Found ${data.totalBugs} Bug(s) Across ${data.scannedFiles} File(s)`;

    const container = document.getElementById("bugDetailsList");
    container.innerHTML = "";

    if (data.files.length === 0) {
      container.innerHTML = `<div style="text-align:center; padding: 30px; color: var(--text-muted);">No source files found in this archive.</div>`;
    } else {
      data.files.forEach(file => {
        const hasBugs = file.bugs && file.bugs.length > 0;

        let bugItemsHtml = "";
        if (!hasBugs) {
          bugItemsHtml = `
            <div style="padding: 16px 20px; color: var(--accent-sage); font-size: 13.5px; display: flex; align-items: center; gap: 8px;">
              <span>✅</span> Clean File — 0 defects or bugs detected.
            </div>
          `;
        } else {
          bugItemsHtml = file.bugs.map((b, idx) => {
            const severityBadge = (b.severity === 'CRITICAL') ? 'badge-critical' : 'badge-high';

            return `
              <div class="bug-item-row">
                <div class="bug-header-line">
                  <div style="display: flex; align-items: center; gap: 10px;">
                    <span class="line-indicator">LINE ${b.lineNumber}</span>
                    <span class="bug-type-tag">Bug #${idx + 1}: ${escapeHtml(formatBugTitle(b.ruleName))}</span>
                  </div>
                  <span class="badge ${severityBadge}">${b.severity}</span>
                </div>

                <div class="bug-desc-text">
                  <strong>Issue:</strong> ${escapeHtml(b.message)}
                </div>

                <div class="bug-fix-hint">
                  <strong>Recommended Fix:</strong> ${escapeHtml(b.recommendation)}
                </div>

                <table class="code-diff-table">
                  <tr class="diff-row-bad">
                    <td style="width: 30px; font-weight: bold; text-align: center;">-</td>
                    <td><strong>Detected Bug:</strong> ${escapeHtml(b.originalSnippet)}</td>
                  </tr>
                  <tr class="diff-row-good">
                    <td style="width: 30px; font-weight: bold; text-align: center;">+</td>
                    <td><strong>Automated Patch:</strong> ${escapeHtml(b.suggestedPatch)}</td>
                  </tr>
                </table>
              </div>
            `;
          }).join("");
        }

        const fileBlock = document.createElement("div");
        fileBlock.className = "file-card-box";

        let riskBadge = "badge-low";
        if (file.riskCategory === "CRITICAL") riskBadge = "badge-critical";
        else if (file.riskCategory === "HIGH") riskBadge = "badge-high";
        else if (file.riskCategory === "MEDIUM") riskBadge = "badge-medium";

        fileBlock.innerHTML = `
          <div class="file-card-header">
            <div>
              <span class="badge badge-low">${escapeHtml(file.language || "Code")}</span>
              <span class="file-name-title" style="margin-left: 8px;">📄 ${escapeHtml(file.fileName)}</span>
              <span class="file-path-sub">${escapeHtml(file.relativePath)}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 12px;">
              <span class="bug-counter-pill ${hasBugs ? 'bug-counter-has-bugs' : 'bug-counter-clean'}">
                ${hasBugs ? '⚠️ ' + file.bugs.length + ' Bug(s) Detected' : '✅ 0 Bugs'}
              </span>
              <span class="badge ${riskBadge}">${file.riskCategory} RISK</span>
            </div>
          </div>
          <div>
            ${bugItemsHtml}
          </div>
        `;
        container.appendChild(fileBlock);
      });
    }

    setStep(3);
  } catch (err) {
    alert("Error loading scan details: " + err.message);
  }
}

function formatBugTitle(ruleName) {
  if (!ruleName) return "Defect";
  return ruleName.replace(/_/g, " ");
}

async function triggerBugCorrection() {
  if (!currentScanId) return;

  const btn = document.getElementById("btnTriggerCorrection");
  btn.disabled = true;
  btn.innerText = "Applying Patches & Compiling Clean ZIP...";

  try {
    const res = await fetch(`/api/workflow/scan/${currentScanId}/apply-corrections`, {
      method: "POST"
    });

    if (res.ok) {
      const result = await res.json();
      document.getElementById("correctedZipMsg").innerText =
        `Successfully corrected all ${result.correctedBugsCount} detected bugs! Your fixed project is ready to download.`;
      setStep(4);
    } else {
      alert("Failed to apply corrections.");
    }
  } catch (e) {
    alert("Error: " + e.message);
  } finally {
    btn.disabled = false;
    btn.innerText = "✨ Correct Bugs & Generate ZIP ➔";
  }
}

function downloadCleanZip() {
  if (currentScanId) {
    window.location.href = `/api/reports/scan/${currentScanId}/download-corrected-zip`;
  }
}

function setStep(stepNum) {
  document.getElementById("step-1").style.display = (stepNum === 1) ? "block" : "none";
  document.getElementById("step-2").style.display = (stepNum === 2) ? "block" : "none";
  document.getElementById("step-3").style.display = (stepNum === 3) ? "block" : "none";
  document.getElementById("step-4").style.display = (stepNum === 4) ? "block" : "none";

  for (let i = 1; i <= 4; i++) {
    const el = document.getElementById(`indicator-step-${i}`);
    if (el) {
      if (i <= stepNum) {
        el.classList.add("active");
      } else {
        el.classList.remove("active");
      }
    }
  }
}

function resetToUpload() {
  selectedFile = null;
  currentScanId = null;
  document.getElementById("zipFileInput").value = "";
  document.getElementById("fileSelectedBar").style.display = "none";
  document.getElementById("uploadPromptText").innerText = "Click here to browse and select a project .ZIP file";
  setStep(1);
}

function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
