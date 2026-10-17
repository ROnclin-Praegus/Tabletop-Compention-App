const fs = require('fs');
let html = fs.readFileSync('docs/index.html', 'utf8');

// 1. Remove "Active Session"
html = html.replace(/\\$\{c\.race\} • Active Session\/g, '\\$\{c.race\}\');

// 2. Dynamic Interval UI
const oldApiAutoSyncHtml = <input id="apiAutoSync" type="checkbox" onchange="toggleAutoSync()">\n          <label style="margin:0;">Auto-sync every 3 mins (Requires open tab)</label>;
const newApiAutoSyncHtml = <input id="apiAutoSync" type="checkbox" onchange="toggleAutoSync()">\n          <label style="margin:0; display:flex; align-items:center; gap:4px;">Auto-sync every <input type="number" id="apiSyncIntervalMins" value="3" style="width:50px; text-align:center; padding:2px; height:auto; color:white; background:var(--bg-dark); border:1px solid var(--border-color); border-radius:4px;" onchange="toggleAutoSync()"> mins (Requires open tab)</label>;
html = html.replace(oldApiAutoSyncHtml, newApiAutoSyncHtml);

// 3. openApiModal update
html = html.replace(
  "document.getElementById('apiAutoSync').checked = localStorage.getItem('apiAutoSync') === 'true';",
  "document.getElementById('apiAutoSync').checked = localStorage.getItem('apiAutoSync') === 'true';\n      document.getElementById('apiSyncIntervalMins').value = localStorage.getItem('apiSyncIntervalMins') || '3';"
);

// 4. closeApiModal update
html = html.replace(
  "localStorage.setItem('apiKeyInput', document.getElementById('apiKeyInput').value.trim());",
  "localStorage.setItem('apiKeyInput', document.getElementById('apiKeyInput').value.trim());\n      const mins = parseInt(document.getElementById('apiSyncIntervalMins').value) || 3;\n      localStorage.setItem('apiSyncIntervalMins', mins);"
);

// 5. toggleAutoSync & startAutoSync
const oldToggleAutoSync = unction toggleAutoSync() {
      const isAuto = document.getElementById('apiAutoSync').checked;
      localStorage.setItem('apiAutoSync', isAuto);
      if (isAuto) {
        startAutoSync();
      } else {
        stopAutoSync();
      }
    }

    function startAutoSync() {
      if (apiSyncInterval) clearInterval(apiSyncInterval);
      apiSyncInterval = setInterval(() => {
        manualApiSync(true);
      }, 3 * 60 * 1000); // 3 minutes
    };

const newToggleAutoSync = unction toggleAutoSync() {
      const isAuto = document.getElementById('apiAutoSync').checked;
      const mins = parseInt(document.getElementById('apiSyncIntervalMins').value) || 3;
      localStorage.setItem('apiAutoSync', isAuto);
      localStorage.setItem('apiSyncIntervalMins', mins);
      if (isAuto) {
        startAutoSync();
      } else {
        stopAutoSync();
      }
    }

    function startAutoSync() {
      if (apiSyncInterval) clearInterval(apiSyncInterval);
      const mins = parseInt(localStorage.getItem('apiSyncIntervalMins')) || 3;
      apiSyncInterval = setInterval(() => {
        manualApiSync(true);
      }, mins * 60 * 1000);
    };
html = html.replace(oldToggleAutoSync, newToggleAutoSync);

// 6. manualApiSync update
const manualSyncCode = sync function manualApiSync(silent = false) {
      if (!activeCharId) return;

      const url = document.getElementById('apiServerUrl').value.trim() || localStorage.getItem('apiServerUrl');;

const newManualSyncCode = let pendingSyncTimeout = null;
    async function manualApiSync(silent = false) {
      if (!activeCharId) return;
      
      if (silent) {
         const hasOpenDialog = Array.from(document.querySelectorAll('.modal-overlay')).some(modal => !modal.classList.contains('hidden'));
         if (hasOpenDialog) {
           if (pendingSyncTimeout) clearTimeout(pendingSyncTimeout);
           
           let toast = document.getElementById('syncWaitingToast');
           if (!toast) {
               toast = document.createElement('div');
               toast.id = 'syncWaitingToast';
               toast.innerText = "Website is waiting for you to finish to Sync...";
               toast.style.position = 'fixed';
               toast.style.bottom = '20px';
               toast.style.left = '50%';
               toast.style.transform = 'translateX(-50%)';
               toast.style.backgroundColor = 'var(--gold-accent)';
               toast.style.color = '#000';
               toast.style.padding = '8px 16px';
               toast.style.borderRadius = '20px';
               toast.style.zIndex = '99999';
               toast.style.fontWeight = 'bold';
               toast.style.fontSize = '12px';
               toast.style.boxShadow = '0 4px 6px rgba(0,0,0,0.3)';
               document.body.appendChild(toast);
           }
           
           pendingSyncTimeout = setTimeout(() => {
               manualApiSync(true);
           }, 3000);
           return;
         } else {
             const toast = document.getElementById('syncWaitingToast');
             if (toast) toast.parentNode.removeChild(toast);
         }
      }

      const url = document.getElementById('apiServerUrl').value.trim() || localStorage.getItem('apiServerUrl');;
html = html.replace(manualSyncCode, newManualSyncCode);

fs.writeFileSync('docs/index.html', html);
