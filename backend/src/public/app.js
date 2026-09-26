(function () {
  "use strict";

  const TOKEN_STORAGE_KEY = "cafeshop_admin_token";
  const POLL_INTERVAL_MS = 5000;

  const loginPanel = document.getElementById("login-panel");
  const dashboardPanel = document.getElementById("dashboard-panel");
  const tokenInput = document.getElementById("token-input");
  const loginBtn = document.getElementById("login-btn");
  const logoutBtn = document.getElementById("logout-btn");
  const loginError = document.getElementById("login-error");
  const ordersBody = document.getElementById("orders-body");
  const ordersEmpty = document.getElementById("orders-empty");
  const ordersError = document.getElementById("orders-error");
  const statusIndicator = document.getElementById("status-indicator");

  let pollHandle = null;

  function getStoredToken() {
    try {
      return sessionStorage.getItem(TOKEN_STORAGE_KEY);
    } catch {
      return null;
    }
  }

  function storeToken(token) {
    try {
      sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
    } catch {
      // sessionStorage may be unavailable (private browsing); the session
      // simply won't survive a page refresh in that case.
    }
  }

  function clearStoredToken() {
    try {
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    } catch {
      // Nothing to clean up if storage was never available.
    }
  }

  function formatCurrency(value) {
    const number = typeof value === "number" ? value : Number(value ?? 0);
    return `$${number.toFixed(2)}`;
  }

  function summarizeItems(items) {
    if (!Array.isArray(items) || items.length === 0) {
      return "-";
    }
    return items
      .map((item) => (item && typeof item === "object" ? item.name : undefined))
      .filter(Boolean)
      .join(", ");
  }

  function renderOrders(orders) {
    ordersBody.innerHTML = "";
    ordersError.hidden = true;

    if (!orders || orders.length === 0) {
      ordersEmpty.hidden = false;
      return;
    }
    ordersEmpty.hidden = true;

    for (const order of orders) {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${order.id}</td>
        <td>${order.customerName ?? "-"}</td>
        <td>${order.status ?? "-"}</td>
        <td>${order.paymentStatus ?? "-"}</td>
        <td>${summarizeItems(order.items)}</td>
        <td>${formatCurrency(order.totalPrice)}</td>
      `;
      ordersBody.appendChild(row);
    }
  }

  async function fetchOrders(token) {
    statusIndicator.textContent = "refreshing…";
    try {
      const res = await fetch("/api/orders", {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.status === 401 || res.status === 403) {
        stopPolling();
        showLogin(res.status === 403 ? "Signed-in account is not an admin." : "Session expired. Please sign in again.");
        return;
      }

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.error?.message ?? `Request failed with status ${res.status}`);
      }

      const body = await res.json();
      renderOrders(body.orders);
      statusIndicator.textContent = `updated ${new Date().toLocaleTimeString()}`;
    } catch (err) {
      ordersError.hidden = false;
      ordersError.textContent = err instanceof Error ? err.message : "Failed to load orders.";
      statusIndicator.textContent = "error";
    }
  }

  function startPolling(token) {
    stopPolling();
    fetchOrders(token);
    pollHandle = setInterval(() => fetchOrders(token), POLL_INTERVAL_MS);
  }

  function stopPolling() {
    if (pollHandle) {
      clearInterval(pollHandle);
      pollHandle = null;
    }
  }

  function showDashboard(token) {
    loginPanel.hidden = true;
    dashboardPanel.hidden = false;
    startPolling(token);
  }

  function showLogin(errorMessage) {
    dashboardPanel.hidden = true;
    loginPanel.hidden = false;
    if (errorMessage) {
      loginError.hidden = false;
      loginError.textContent = errorMessage;
    } else {
      loginError.hidden = true;
    }
  }

  loginBtn.addEventListener("click", () => {
    const token = tokenInput.value.trim();
    if (!token) {
      loginError.hidden = false;
      loginError.textContent = "Please paste a Firebase ID token first.";
      return;
    }
    storeToken(token);
    showDashboard(token);
  });

  logoutBtn.addEventListener("click", () => {
    stopPolling();
    clearStoredToken();
    tokenInput.value = "";
    showLogin();
  });

  const existingToken = getStoredToken();
  if (existingToken) {
    showDashboard(existingToken);
  } else {
    showLogin();
  }
})();
