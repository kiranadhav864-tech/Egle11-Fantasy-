/**
 * Egle11 Cricket Fantasy - Dedicated Admin Console Client Controller
 */

let currentAuthToken = localStorage.getItem("egle11_admin_token") || "";

// DOM Elements
const loginModal = document.getElementById("login-modal");
const adminKeyInput = document.getElementById("admin-key-input");
const loginBtn = document.getElementById("login-btn");
const loginError = document.getElementById("login-error");
const logoutBtn = document.getElementById("logout-btn");
const pageTitle = document.getElementById("page-title");
const navItems = document.querySelectorAll(".nav-item");
const tabPanes = document.querySelectorAll(".tab-pane");

// Initialization
document.addEventListener("DOMContentLoaded", () => {
  if (currentAuthToken) {
    loginModal.style.display = "none";
    loadDashboardData();
  } else {
    loginModal.style.display = "flex";
  }

  setupEventListeners();
});

function setupEventListeners() {
  // Login
  loginBtn.addEventListener("click", handleLogin);
  adminKeyInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") handleLogin();
  });

  // Logout
  logoutBtn.addEventListener("click", () => {
    localStorage.removeItem("egle11_admin_token");
    currentAuthToken = "";
    loginModal.style.display = "flex";
  });

  // Navigation tabs
  navItems.forEach(item => {
    item.addEventListener("click", () => {
      const tabName = item.getAttribute("data-tab");
      switchTab(tabName);
    });
  });

  // Refresh Diagnostics
  const diagBtn = document.getElementById("btn-refresh-diag");
  if (diagBtn) {
    diagBtn.addEventListener("click", loadDiagnostics);
  }

  // Settings form
  const settingsForm = document.getElementById("settings-form");
  if (settingsForm) {
    settingsForm.addEventListener("submit", handleSaveSettings);
  }
}

async function handleLogin() {
  const key = adminKeyInput.value.trim();
  if (!key) {
    showLoginError("Please enter the Admin Master Key.");
    return;
  }

  loginBtn.disabled = true;
  loginBtn.textContent = "Authorizing...";

  try {
    const res = await fetch("/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ adminKey: key })
    });

    const data = await res.json();
    if (res.ok && data.token) {
      currentAuthToken = data.token;
      localStorage.setItem("egle11_admin_token", currentAuthToken);
      loginModal.style.display = "none";
      loadDashboardData();
    } else {
      showLoginError(data.error || "Authentication failed. Invalid master key.");
    }
  } catch (err) {
    showLoginError("Failed to reach Admin Server: " + err.message);
  } finally {
    loginBtn.disabled = false;
    loginBtn.textContent = "Authorize & Enter Console";
  }
}

function showLoginError(msg) {
  loginError.textContent = msg;
  loginError.style.display = "block";
}

function switchTab(tabName) {
  navItems.forEach(n => n.classList.remove("active"));
  tabPanes.forEach(p => p.classList.remove("active"));

  const targetNav = document.querySelector(`.nav-item[data-tab="${tabName}"]`);
  const targetPane = document.getElementById(`tab-${tabName}`);

  if (targetNav) targetNav.classList.add("active");
  if (targetPane) targetPane.classList.add("active");

  const titles = {
    overview: "Overview & Metrics",
    matches: "Fixtures & Matches",
    contests: "Contests & Prizes",
    withdrawals: "UPI Withdrawals",
    users: "Users & Wallets",
    settings: "Platform Settings",
    diagnostics: "Firebase Diagnostics"
  };

  pageTitle.textContent = titles[tabName] || "Admin Console";

  // Trigger data loader for the active tab
  if (tabName === "overview") loadOverview();
  if (tabName === "matches") loadMatches();
  if (tabName === "contests") loadContests();
  if (tabName === "withdrawals") loadWithdrawals();
  if (tabName === "users") loadUsers();
  if (tabName === "settings") loadSettings();
  if (tabName === "diagnostics") loadDiagnostics();
}

async function apiFetch(endpoint, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${currentAuthToken}`,
    ...options.headers
  };

  const res = await fetch(endpoint, { ...options, headers });
  if (res.status === 401) {
    localStorage.removeItem("egle11_admin_token");
    currentAuthToken = "";
    loginModal.style.display = "flex";
    throw new Error("Session expired. Please log in again.");
  }
  return res.json();
}

function loadDashboardData() {
  loadOverview();
  loadDiagnostics();
}

async function loadOverview() {
  try {
    const data = await apiFetch("/api/admin/overview");
    document.getElementById("stat-matches").textContent = data.totalMatches ?? "-";
    document.getElementById("stat-contests").textContent = data.totalContests ?? "-";
    document.getElementById("stat-withdrawals").textContent = data.pendingWithdrawals ?? "-";
    document.getElementById("stat-users").textContent = data.totalUsers ?? "-";
  } catch (err) {
    console.error("Failed to load overview:", err);
  }
}

async function loadMatches() {
  const tbody = document.getElementById("matches-tbody");
  tbody.innerHTML = `<tr><td colspan="7" class="text-center">Loading fixtures...</td></tr>`;

  try {
    const matches = await apiFetch("/api/admin/matches");
    if (!matches || matches.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center">No fixtures found.</td></tr>`;
      return;
    }

    tbody.innerHTML = matches.map(m => `
      <tr>
        <td><code>${m.matchId}</code></td>
        <td><strong>${m.team1} vs ${m.team2}</strong></td>
        <td>${m.series || "T20 League"}</td>
        <td>${m.venue || "Stadium"}</td>
        <td><span class="badge ${m.status === 'LIVE' ? 'badge-danger' : 'badge-gold'}">${m.status}</span></td>
        <td>${m.isPublished ? '<span class="badge badge-success">Live</span>' : '<span class="badge">Draft</span>'}</td>
        <td>
          <button class="btn btn-outline btn-sm" onclick="alert('Match details for: ${m.matchId}')">Edit</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:red">Failed to load matches: ${err.message}</td></tr>`;
  }
}

async function loadContests() {
  const tbody = document.getElementById("contests-tbody");
  tbody.innerHTML = `<tr><td colspan="7" class="text-center">Loading contests...</td></tr>`;

  try {
    const contests = await apiFetch("/api/admin/contests");
    if (!contests || contests.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center">No contests found.</td></tr>`;
      return;
    }

    tbody.innerHTML = contests.map(c => `
      <tr>
        <td><strong>${c.title}</strong></td>
        <td>${c.matchId || "Global"}</td>
        <td>₹${c.entryFee}</td>
        <td><strong>₹${c.prizePool.toLocaleString("en-IN")}</strong></td>
        <td>${c.filledSpots} / ${c.totalSpots}</td>
        <td><span class="badge badge-gold">${c.status}</span></td>
        <td>
          <button class="btn btn-outline btn-sm" onclick="alert('Prize breakdown configured')">View</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:red">Failed to load contests: ${err.message}</td></tr>`;
  }
}

async function loadWithdrawals() {
  const tbody = document.getElementById("withdrawals-tbody");
  tbody.innerHTML = `<tr><td colspan="7" class="text-center">Loading withdrawal requests...</td></tr>`;

  try {
    const list = await apiFetch("/api/admin/withdrawals");
    if (!list || list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center">No withdrawal requests found.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(w => `
      <tr>
        <td><code>${w.withdrawalId}</code></td>
        <td>${w.userName || w.userId}<br><small class="text-muted">${w.userMobile || ""}</small></td>
        <td><strong style="color: #00C853; font-size: 14px;">₹${w.amount}</strong></td>
        <td><code>${w.upiId}</code></td>
        <td>${new Date(w.requestedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
        <td>
          <span class="badge ${w.status === 'COMPLETED' ? 'badge-success' : w.status === 'REJECTED' ? 'badge-danger' : 'badge-gold'}">
            ${w.status}
          </span>
        </td>
        <td>
          ${w.status === 'PENDING' ? `
            <button class="btn btn-success btn-sm" onclick="reviewWithdrawal('${w.withdrawalId}', 'COMPLETE')">Pay UPI</button>
            <button class="btn btn-danger btn-sm" onclick="reviewWithdrawal('${w.withdrawalId}', 'REJECT')">Reject</button>
          ` : `
            <span class="text-muted">Reviewed</span>
          `}
        </td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:red">Failed to load withdrawals: ${err.message}</td></tr>`;
  }
}

async function reviewWithdrawal(id, action) {
  const remarks = action === "REJECT" 
    ? prompt("Enter rejection reason:", "UPI details unverified")
    : "Processed via UPI Gateway";

  if (action === "REJECT" && !remarks) return;

  try {
    const res = await apiFetch("/api/admin/withdrawals/review", {
      method: "POST",
      body: JSON.stringify({ withdrawalId: id, action, remarks })
    });
    if (res.success) {
      alert(`Withdrawal ${id} marked as ${res.status}`);
      loadWithdrawals();
      loadOverview();
    }
  } catch (err) {
    alert("Error updating withdrawal: " + err.message);
  }
}

async function loadUsers() {
  const tbody = document.getElementById("users-tbody");
  tbody.innerHTML = `<tr><td colspan="7" class="text-center">Loading users...</td></tr>`;

  try {
    const users = await apiFetch("/api/admin/users");
    tbody.innerHTML = users.map(u => `
      <tr>
        <td><code>${u.userId}</code></td>
        <td><strong>${u.name}</strong></td>
        <td>${u.mobileNumber}</td>
        <td>${u.email || "-"}</td>
        <td>${u.selectedState || "-"}</td>
        <td><span class="badge ${u.role === 'ADMIN' ? 'badge-gold' : ''}">${u.role}</span></td>
        <td><span class="badge badge-success">${u.accountStatus}</span></td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:red">Failed: ${err.message}</td></tr>`;
  }
}

async function loadSettings() {
  try {
    const settings = await apiFetch("/api/admin/settings");
    document.getElementById("setting-min-deposit").value = settings.minDeposit ?? 10;
    document.getElementById("setting-min-withdrawal").value = settings.minWithdrawal ?? 100;
    document.getElementById("setting-max-withdrawal").value = settings.maxWithdrawalPerDay ?? 50000;
    document.getElementById("setting-maintenance").checked = !!settings.isMaintenanceMode;
    document.getElementById("setting-maintenance-msg").value = settings.maintenanceMessage || "";
  } catch (err) {
    console.error("Failed to load settings:", err);
  }
}

async function handleSaveSettings(e) {
  e.preventDefault();
  const payload = {
    minDeposit: Number(document.getElementById("setting-min-deposit").value),
    minWithdrawal: Number(document.getElementById("setting-min-withdrawal").value),
    maxWithdrawalPerDay: Number(document.getElementById("setting-max-withdrawal").value),
    isMaintenanceMode: document.getElementById("setting-maintenance").checked,
    maintenanceMessage: document.getElementById("setting-maintenance-msg").value
  };

  try {
    const res = await apiFetch("/api/admin/settings", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    if (res.success) {
      alert("Platform settings saved successfully!");
    }
  } catch (err) {
    alert("Error saving settings: " + err.message);
  }
}

async function loadDiagnostics() {
  const diagPre = document.getElementById("diagnostics-json");
  diagPre.textContent = "Querying live backend status...";

  try {
    const data = await apiFetch("/api/admin/diagnostics");
    diagPre.textContent = JSON.stringify(data, null, 2);
  } catch (err) {
    diagPre.textContent = "Diagnostics error: " + err.message;
  }
}
