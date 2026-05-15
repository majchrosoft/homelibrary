# Firebase anonymization tool

A one-off Node script that anonymizes the Home Library **dev** Firebase project.
Three modes — pick the one that fits your situation:

| Mode             | What it does                              | When to use                                |
|------------------|-------------------------------------------|--------------------------------------------|
| `export`         | Reads dev → writes sanitized JSON to disk | You only want a snapshot to share          |
| `sync`           | Copies dev → a separate "anon" project    | You can spin up a second Firebase project  |
| `in-place`       | Overwrites dev itself, record by record   | You only have one dev project (DESTRUCTIVE)|

**The script never touches your production project — it only knows about the
URL you give it as `--source-db-url`. Double-check that URL before each run.**

## Setup (one-time)

1. Firebase Console → your **dev** project → Project settings → Service
   accounts → **Generate new private key** → save as `source-sa.json` here.
2. (Sync mode only) Repeat for your separate anon project, save as `dest-sa.json`.
3. `npm install`

`source-sa.json` and `dest-sa.json` are gitignored — keep them on your laptop.

## Mode 1: Export-only (safest, recommended for sharing data)

```bash
node anonimize_dev.js \
  --source-sa ./source-sa.json \
  --source-db-url 'https://YOUR-DEV-default-rtdb.<region>.firebasedatabase.app' \
  --output ./anonymized-db.json
```

Produces `anonymized-db.json` (safe to share) and `mapping.json` (keep local —
contains the real → anon email mapping). No Firebase project is modified.

## Mode 3: In-place (when you only have one dev project)

This mode **overwrites your dev project** with anonymized data + auth records.
After running:
- Real emails are replaced with `user-N@example.test`
- All accounts share the same password (default `Test1234!`)
- Free-text fields are scrubbed; titles/authors/ISBNs are kept as-is

Always preview first (no `--confirm`):

```bash
node anonimize_dev.js \
  --source-sa ./source-sa.json \
  --source-db-url 'https://YOUR-DEV-default-rtdb.<region>.firebasedatabase.app' \
  --in-place
```

The preview prints exactly what would be written for each user, one at a time.
Inspect `mapping.json` and the log output. If it looks right:

```bash
node anonimize_dev.js \
  --source-sa ./source-sa.json \
  --source-db-url 'https://YOUR-DEV-default-rtdb.<region>.firebasedatabase.app' \
  --in-place \
  --password 'Test1234!' \
  --confirm
```

You'll then be asked to **type the source DB URL** as a final confirmation
before any writes happen. The script processes one user at a time, so if
something fails halfway through, you'll know exactly which uid failed and can
re-run safely (the operation is idempotent per-user).

### Keep specific accounts intact

Useful if you want to preserve your own admin login while anonymizing
everything else:

```bash
... --in-place --confirm \
    --exclude-uid 3 \
    --exclude-uid 6DUN7jJRjxQWW7lCRrP6Y94Rsus1
```

The excluded UIDs keep their original email, password, displayName, and DB
subtree.

## Mode 2: Sync to a separate anon project

```bash
node anonimize_dev.js \
  --source-sa ./source-sa.json \
  --dest-sa   ./dest-sa.json \
  --source-db-url 'https://DEV-default-rtdb.<region>.firebasedatabase.app' \
  --dest-db-url   'https://ANON-default-rtdb.<region>.firebasedatabase.app' \
  --password 'Test1234!'
```

The dest project is created/updated with anonymized auth + DB. The source dev
project is untouched. Use `--dry-run` to preview without writing.

## What gets anonymized

| Source                                  | After                                          |
|-----------------------------------------|------------------------------------------------|
| Auth user `majchrosoft@gmail.com`       | `user-1@example.test`, password = `Test1234!`  |
| `displayName: "Pawel"`                  | `displayName: "User user-1"`                   |
| `description: "Borrowed from Mom"`      | `description: "[redacted N chars]"`            |
| `users/3/bookcases/9/name: "Office"`    | `users/3/bookcases/9/name: "Bookcase 9"`       |
| `title`, `author`, `isbn`, `type`, `quality` | (kept — public-domain bibliographic facts) |
| Anything matching `token`/`password`/`phone` etc. | Dropped entirely                       |

UIDs are preserved — RTDB foreign-key references between user nodes and auth
records remain consistent.

## Sharing the anonymized result with Claude

After running:
- Share `anonymized-db.json` (export mode) or the anon project URL (sync mode),
  or just paste a few sample records.
- **Do not share** `mapping.json` or any `*-sa.json` file.
- For in-place mode, sharing the dev DB URL + a sample exported snapshot is
  enough — no service-account credentials needed.
