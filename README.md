# Synthetic FIT Generator

A local developer tool for generating synthetic Garmin-compatible `.fit` cycling activity files from user-drawn Google Maps routes.

## Purpose

This tool is intended **only for generating synthetic test data**. It does not impersonate a real Garmin device. The generated files use `manufacturer = DEVELOPMENT` and a clearly synthetic product identifier.

## Architecture Overview

```
Frontend (React/Vite :5173)
    │  POST /api/routes          → route geometry + polyline
    │  POST /api/activities/preview → synthetic samples (JSON)
    │  POST /api/activities/fit  → binary .fit file
    ▼
Backend (Spring Boot :8080)
    ├── routing         ← Google Directions API / RoutingProvider interface
    ├── activity        ← time calculation, speed + HR generation, pauses
    ├── fit/encoding    ← Garmin FIT SDK BufferEncoder
    └── fit/validation  ← re-decode + integrity check
```

The activity generation pipeline is fully independent of HTTP and Google Maps:

```
Route geometry
  → Distance-indexed polyline
  → Deterministic speed profile (seeded RNG, realistic envelope)
  → Timestamped distance samples
  → Interpolated GPS coordinates (Haversine)
  → Synthetic heart-rate profile (cardiac lag model)
  → FIT serialization (Garmin SDK)
  → FIT validation (re-decode)
```

## Requirements

| Component | Version  |
|-----------|----------|
| Java      | 17+      |
| Maven     | 3.9+     |
| Node.js   | 18+      |
| npm       | 9+       |

## Google API Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project and enable **Directions API** and **Maps JavaScript API**
3. Create an API key
4. Restrict the key to your local origin (`http://localhost:5173`) for the browser key

## Environment Variables

**Frontend** (`frontend/.env`):
```
VITE_GOOGLE_MAPS_API_KEY=your_key_here
```

**Backend** (`backend/.env` — loaded by your shell or IDE):
```
GOOGLE_MAPS_API_KEY=your_key_here
```

The backend uses `GOOGLE_MAPS_API_KEY` only for the `/api/routes` endpoint. All other endpoints work without it.

## Local Startup

### Backend

```bash
cd backend
mvn spring-boot:run
# OR with API key:
GOOGLE_MAPS_API_KEY=your_key mvn spring-boot:run
```

Backend listens on `http://localhost:8080`.

### Frontend

```bash
cd frontend
cp .env.example .env
# Edit .env and add your VITE_GOOGLE_MAPS_API_KEY
npm install
npm run dev
```

Frontend dev server at `http://localhost:5173`. API calls are proxied to `:8080`.

## Test Commands

```bash
# Backend tests (20 tests)
cd backend && mvn test

# Frontend tests (8 tests)
cd frontend && npm test

# All tests from root
make test
```

## Example API Request

### Preview activity (no Google key needed — pass your own route points)

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

When `timeConfiguration.mode = "END_NOW"`, the server captures `clock.instant()` at the **very beginning** of the `/api/activities/fit` request handler — before any generation work begins. The activity end time is set to this instant. Start time is calculated backwards:

```
start = end - (route_distance / average_speed) - sum(pause_durations)
```

This means the activity end time equals the moment the user clicked "Download", not when the file finished generating.

## Moving Time vs Elapsed Time

- **`total_timer_time`** — actual movement time (pauses excluded)
- **`total_elapsed_time`** — wall-clock duration (pauses included)

For an activity with no pauses, these are equal. With pauses:

```
total_elapsed_time = total_timer_time + Σ(pause durations)
```

The configured `averageSpeedKmh` is the **moving** average — it excludes paused time.

## FIT Validation

After encoding, the backend re-decodes every generated file using the Garmin SDK and checks:

1. File is non-empty and ≥14 bytes
2. `Decode.checkFileIntegrity()` returns true (valid CRC)
3. Re-decode succeeds without exceptions
4. Required messages are present: Record, Session, Activity, Lap
5. Record timestamps are monotonically increasing
6. Accumulated distance never decreases

If any check fails, the endpoint returns HTTP 500 with an `ErrorResponse` containing the specific issues. No corrupt file is returned.

## Known Limitations

- Only **cycling** sport is implemented. Running/walking support requires adding sport type selection to the UI and backend.
- Altitude data is not generated (no elevation source). The altitude fields in FIT records are omitted.
- Cadence and power fields are present in the domain model but not generated or written.
- The Google Maps integration uses the Directions API (bicycling mode). For regions with poor cycling route data, the route may fall back to driving roads.
- The frontend map requires a valid Google Maps API key. Without one, the map shows a placeholder message but the backend API still works.

## Troubleshooting

**`GOOGLE_MAPS_API_KEY is not configured`** — set the env variable before starting the backend, or use the `/api/activities/*` endpoints directly with your own route points.

**`This page can't load Google Maps correctly`** — the `VITE_GOOGLE_MAPS_API_KEY` is missing, invalid, or the key doesn't have Maps JavaScript API enabled.

**Port already in use** — backend defaults to `:8080`, frontend to `:5173`. Change with `server.port` in `application.yml` or `--port` in Vite.

**`BUILD FAILURE` with corporate Gradle** — this project uses Maven to avoid corporate Gradle init scripts. Use `mvn` not `./gradlew`.

**FIT file not recognized by Garmin Connect** — this is expected for synthetic files using `manufacturer = DEVELOPMENT`. The files are valid FIT format and can be opened with tools like FIT CSV Tool or decoded with the SDK.
