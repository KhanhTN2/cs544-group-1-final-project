# Frontend Demo (React + Vue)

This static page provides a lightweight UI for the demo flow using React and Vue via CDN.

## Run (localhost:3000)

1. Start the backend services with Docker Compose as described in the repo root README.
2. Start the static server:
   ```bash
   node server.js
   ```
3. Open `http://localhost:3000` in your browser.
4. Use the React cards to request tokens and create releases, discussion messages, and AI chat requests.
5. Use the Vue card to send a SystemErrorEvent to the notification service.

## Run with Docker Compose

The repo `docker-compose.yml` now includes a `frontend` service bound to `http://localhost:3000`.
Grafana is available at `http://localhost:3001` to avoid port conflicts.
