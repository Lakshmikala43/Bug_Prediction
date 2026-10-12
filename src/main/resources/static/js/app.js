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
    document.getElementById("bugsDetectedTitle").innerText = `Found ${data.totalBugs} Error(s) Across ${data.scannedFiles} File(s)`;

    const container = document.getElementById("bugDetailsList");
    container.innerHTML = "";

    if (data.files.length === 0) {
      container.innerHTML = `<div style="text-align:center; padding: 30px; color: var(--text-muted);">No source files found in this archive.</div>`;
    } else {
      data.files.forEach(file => {
        let badgeClass = "badge-low";
        if (file.riskCategory === "CRITICAL") badgeClass = "badge-critical";
        else if (file.riskCategory === "HIGH") badgeClass = "badge-high";
        else if (file.riskCategory === "MEDIUM") badgeClass = "badge-medium";

        let bugItemsHtml = "";
        if (file.bugs.length === 0) {
          bugItemsHtml = `<div style="color: var(--accent-sage); font-size: 13px; margin-top: 8px;">✓ Clean file — 0 bugs detected.</div>`;
        } else {
          bugItemsHtml = file.bugs.map((b, idx) => `
            <div class="bug-card">
              <div style="display:flex; justify-content: space-between; align-items:center;">
                <strong>Bug #${idx + 1}: ${escapeHtml(b.ruleName)} (Line ${b.lineNumber})</strong>
                <span class="badge ${b.severity === 'CRITICAL' ? 'badge-critical' : 'badge-high'}">${b.severity}</span>
              </div>
              <p style="font-size: 13.5px; color: var(--text-main); margin-top: 6px;">${escapeHtml(b.message)}</p>
              <div style="font-size: 13px; color: var(--text-muted); margin-top: 4px;"><strong>Proposed Fix:</strong> ${escapeHtml(b.recommendation)}</div>

              <div class="code-snippet">
                <div class="diff-before">❌ Original Line: ${escapeHtml(b.originalSnippet)}</div>
                <div class="diff-after">✅ Fixed Patch:  ${escapeHtml(b.suggestedPatch)}</div>
              </div>
            </div>
          `).join("");
        }

        const fileBlock = document.createElement("div");
        fileBlock.style.border = "1px solid var(--border-subtle)";
        fileBlock.style.borderRadius = "8px";
        fileBlock.style.padding = "16px";
        fileBlock.style.marginBottom = "16px";
        fileBlock.style.background = "#fff";

        fileBlock.innerHTML = `
          <div style="display:flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-subtle); padding-bottom: 10px;">
            <div>
              <span class="badge badge-low">${escapeHtml(file.language || "Code")}</span>
              <strong style="margin-left: 8px; font-size: 14.5px;">${escapeHtml(file.fileName)}</strong>
              <small style="color: var(--text-muted); margin-left: 8px;">${escapeHtml(file.relativePath)}</small>
            </div>
            <div>
              <span style="font-size: 12px; color: var(--text-muted); margin-right: 6px;">Risk: <strong>${file.riskScore.toFixed(1)}</strong></span>
              <span class="badge ${badgeClass}">${file.riskCategory}</span>
            </div>
          </div>
          ${bugItemsHtml}
        `;
        container.appendChild(fileBlock);
      });
    }

    setStep(3);
  } catch (err) {
    alert("Error loading scan details: " + err.message);
  }
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
