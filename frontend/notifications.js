const { createApp, ref, onMounted, onBeforeUnmount } = Vue;

createApp({
  setup() {
    const token = ref("");
    const service = ref("release-service");
    const message = ref("down");
    const status = ref("Ready.");
    const lastError = ref(null);
    const alerts = ref([]);
    const streamActive = ref(false);
    let eventSource = null;

    const role = () => document.body.dataset.role || "release-manager";

    const ensureToken = async () => {
      if (token.value) {
        return token.value;
      }
      const response = await fetch("http://localhost:8084/api/notifications/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: role() }),
      });
      if (!response.ok) {
        throw new Error(`Token request failed: ${response.status}`);
      }
      const data = await response.json();
      token.value = data.token || "";
      return token.value;
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
        status.value = "SystemErrorEvent sent.";
        await loadLastError();
        await loadAlerts();
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
        alerts.value = await response.json();
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
          alerts.value = [payload, ...alerts.value];
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
      startStream();
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
      <h2>Notifications (Vue)</h2>
      <div class="stack">
        <label>Service</label>
        <input v-model="service" />
        <label>Message</label>
        <input v-model="message" />
        <button @click="submitNotification">Send system error event</button>
        <div class="split">
          <button class="secondary" @click="loadLastError">Refresh latest alert</button>
          <button class="secondary" @click="loadAlerts">Load all alerts</button>
        </div>
        <div class="split">
          <button class="secondary" @click="startStream" :disabled="streamActive">Start live stream</button>
          <button class="secondary" @click="stopStream" :disabled="!streamActive">Stop live stream</button>
        </div>
        <div class="status" v-if="lastError">
          Latest alert: {{ lastError.service }} - {{ lastError.message }}
        </div>
        <div class="status" v-else>No alerts received yet.</div>
        <div class="panel-title">All alerts</div>
        <div class="token-list" v-if="alerts.length">
          <div class="token-item" v-for="(alert, idx) in alerts" :key="idx">
            <strong>{{ alert.service }}</strong> — {{ alert.message }}
          </div>
        </div>
        <div class="helper" v-else>No alerts in history.</div>
        <div class="status">{{ status }}</div>
      </div>
    </div>
  `,
}).mount("#vue-root");
