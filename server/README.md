# Rbx.m Cloud Backend

This backend is intended for Rbx.m configuration, device state, logs, and server-side jobs that do not require a Roblox client UI.

## Important

A cloud VM/API cannot turn into a remote Roblox Android/Windows client simply by installing this project. Roblox client automation must comply with Roblox and the target experience's rules.

If the target experience is your own Roblox game, move persistent game logic into Roblox server-side scripts. Rbx.m can use this backend for configuration and device state while the phone is offline.

## API

- `GET /health` — health check
- `GET /api/config` — current public macro configuration
- `PUT /api/config` — replace configuration (demo/local deployment; add authentication before exposing publicly)
- `POST /api/events` — append a device event
- `GET /api/events` — inspect recent events

The sample implementation uses in-memory storage intentionally. For production, replace it with a persistent database or object storage.

## Google Cloud

This project can be containerized and deployed to Cloud Run. Cloud Run is suitable for the API/config/log portion, but it is **not** a replacement for a continuously running Roblox client.
