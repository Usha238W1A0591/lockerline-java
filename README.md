# LockerLine

A bank of 48 parcel lockers in an apartment block: couriers drop, residents collect with a code that expires.

## Run it in a container

The image builds the application in a JDK stage and runs it on a JRE stage. You need Docker; you do not need to install Java or Maven on the machine running the container.

Build the image:

```bash
docker build -t lockerline .
```

Run it:

```bash
docker run -p 8080:8080 lockerline
```

The application is available at `http://localhost:8080`.

This first run is ephemeral: the database lives inside the container. When the container is removed, the database and its parcels are lost.

## Keep the parcels, and configure it from one file

To keep the database when the container is replaced, use a named Docker volume. The `.env.example` file contains the settings you can change.

Copy it to `.env`:

```bash
cp .env.example .env
```

Edit `.env`, especially `CODE_HASH_KEY`, and set your own long, random value.

Run the container with the volume and environment file:

```bash
docker run -d --name lockerline -p 8080:8080 -v lockerline-data:/data --env-file .env lockerline
```

The named volume `lockerline-data` keeps the database outside the container, so removing or replacing the container does not remove the parcels.

### Settings

| Variable | What it sets | Default |
|---|---|---|
| `DATABASE_URL` | The JDBC URL of the H2 file | `jdbc:h2:file:./data/lockerline` (`jdbc:h2:file:/data/lockerline` in `.env.example`) |
| `FREE_WINDOW_HOURS` | How long a pickup code stays valid after the drop | `48` |
| `CODE_HASH_KEY` | The key pickup codes are hashed with | `dev-only-change-me` - set your own |
| `HOST` | The address the server listens on | `127.0.0.1` (`0.0.0.0` in the image) |
| `PORT` | The port | `8080` |

To change the free window: edit the line in `.env`, then `docker rm -f lockerline` and run the same `docker run` again. Nothing is rebuilt, and the parcels are still there, because they live on the volume.

Logs go to stdout: `docker logs lockerline` shows one line per request and one per event - never a pickup code.

### How pickup codes are stored

A pickup code is never stored in the database in plain text. It is stored as a keyed hash:

as HMAC-SHA-256 with `CODE_HASH_KEY`, which lives in the environment and never in the database: without the key the
stored hashes are useless. Because the hash is keyed but has no per-row salt, a presented code is found with one
equality lookup (bcrypt would mean checking the code against every waiting parcel), and the final comparison is
constant-time.

## One parcel, from drop to collection

With the container running on port 8080:

### Is it up? What's free?

```bash
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}
```

```bash
curl -s "http://localhost:8080/towers/A/doors?size=M&state=free"
# {"tower":"A","free":20,"doors":[{"no":"D21","size":"M","state":"free"}, ...]}
```

### A courier drops a parcel

```bash
curl -s -X POST http://localhost:8080/drops -H 'content-type: application/json' -d '{"sizeClass":"M","flat":"302"}'
# {"parcelId":"P001","door":"D21","sizeClass":"M","flat":"302","droppedAt":"...","code":"482913","expiresAt":"..."}
```

"..." stands for a time that's different on every run; the code is random too. A reader uses their own code in the next call.

### The resident collects; the history

```bash
curl -s -X POST http://localhost:8080/pickups -H 'content-type: application/json' -d '{"code":"482913"}'
# {"parcelId":"P001","door":"D21","openedAt":"...","dwellSeconds":95}
```

```bash
curl -s http://localhost:8080/parcels/P001
# {"parcelId":"P001","state":"collected", ... "events":[dropped, code_issued, opened]}
```

That last answer is your event log, read back: one parcel, three events, in order.

Use the code your own drop returned. The API docs are at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

<!-- learntube:start -->
---

[![tests](https://github.com/Usha238W1A0591/lockerline-java/actions/workflows/tests.yml/badge.svg)](https://github.com/Usha238W1A0591/lockerline-java/actions/workflows/tests.yml)

### Progress

- [x] Ticket 1 · Forty-eight doors, and a wall you can look at
- [x] Ticket 2 · A courier asks for a door
- [x] Ticket 3 · The code that dies at midnight
- [x] Ticket 4 · Three ways to say no
- [x] Ticket 5 · The door opens, and the clock stops
- [x] Ticket 6 · A notebook nobody can rewrite
- [x] Ticket 7 · One file becomes an app
- [x] Ticket 8 · The rule table, and then the rule changes
- [x] Ticket 9 · It runs on a machine that isn't yours
- [x] Ticket 10 · It survives being switched off

Built by Kowluri Usha Kiranmai on [LearnTube Guided Projects](https://guidedprojects.learntube.ai).
<!-- learntube:end -->
