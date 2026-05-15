#!/usr/bin/env node
/* eslint-disable no-console */

/**
 * anonimize_dev.js
 *
 * Anonymizes a Home Library development Firebase project IN PLACE:
 *   - Every /users/{uid} subtree has free-text PII replaced with placeholders
 *     (titles/authors/ISBNs are kept — they're public-domain bibliographic data).
 *   - Every Auth account is updated to user-N@example.test + a default password.
 *   - Any /users/{uid} node that has NO matching Auth account gets one created
 *     with the same anonymized email & default password.
 *
 * Usage:
 *   npm install
 *   node anonimize_dev.js \
 *     --source-sa ./source-sa.json \
 *     --source-db-url 'https://YOUR-DEV-default-rtdb.<region>.firebasedatabase.app' \
 *     [--password 'Test1234!'] \
 *     [--exclude-uid 3] \
 *     [--dry-run]
 *
 * Flags:
 *   --password X        Override default password ('Test1234!').
 *   --exclude-uid X     Skip uid X entirely (repeatable). Useful for keeping
 *                       your own admin login intact.
 *   --dry-run           Log everything but write nothing.
 *   --keep-emails       Keep original emails (debugging only — defeats anonymization).
 *
 * Service-account JSON: Firebase Console → Project settings → Service accounts →
 * Generate new private key. All *.json in this folder is gitignored.
 */

'use strict';

const fs = require('node:fs');
const path = require('node:path');
const admin = require('firebase-admin');

// ────────────────────────────────────────────────────────────────────────────
// CLI parsing — supports repeated flags (e.g. --exclude-uid X --exclude-uid Y).
// ────────────────────────────────────────────────────────────────────────────
// Flags that take string values (a value MUST follow). If the next token is
// another `--flag`, we error rather than silently treating the flag as
// boolean — that's the bug class that produced the EISDIR confusion.
const STRING_FLAGS = new Set([
    'source-sa', 'source-db-url', 'password', 'mapping', 'exclude-uid',
]);
const BOOLEAN_FLAGS = new Set(['dry-run', 'keep-emails']);

function parseArgs(argv) {
    const out = { _multi: {} };
    const multiKeys = new Set(['exclude-uid']);
    for (let i = 0; i < argv.length; i++) {
        const a = argv[i];
        if (!a.startsWith('--')) continue;
        const key = a.slice(2);
        const next = argv[i + 1];
        let value;
        if (BOOLEAN_FLAGS.has(key)) {
            value = true;
        } else if (STRING_FLAGS.has(key)) {
            if (next === undefined || next.startsWith('--')) {
                console.error(`ERROR: --${key} requires a value, got: ${next ?? '(end of args)'}`);
                process.exit(2);
            }
            value = next;
            i++;
        } else {
            // Unknown flag — accept silently for forward-compatibility.
            value = next === undefined || next.startsWith('--') ? true : (i++, next);
        }
        if (multiKeys.has(key)) {
            (out._multi[key] = out._multi[key] || []).push(value);
        } else {
            out[key] = value;
        }
    }
    return out;
}

const args = parseArgs(process.argv.slice(2));

for (const r of ['source-sa', 'source-db-url']) {
    if (!args[r]) {
        console.error(`Missing required arg: --${r}`);
        process.exit(2);
    }
}

const SOURCE_SA = path.resolve(args['source-sa']);
const SOURCE_DB_URL = args['source-db-url'];

if (!SOURCE_DB_URL.startsWith('https://')) {
    console.error('');
    console.error(`ERROR: --source-db-url doesn't look like a Firebase Realtime DB URL:`);
    console.error(`  ${SOURCE_DB_URL}`);
    console.error('');
    console.error('Expected format:');
    console.error('  https://<project>-default-rtdb.firebaseio.com');
    console.error('    or');
    console.error('  https://<project>-default-rtdb.<region>.firebasedatabase.app');
    console.error('');
    console.error('Find it in Firebase Console -> Realtime Database (top of the data view).');
    process.exit(2);
}
if (!/firebaseio\.com|firebasedatabase\.app/.test(SOURCE_DB_URL)) {
    console.error('');
    console.error(`ERROR: --source-db-url doesn't look like a Firebase RTDB host:`);
    console.error(`  ${SOURCE_DB_URL}`);
    console.error('Expected the host to end in firebaseio.com or firebasedatabase.app.');
    process.exit(2);
}
const DEFAULT_PASSWORD = args['password'] || 'Test1234!';
const DRY_RUN = !!args['dry-run'];
const KEEP_EMAILS = !!args['keep-emails'];
const EXCLUDE_UIDS = new Set(args._multi['exclude-uid'] || []);
const OUTPUT_MAPPING = path.resolve(args['mapping'] || './mapping.json');

// ────────────────────────────────────────────────────────────────────────────
// Firebase Admin init (single project — in-place writes go back to source)
// ────────────────────────────────────────────────────────────────────────────
function loadServiceAccount(absPath) {
    if (!fs.existsSync(absPath)) {
        console.error('');
        console.error('ERROR: Service account file not found at:');
        console.error(`  ${absPath}`);
        console.error('');
        console.error('How to get it:');
        console.error('  1. Open https://console.firebase.google.com/');
        console.error('  2. Select your DEV project');
        console.error('  3. Gear icon -> Project settings -> Service accounts tab');
        console.error('  4. Click "Generate new private key" -> save the JSON file');
        console.error('  5. Move it into tools/anonymize/source-sa.json');
        console.error('');
        console.error('The file is gitignored, so it will not be committed.');
        process.exit(2);
    }
    const stat = fs.statSync(absPath);
    if (!stat.isFile()) {
        console.error('');
        console.error(`ERROR: --source-sa points to a directory, not a file:`);
        console.error(`  ${absPath}`);
        console.error('');
        console.error('Pass the JSON file directly, e.g.:');
        console.error('  --source-sa ./source-sa.json');
        process.exit(2);
    }
    try {
        return JSON.parse(fs.readFileSync(absPath, 'utf8'));
    } catch (err) {
        console.error(`ERROR: ${absPath} is not valid JSON: ${err.message}`);
        process.exit(2);
    }
}

const serviceAccount = loadServiceAccount(SOURCE_SA);

// Sanity-check the JSON looks like a Firebase service account.
for (const required of ['project_id', 'private_key', 'client_email']) {
    if (!serviceAccount[required]) {
        console.error(`ERROR: ${SOURCE_SA} doesn't look like a Firebase service-account JSON`);
        console.error(`       (missing field: ${required}). Re-download from the Firebase Console.`);
        process.exit(2);
    }
}

const app = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: SOURCE_DB_URL,
});
const db = app.database();
const auth = app.auth();

// ────────────────────────────────────────────────────────────────────────────
// Anonymization rules
// ────────────────────────────────────────────────────────────────────────────

const DROP_KEYS = new Set([
    'token', 'fcmToken', 'fcm_token',
    'sessionId', 'session_id',
    'password', 'passwordHash', 'salt',
    'apiKey', 'phone', 'phoneNumber', 'address',
]);

const SCRUB_TEXT_KEYS = new Set([
    'description', 'notes', 'note', 'comment', 'comments',
    'displayName', 'displayname',
]);

function anonymizeNode(node, context = {}) {
    if (node === null || node === undefined) return node;
    if (typeof node !== 'object') return node;
    if (Array.isArray(node)) return node.map((item) => anonymizeNode(item, context));

    const out = {};
    for (const [key, value] of Object.entries(node)) {
        if (DROP_KEYS.has(key)) continue;

        if (SCRUB_TEXT_KEYS.has(key) && typeof value === 'string') {
            out[key] = value.length > 0 ? `[redacted ${value.length} chars]` : '';
            continue;
        }

        if (context.path === 'bookcase' && key === 'name') {
            out[key] = `Bookcase ${context.bookcaseId ?? '?'}`;
            continue;
        }

        out[key] = anonymizeNode(value, { ...context, path: key });
    }
    return out;
}

/**
 * Specialized anonymizer for the legacy shape:
 *   users/{uid}/bookcases/{id}/{ id, name, userId }
 *   users/{uid}/items/{id}/{ id, userId, borrow: { isBorrowed }, item: {...} }
 */
function anonymizeUserSubtree(uid, subtree) {
    if (!subtree || typeof subtree !== 'object') return subtree;
    const out = { ...subtree };

    if (subtree.bookcases) {
        out.bookcases = {};
        for (const [bcId, bc] of Object.entries(subtree.bookcases)) {
            out.bookcases[bcId] = anonymizeNode(bc, { bookcaseId: bcId, path: 'bookcase' });
        }
    }

    if (subtree.items) {
        out.items = {};
        for (const [itemId, item] of Object.entries(subtree.items)) {
            const anonItem = { ...item };
            if (item && typeof item === 'object') {
                if (item.item) anonItem.item = anonymizeNode(item.item, { path: 'item' });
                if (item.borrow) anonItem.borrow = anonymizeNode(item.borrow, { path: 'borrow' });
            }
            out.items[itemId] = anonItem;
        }
    }
    return out;
}

// ────────────────────────────────────────────────────────────────────────────
// Auth helpers
// ────────────────────────────────────────────────────────────────────────────

async function listAllAuthUsers() {
    const all = [];
    let pageToken;
    do {
        // eslint-disable-next-line no-await-in-loop
        const res = await auth.listUsers(1000, pageToken);
        all.push(...res.users);
        pageToken = res.pageToken;
    } while (pageToken);
    return all;
}

/**
 * Update the auth account if it exists, otherwise create one with the given
 * uid. Idempotent — safe to re-run.
 */
async function upsertAuth({ uid, anonymizedEmail }) {
    if (DRY_RUN) {
        console.log(`     [dry-run] auth upsert: uid=${uid} -> ${anonymizedEmail}`);
        return;
    }
    try {
        await auth.updateUser(uid, {
            email: anonymizedEmail,
            password: DEFAULT_PASSWORD,
            emailVerified: true,
            displayName: `User ${anonymizedEmail.split('@')[0]}`,
            photoURL: null,
        });
    } catch (err) {
        if (err.code === 'auth/user-not-found') {
            // No matching auth record — create one with the same uid as the DB node.
            await auth.createUser({
                uid,
                email: anonymizedEmail,
                password: DEFAULT_PASSWORD,
                emailVerified: true,
                displayName: `User ${anonymizedEmail.split('@')[0]}`,
            });
        } else if (err.code === 'auth/email-already-exists') {
            console.warn(`     ! Email collision for uid=${uid}; skipping email update.`);
        } else {
            throw err;
        }
    }
}

async function writeUserSubtree(uid, anonymizedSubtree) {
    if (DRY_RUN) {
        console.log(`     [dry-run] db write: /users/${uid}`);
        return;
    }
    await db.ref(`users/${uid}`).set(anonymizedSubtree);
}

// ────────────────────────────────────────────────────────────────────────────
// Main
// ────────────────────────────────────────────────────────────────────────────

(async function main() {
    console.log('=== Home Library — anonymization (in-place) ===');
    console.log(`source DB : ${SOURCE_DB_URL}`);
    console.log(`dry-run   : ${DRY_RUN}`);
    if (EXCLUDE_UIDS.size > 0) console.log(`exclude   : ${[...EXCLUDE_UIDS].join(', ')}`);
    console.log('');

    console.log('Reading /users from source...');
    const usersSnap = await db.ref('users').once('value');
    const usersTree = usersSnap.val() || {};
    const dbUserIds = Object.keys(usersTree);
    console.log(`  Found ${dbUserIds.length} DB user nodes.`);

    console.log('Listing auth users...');
    const authUsers = await listAllAuthUsers();
    const authByUid = new Map(authUsers.map((u) => [u.uid, u]));
    console.log(`  Found ${authUsers.length} auth records.`);

    // Union of all known UIDs — covers DB-only orphans AND auth-only orphans.
    const allUids = [...new Set([...dbUserIds, ...authUsers.map((u) => u.uid)])];

    // Build mapping uid → anonymized email (deterministic ordering).
    const mapping = [];
    let counter = 1;
    for (const uid of allUids) {
        if (EXCLUDE_UIDS.has(uid)) continue;
        const original = authByUid.get(uid);
        const anonymizedEmail = KEEP_EMAILS && original?.email
            ? original.email
            : `user-${counter}@example.test`;
        mapping.push({
            uid,
            originalEmail: original?.email ?? null,
            anonymizedEmail,
            password: DEFAULT_PASSWORD,
            hadAuthRecord: !!original,
            hadDbNode: dbUserIds.includes(uid),
        });
        counter++;
    }

    fs.writeFileSync(OUTPUT_MAPPING, JSON.stringify(mapping, null, 2));
    console.log(`Wrote mapping to ${OUTPUT_MAPPING} (gitignored — keep local).`);
    console.log('');

    // Process one user at a time so a mid-run failure tells you exactly where.
    let ok = 0;
    let failed = 0;
    for (const m of mapping) {
        const tag = m.hadAuthRecord
            ? (m.hadDbNode ? 'both' : 'auth-only')
            : 'db-only (will create auth)';
        console.log(`[${ok + failed + 1}/${mapping.length}] uid=${m.uid}  ${tag}  ${m.originalEmail || '(no auth)'} -> ${m.anonymizedEmail}`);
        try {
            if (m.hadDbNode) {
                const anonymized = anonymizeUserSubtree(m.uid, usersTree[m.uid]);
                // eslint-disable-next-line no-await-in-loop
                await writeUserSubtree(m.uid, anonymized);
            }
            // Always upsert auth — creates if missing, updates if present.
            // eslint-disable-next-line no-await-in-loop
            await upsertAuth({ uid: m.uid, anonymizedEmail: m.anonymizedEmail });
            ok++;
        } catch (err) {
            console.error(`     FAILED on uid=${m.uid}:`, err.message);
            failed++;
        }
    }

    console.log('');
    console.log(`Done. ok=${ok}  failed=${failed}`);
    if (!DRY_RUN) {
        console.log('');
        console.log('All anonymized accounts now use:');
        console.log(`  password: ${DEFAULT_PASSWORD}`);
        console.log(`  emails:   user-N@example.test (see mapping.json)`);
    }

    await app.delete();
})().catch((err) => {
    console.error('FAILED:', err);
    process.exit(1);
});
