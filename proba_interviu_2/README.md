# Habit quests

A Fastify API and a Next.js page that rebuild the retired habit tracker. One person, no login. Quests award XP, levels grow, and a streak survives only if the calendar days stay consecutive.

## Run with Docker

From this directory, with Docker installed:

```sh
docker compose up --build
```

The API listens on port 3001 and the page on port 3000. Quest data is stored in a Docker volume, so it is still there after a restart.

## Run without Docker

Requires Node.js 24. One command:

```sh
# macOS or Linux
./scripts/start.sh
```

```powershell
# Windows
powershell -File scripts/start.ps1
```

The script installs dependencies if they are missing, starts the API on port 3001, and the page on port 3000. The database file is `data/habit.db`.

## Tests

```sh
npm --prefix api test
```

`test/replay.test.ts` sends every request in `reference_recording.jsonl` to the API and compares the response. The old ids (`q1`, `c1`) are mapped to the ids this API generates.

## Notes for review

See `NOTES.md` for the rules found in the recording, the choices the sources leave open, and what was left out.
