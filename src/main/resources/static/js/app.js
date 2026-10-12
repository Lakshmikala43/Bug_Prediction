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
    document.getElementById("scanStatusMsg").innerText = `Uploading and scanning ${selectedFile.name}...`;
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

    document.getElementById("scanStatusMsg").innerText = `Scanning codebase and identifying bugs...`;
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
              <div class="bug-entry">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                  <div style="display: flex; align-items: center; gap: 10px;">
                    <span style="background: #EFECE6; padding: 2px 7px; border-radius: 4px; font-size: 11.5px; font-weight: 700; font-family: Consolas;">LINE ${b.lineNumber}</span>
                    <strong style="color: #9C201C; font-size: 14px;">Bug #${idx + 1}: ${escapeHtml(b.ruleName.replace(/_/g, ' '))}</strong>
                  </div>
                  <span class="badge ${severityBadge}">${b.severity}</span>
                </div>

                <div style="font-size: 13.5px; color: #2D3748; margin-bottom: 6px;">
                  <strong>Issue:</strong> ${escapeHtml(b.message)}
                </div>

                <div style="font-size: 13px; color: #4A5568; background: #F7FAFC; padding: 8px 12px; border-radius: 4px; border-left: 3px solid var(--accent-sage); margin-bottom: 8px;">
                  <strong>Recommended Fix:</strong> ${escapeHtml(b.recommendation)}
                </div>

                <div class="code-box">
                  <div class="diff-minus">❌ Original: ${escapeHtml(b.originalSnippet)}</div>
                  <div class="diff-plus">✅ Patch:    ${escapeHtml(b.suggestedPatch)}</div>
                </div>
              </div>
            `;
          }).join("");
        }

        const fileBlock = document.createElement("div");
        fileBlock.className = "file-box";

        let riskBadge = "badge-low";
        if (file.riskCategory === "CRITICAL") riskBadge = "badge-critical";
        else if (file.riskCategory === "HIGH") riskBadge = "badge-high";
        else if (file.riskCategory === "MEDIUM") riskBadge = "badge-medium";

        fileBlock.innerHTML = `
          <div class="file-box-header">
            <div>
              <span class="badge badge-low">${escapeHtml(file.language || "Code")}</span>
              <strong style="margin-left: 8px; font-size: 14.5px;">📄 ${escapeHtml(file.fileName)}</strong>
              <small style="color: var(--text-muted); margin-left: 8px; font-family: Consolas;">${escapeHtml(file.relativePath)}</small>
            </div>
            <div style="display: flex; align-items: center; gap: 12px;">
              <span class="badge ${hasBugs ? 'badge-high' : 'badge-low'}">
                ${hasBugs ? '⚠️ ' + file.bugs.length + ' Bug(s)' : '✅ Clean'}
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

async function triggerBugCorrection() {
  if (!currentScanId) return;

  const btn = document.getElementById("btnCorrectBugs");
  btn.disabled = true;
  btn.innerText = "Applying Patches & Compiling Clean ZIP...";

  try {
    const res = await fetch(`/api/workflow/scan/${currentScanId}/apply-corrections`, {
      method: "POST"
    });

    if (res.ok) {
      const result = await res.json();
      document.getElementById("correctedZipMsg").innerText =
        `Successfully corrected all ${result.correctedBugsCount} detected bugs! Your bug-free project is ready to download.`;
      setStep(4);
    } else {
      alert("Failed to apply corrections.");
    }
  } catch (e) {
    alert("Error: " + e.message);
  } finally {
    btn.disabled = false;
    btn.innerText = "✨ Correct Bugs ➔";
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
