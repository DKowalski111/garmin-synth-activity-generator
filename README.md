# Synthetic FIT Generator

A local developer tool for generating synthetic Garmin-compatible `.fit` cycling activity files from drawn routes.

## Purpose

This tool is intended only for generating synthetic test data for testing sports applications.

## Architecture Overview

```
Frontend (React/Vite :5173)
    │  POST /api/routes              → route geometry from Valhalla
    │  POST /api/activities/preview  → synthetic samples (JSON)
    │  POST /api/activities/fit      → binary .fit file
    ▼
Backend (Spring Boot :8080)
    ├── routing         ← Valhalla routing API / RoutingProvider interface
    ├── activity        ← time calculation, speed + HR + calorie generation, pauses
    ├── fit/encoding    ← Garmin FIT SDK BufferEncoder
    └── fit/validation  ← re-decode + integrity check
```

The activity generation pipeline is fully independent of routing:

```
Route geometry
  → Distance-indexed polyline
  → Deterministic speed profile (seeded RNG, realistic envelope)
  → Timestamped distance samples
  → Interpolated GPS coordinates (Haversine)
  → Synthetic heart-rate profile (cardiac lag model)
  → MET-based calorie estimate
  → FIT serialization (Garmin SDK)
  → FIT validation (re-decode)
```

## Requirements

| Component | Version |
|-----------|---------|
| Java      | 17+     |
| Maven     | 3.9+    |
| Node.js   | 18+     |
| npm       | 9+      |

## No API Keys Required

The map uses **OpenStreetMap** tiles (free, no account needed) via Leaflet.

Routing uses the **Valhalla** public instance at `valhalla1.openstreetmap.de` (free, no account needed). It uses the bicycle costing model with a strong preference for dedicated cycling infrastructure — cycle lanes, paths, and shared-use tracks over roads.

## Local Startup

### Backend

```bash
cd backend
mvn spring-boot:run
```

Backend listens on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server at `http://localhost:5173`. API calls are proxied to `:8080`.

## Test Commands

```bash
# Backend (20 tests)
cd backend && mvn test

# Frontend (9 tests)
cd frontend && npm test

# Both from root
make test
```

## Example API Request

### Preview activity

```bash
curl -s -X POST http://localhost:8080/api/activities/preview \
  -H 'Content-Type: application/json' \
  -d '{
    "activityName": "Synthetic cycling test",
    "sport": "CYCLING",
    "route": {
      "distanceMeters": 32140.5,
      "points": [
        {"latitude": 50.2945, "longitude": 18.6712},
        {"latitude": 50.2750, "longitude": 18.7500},
        {"latitude": 50.2581, "longitude": 18.9974}
      ]
    },
    "timeConfiguration": {"mode": "END_NOW"},
    "averageSpeedKmh": 25.0,
    "averageHeartRate": 145,
    "recordingIntervalSeconds": 1,
    "seed": 12345,
    "pauses": []
  }' | jq .summary
```

### Download FIT file

```bash
curl -s -X POST http://localhost:8080/api/activities/fit \
  -H 'Content-Type: application/json' \
  -d '{ ... same body ... }' \
  -o activity.fit
```

## End Now Timestamp Behavior

When `timeConfiguration.mode = "END_NOW"`, the server captures `clock.instant()` at the **very beginning** of the `/api/activities/fit` request handler — before any generation work begins. Start time is calculated backwards:

```
start = end - (route_distance / average_speed) - sum(pause_durations)
```

## Moving Time vs Elapsed Time

- **`total_timer_time`** — actual movement time (pauses excluded)
- **`total_elapsed_time`** — wall-clock duration (pauses included)

For an activity with no pauses, these are equal. With pauses:

```
total_elapsed_time = total_timer_time + Σ(pause durations)
```

The configured `averageSpeedKmh` is the **moving** average — it excludes paused time.

## FIT Validation

After encoding, the backend re-decodes every generated file and checks:

1. File is non-empty and ≥14 bytes
2. `Decode.checkFileIntegrity()` returns true (valid CRC)
3. Re-decode succeeds without exceptions
4. Required messages present: Record, Session, Activity, Lap
5. Record timestamps are monotonically increasing
6. Accumulated distance never decreases

If any check fails, the endpoint returns HTTP 500 with a structured error. No corrupt file is returned.

## Known Limitations

- Only **cycling** is implemented. Running/walking can be added via the `RoutingProvider` and sport-type selection.
- Altitude data is not generated (no elevation source).
- Cadence and power are not generated.
- Calorie estimate assumes a body weight of 75 kg using MET values from the Compendium of Physical Activities.

## Troubleshooting

**Port already in use** — backend defaults to `:8080`, frontend to `:5173`. Change with `server.port` in `application.yml` or `--port` in Vite.

**`BUILD FAILURE` with corporate Gradle** — this project uses Maven. Use `mvn`, not `./gradlew`.

**Valhalla returns no route** — the public instance has rate limits and occasional downtime. Wait a moment and try again, or self-host Valhalla with OpenStreetMap data.
