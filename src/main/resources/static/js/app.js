let selectedFile = null;
let currentScanId = null;

function onFileSelected(event) {
  selectedFile = event.target.files[0];
  if (!selectedFile) return;

  document.getElementById("selectedFileName").innerText = selectedFile.name;
  document.getElementById("selectedFileSize").innerText = `${(selectedFile.size / 1024).toFixed(1)} KB`;
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

    document.getElementById("scanStatusMsg").innerText = `Analyzing code files and detecting bugs in parallel...`;
    
    // Direct Synchronous Scan Execution
    const scanRes = await fetch(`/api/scans/project/${project.id}/start-sync`, { method: "POST" });
    if (!scanRes.ok) {
      const err = await scanRes.json();
      alert("Scan failed: " + (err.error || "Server error"));
      setStep(1);
      return;
    }
    const scanData = await scanRes.json();
    currentScanId = scanData.scanId;

    await loadDetectedBugs(currentScanId);
  } catch (err) {
    alert("Error: " + err.message);
    setStep(1);
  }
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
      container.innerHTML = `<div style="text-align:center; padding: 40px; color: var(--text-muted);">No source files found in this archive.</div>`;
    } else {
      data.files.forEach(file => {
        const hasBugs = file.bugs && file.bugs.length > 0;

        let bugItemsHtml = "";
        if (!hasBugs) {
          bugItemsHtml = `
            <div style="padding: 18px 24px; color: var(--accent-sage); font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 10px;">
              <span>✅</span> Clean File — No bugs or security risks found.
            </div>
          `;
        } else {
          bugItemsHtml = file.bugs.map((b, idx) => {
            const severityBadge = (b.severity === 'CRITICAL') ? 'pill-bugs' : 'status-pill';

            return `
              <div class="bug-defect-item">
                <div class="bug-title-row">
                  <div style="display: flex; align-items: center; gap: 12px;">
                    <span style="background: #EAE6DF; color: #2C3530; font-family: 'JetBrains Mono', monospace; font-size: 11.5px; font-weight: 700; padding: 3px 8px; border-radius: 4px;">LINE ${b.lineNumber}</span>
                    <span style="font-size: 15px; font-weight: 700; color: #9C1F16;">Bug #${idx + 1}: ${escapeHtml(formatBugTitle(b.ruleName))}</span>
                  </div>
                  <span class="${severityBadge}" style="font-size: 11px; padding: 3px 10px; border-radius: 20px;">${b.severity}</span>
                </div>

                <p style="font-size: 14px; color: var(--text-main); margin: 6px 0 8px; line-height: 1.5;">
                  <strong>Issue:</strong> ${escapeHtml(b.message)}
                </p>

                <div style="font-size: 13px; color: var(--text-secondary); background: #FAF8F5; padding: 10px 14px; border-radius: 6px; border-left: 3px solid var(--accent-sage); margin-bottom: 12px;">
                  <strong>Recommended Fix:</strong> ${escapeHtml(b.recommendation)}
                </div>

                <div class="code-diff-viewer">
                  <div class="diff-line diff-line-bad">
                    <span class="diff-sign">-</span>
                    <span>${escapeHtml(b.originalSnippet)}</span>
                  </div>
                  <div class="diff-line diff-line-good">
                    <span class="diff-sign">+</span>
                    <span>${escapeHtml(b.suggestedPatch)}</span>
                  </div>
                </div>
              </div>
            `;
          }).join("");
        }

        const fileBlock = document.createElement("div");
        fileBlock.className = "file-defect-card";

        fileBlock.innerHTML = `
          <div class="file-defect-header">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span class="lang-tag">${escapeHtml(file.language || "Code")}</span>
              <span style="font-size: 15px; font-weight: 700;">📄 ${escapeHtml(file.fileName)}</span>
              <span style="font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--text-muted);">${escapeHtml(file.relativePath)}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 12px;">
              <span class="bug-counter-pill ${hasBugs ? 'pill-bugs' : 'pill-clean'}">
                ${hasBugs ? '⚠️ ' + file.bugs.length + ' Bug(s)' : '✅ Clean'}
              </span>
              <span style="font-size: 12.5px; font-weight: 700; color: var(--text-secondary);">Risk: ${file.riskScore.toFixed(1)}</span>
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
  btn.innerHTML = `<span>Applying Patches & Generating Clean ZIP...</span>`;

  try {
    const res = await fetch(`/api/workflow/scan/${currentScanId}/apply-corrections`, {
      method: "POST"
    });

    if (res.ok) {
      const result = await res.json();
      document.getElementById("correctedZipMsg").innerText =
        `Successfully corrected all ${result.correctedBugsCount} detected bugs! Your zero-bug project archive is compiled and ready for download.`;
      setStep(4);
    } else {
      alert("Failed to apply corrections.");
    }
  } catch (e) {
    alert("Error: " + e.message);
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<span>✨ Correct Bugs & Generate ZIP</span><span>➔</span>`;
  }
}

function downloadCleanZip() {
  if (currentScanId) {
    const link = document.createElement("a");
    link.href = `/api/reports/scan/${currentScanId}/download-corrected-zip`;
    link.download = `scan-${currentScanId}-corrected-code.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}

function setStep(stepNum) {
  document.getElementById("view-stage-1").style.display = (stepNum === 1) ? "block" : "none";
  document.getElementById("view-stage-2").style.display = (stepNum === 2) ? "block" : "none";
  document.getElementById("view-stage-3").style.display = (stepNum === 3) ? "block" : "none";
  document.getElementById("view-stage-4").style.display = (stepNum === 4) ? "block" : "none";

  for (let i = 1; i <= 4; i++) {
    const el = document.getElementById(`step-nav-${i}`);
    if (el) {
      if (i === stepNum) {
        el.className = "step-item active";
      } else if (i < stepNum) {
        el.className = "step-item completed";
      } else {
        el.className = "step-item";
      }
    }
  }
}

function resetToUpload() {
  selectedFile = null;
  currentScanId = null;
  document.getElementById("zipFileInput").value = "";
  document.getElementById("fileSelectedBar").style.display = "none";
  document.getElementById("uploadPromptText").innerText = "Click to Browse or Drag & Drop Project .ZIP File";
  setStep(1);
}

function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
