import http from 'node:http';
import { mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';

const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || '0.0.0.0';
const DATA_DIR = process.env.RBXM_DATA_DIR || join(process.cwd(), 'data');
const DATA_FILE = join(DATA_DIR, 'state.json');
const API_TOKEN = process.env.RBXM_API_TOKEN || '';
const MAX_BODY = 256 * 1024;
const MAX_EVENTS = 5000;
const MAX_PROFILES = 100;

const defaultState = {
  version: 1,
  config: { enabled: false, intervalMs: 5000, updatedAt: new Date().toISOString() },
  accounts: [
    { id: 'family-1', name: '가족 계정 1', selected: true, updatedAt: null },
    { id: 'family-2', name: '가족 계정 2', selected: false, updatedAt: null }
  ],
  profiles: [],
  events: []
};

let state = structuredClone(defaultState);
let saveTimer = null;
let savePending = false;

const jsonHeaders = {
  'content-type': 'application/json; charset=utf-8',
  'cache-control': 'no-store',
  'x-content-type-options': 'nosniff'
};

function send(res, status, body, extra = {}) {
  const data = JSON.stringify(body);
  res.writeHead(status, { ...jsonHeaders, 'content-length': Buffer.byteLength(data), ...extra });
  res.end(data);
}

function noContent(res) {
  res.writeHead(204, { 'cache-control': 'no-store' });
  res.end();
}

function authorized(req) {
  if (!API_TOKEN) return true;
  const header = req.headers.authorization || '';
  return header === `Bearer ${API_TOKEN}` || req.headers['x-rbxm-token'] === API_TOKEN;
}

async function readJson(req) {
  let size = 0;
  const chunks = [];
  for await (const chunk of req) {
    size += chunk.length;
    if (size > MAX_BODY) throw new Error('payload_too_large');
    chunks.push(chunk);
  }
  if (!size) return {};
  const text = Buffer.concat(chunks).toString('utf8');
  return JSON.parse(text);
}

function scheduleSave() {
  savePending = true;
  if (saveTimer) return;
  saveTimer = setTimeout(async () => {
    saveTimer = null;
    if (!savePending) return;
    savePending = false;
    try {
      await mkdir(DATA_DIR, { recursive: true });
      const tmp = `${DATA_FILE}.tmp`;
      await writeFile(tmp, JSON.stringify(state), 'utf8');
      await rename(tmp, DATA_FILE);
    } catch (error) {
      console.error('state save failed:', error?.message || error);
    }
  }, 250);
}

async function loadState() {
  try {
    const raw = await readFile(DATA_FILE, 'utf8');
    const loaded = JSON.parse(raw);
    state = {
      ...defaultState,
      ...loaded,
      config: { ...defaultState.config, ...(loaded.config || {}) },
      accounts: Array.isArray(loaded.accounts) ? loaded.accounts.slice(0, 2) : defaultState.accounts,
      profiles: Array.isArray(loaded.profiles) ? loaded.profiles.slice(0, MAX_PROFILES) : [],
      events: Array.isArray(loaded.events) ? loaded.events.slice(-MAX_EVENTS) : []
    };
  } catch {
    await mkdir(DATA_DIR, { recursive: true });
    scheduleSave();
  }
}

function addEvent(body) {
  state.events.push({ ...body, receivedAt: new Date().toISOString() });
  if (state.events.length > MAX_EVENTS) state.events.splice(0, state.events.length - MAX_EVENTS);
  scheduleSave();
}

const server = http.createServer({
  keepAlive: true,
  maxHeaderSize: 16 * 1024,
  requestTimeout: 15_000,
  headersTimeout: 10_000,
  keepAliveTimeout: 5_000
}, async (req, res) => {
  try {
    if (!authorized(req)) return send(res, 401, { error: 'unauthorized' });

    const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);

    if (req.method === 'GET' && url.pathname === '/health') {
      return send(res, 200, {
        ok: true,
        service: 'rbxm-cloud-backend',
        uptimeSec: Math.round(process.uptime()),
        memory: process.memoryUsage().rss,
        time: new Date().toISOString()
      });
    }

    if (req.method === 'GET' && url.pathname === '/api/config') return send(res, 200, state.config);

    if (req.method === 'PUT' && url.pathname === '/api/config') {
      const body = await readJson(req);
      state.config = { ...state.config, ...body, updatedAt: new Date().toISOString() };
      scheduleSave();
      return send(res, 200, state.config);
    }

    if (req.method === 'GET' && url.pathname === '/api/accounts') return send(res, 200, state.accounts);

    if (req.method === 'PUT' && url.pathname.startsWith('/api/accounts/')) {
      const id = decodeURIComponent(url.pathname.slice('/api/accounts/'.length));
      const account = state.accounts.find((item) => item.id === id);
      if (!account) return send(res, 404, { error: 'account_not_found' });
      const body = await readJson(req);
      account.name = typeof body.name === 'string' ? body.name.slice(0, 80) : account.name;
      if (body.selected === true) state.accounts.forEach((item) => { item.selected = item.id === id; });
      account.updatedAt = new Date().toISOString();
      scheduleSave();
      return send(res, 200, account);
    }

    if (req.method === 'GET' && url.pathname === '/api/profiles') return send(res, 200, state.profiles);

    if (req.method === 'PUT' && url.pathname.startsWith('/api/profiles/')) {
      const id = decodeURIComponent(url.pathname.slice('/api/profiles/'.length));
      const body = await readJson(req);
      if (!id || id.length > 80) return send(res, 400, { error: 'invalid_profile_id' });
      const safe = { id, accountId: String(body.accountId || 'family-1'), name: String(body.name || id).slice(0, 80), settings: body.settings || {}, updatedAt: new Date().toISOString() };
      const index = state.profiles.findIndex((item) => item.id === id);
      if (index >= 0) state.profiles[index] = safe;
      else {
        if (state.profiles.length >= MAX_PROFILES) return send(res, 409, { error: 'profile_limit_reached' });
        state.profiles.push(safe);
      }
      scheduleSave();
      return send(res, 200, safe);
    }

    if (req.method === 'DELETE' && url.pathname.startsWith('/api/profiles/')) {
      const id = decodeURIComponent(url.pathname.slice('/api/profiles/'.length));
      state.profiles = state.profiles.filter((item) => item.id !== id);
      scheduleSave();
      return noContent(res);
    }

    if (req.method === 'POST' && url.pathname === '/api/events') {
      addEvent(await readJson(req));
      return send(res, 201, { ok: true });
    }

    if (req.method === 'GET' && url.pathname === '/api/events') {
      const limit = Math.min(Math.max(Number(url.searchParams.get('limit') || 100), 1), 500);
      return send(res, 200, state.events.slice(-limit));
    }

    if (req.method === 'DELETE' && url.pathname === '/api/events') {
      state.events = [];
      scheduleSave();
      return noContent(res);
    }

    return send(res, 404, { error: 'not_found' });
  } catch (error) {
    const status = error?.message === 'payload_too_large' ? 413 : 400;
    return send(res, status, { error: status === 413 ? 'payload_too_large' : 'bad_request', message: error?.message || 'unknown_error' });
  }
});

server.on('clientError', (error, socket) => {
  socket.end('HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n');
});

await loadState();
server.listen(PORT, HOST, () => console.log(`Rbx.m backend listening on http://${HOST}:${PORT}`));
