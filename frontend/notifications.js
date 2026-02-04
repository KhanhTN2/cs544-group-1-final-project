const { createApp, ref, onMounted, onBeforeUnmount } = Vue;

createApp({
  setup() {
    const token = ref("");
    const service = ref("release-service");
    const message = ref("down");
    const status = ref("Ready.");
    const lastError = ref(null);
    const alerts = ref([]);
    const seen = new Set();
    const streamActive = ref(false);
    let eventSource = null;

    const userRole = () => document.body.dataset.role || "DEVELOPER";
    const userId = () => document.body.dataset.userId || "dev-1";

    const ensureToken = async () => {
      if (token.value) {
        return token.value;
      }
      const loginResponse = await fetch("http://localhost:8086/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: userId(), password: "secret" }),
      });

      if (loginResponse.ok) {
        const data = await loginResponse.json();
        token.value = data.token || "";
        return token.value;
      }

      if (loginResponse.status !== 400) {
        throw new Error(`Auth login failed: ${loginResponse.status}`);
      }

      const registerResponse = await fetch("http://localhost:8086/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: userId(), password: "secret", role: userRole() }),
      });

      if (!registerResponse.ok) {
        throw new Error(`Auth register failed: ${registerResponse.status}`);
      }

      const retryLogin = await fetch("http://localhost:8086/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: userId(), password: "secret" }),
      });
      if (!retryLogin.ok) {
        throw new Error(`Auth login failed: ${retryLogin.status}`);
      }
      const data = await retryLogin.json();
      token.value = data.token || "";
      return token.value;
    };

    const makeKey = (payload) => (payload && payload.id ? String(payload.id) : "");

    const addAlert = (payload) => {
      const key = makeKey(payload);
      if (!key) {
        return;
      }
      if (seen.has(key)) {
        return;
      }
      seen.add(key);
      alerts.value = [payload, ...alerts.value].slice(0, 20);
    };

    const submitNotification = async () => {
      status.value = "Sending system error event...";
      try {
        const jwt = await ensureToken();
        const response = await fetch("http://localhost:8084/api/notifications/system-error", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${jwt}`,
          },
          body: JSON.stringify({ service: service.value, message: message.value }),
        });

        if (!response.ok) {
          throw new Error(`Notification failed: ${response.status}`);
        }
        const payload = await response.json();
        addAlert(payload);
        lastError.value = payload;
        status.value = "System error sent.";
      } catch (error) {
        status.value = error.message;
      }
    };

    const loadLastError = async () => {
      status.value = "Loading latest system alert...";
      try {
        const jwt = await ensureToken();
        const response = await fetch("http://localhost:8084/api/notifications/last", {
          headers: { Authorization: `Bearer ${jwt}` },
        });
        if (response.status === 204) {
          lastError.value = null;
          status.value = "No system alerts yet.";
          return;
        }
        if (!response.ok) {
          throw new Error(`Alert fetch failed: ${response.status}`);
        }
        lastError.value = await response.json();
        addAlert(lastError.value);
        status.value = "Latest system alert loaded.";
      } catch (error) {
        status.value = error.message;
      }
    };

    const loadAlerts = async () => {
      status.value = "Loading alert history...";
      try {
        const jwt = await ensureToken();
        const response = await fetch("http://localhost:8084/api/notifications", {
          headers: { Authorization: `Bearer ${jwt}` },
        });
        if (!response.ok) {
          throw new Error(`Alert list failed: ${response.status}`);
        }
        const list = await response.json();
        list.forEach((item) => addAlert(item));
        status.value = "Alert history loaded.";
      } catch (error) {
        status.value = error.message;
      }
    };

    const startStream = async () => {
      if (eventSource) {
        return;
      }
      streamActive.value = true;
      const jwt = await ensureToken();
      eventSource = new EventSource(
        `http://localhost:8084/api/notifications/stream?token=${encodeURIComponent(jwt)}`
      );
      eventSource.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data);
          lastError.value = payload;
          addAlert(payload);
        } catch (error) {
          // Ignore malformed events
        }
      };
      eventSource.onerror = () => {
        streamActive.value = false;
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
      };
    };

    const stopStream = () => {
      streamActive.value = false;
      if (eventSource) {
        eventSource.close();
        eventSource = null;
      }
    };

    onMounted(() => {
      loadLastError();
      loadAlerts();
    });

    onBeforeUnmount(() => {
      stopStream();
    });

    return {
      service,
      message,
      status,
      submitNotification,
      loadLastError,
      lastError,
      alerts,
      loadAlerts,
      streamActive,
      startStream,
      stopStream,
    };
  },
  template: `
    <div class="card">
      <h2>Notifications</h2>
      <div class="stack">
        <div class="panel-title">Send system alert</div>
        <div class="split">
          <div>
            <label>Service</label>
            <input v-model="service" />
          </div>
          <div>
            <label>Message</label>
            <input v-model="message" />
          </div>
        </div>
        <button @click="submitNotification">Send system error event</button>

        <div class="divider"></div>
        <div class="panel-title">Live feed</div>
        <div class="split">
          <button class="secondary" @click="startStream" :disabled="streamActive">Start live stream</button>
          <button class="secondary" @click="stopStream" :disabled="!streamActive">Stop live stream</button>
        </div>

        <div class="divider"></div>
        <div class="panel-title">Latest alert</div>
        <div class="status" v-if="lastError">
          <strong>{{ lastError.service }}</strong> — {{ lastError.message }}
        </div>
        <div class="status" v-else>No alerts received yet.</div>

        <div class="panel-title">Alert history</div>
        <div class="split">
          <button class="secondary" @click="loadLastError">Refresh latest</button>
          <button class="secondary" @click="loadAlerts">Reload history</button>
        </div>
        <div class="token-list" v-if="alerts.length">
          <div class="token-item" v-for="(alert, idx) in alerts" :key="idx">
            <strong>{{ alert.service }}</strong>
            <span>—</span>
            <span>{{ alert.message }}</span>
          </div>
        </div>
        <div class="helper" v-else>No alerts in history.</div>
        <div class="status">{{ status }}</div>
      </div>
    </div>
  `,
}).mount("#vue-root");
