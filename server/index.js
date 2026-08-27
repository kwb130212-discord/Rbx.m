import http from 'node:http';

const port = Number(process.env.PORT || 8080);
const apiKey = process.env.API_KEY || '';
const state = {
  config: { enabled: false, intervalMs: 1500, mode: 'BALANCED', updatedAt: new Date().toISOString() },
  events: []
};

function send(res, status, body) {
  const data = JSON.stringify(body);
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store'
  });
  res.end(data);
}

function authorized(req) {
  if (!apiKey) return process.env.NODE_ENV !== 'production';
  return req.headers.authorization === `Bearer ${apiKey}` || req.headers['x-api-key'] === apiKey;
}

async function readJson(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  if (!chunks.length) return {};
  if (Buffer.concat(chunks).length > 256 * 1024) throw new Error('payload_too_large');
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

function validConfig(body) {
  const allowedModes = new Set(['SAFE', 'BALANCED', 'AGGRESSIVE']);
  return (!('enabled' in body) || typeof body.enabled === 'boolean') &&
    (!('intervalMs' in body) || Number.isFinite(body.intervalMs)) &&
    (!('mode' in body) || allowedModes.has(body.mode));
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

    if (req.method === 'GET' && url.pathname === '/health') {
      return send(res, 200, { ok: true, service: 'rbxm-cloud-backend', time: new Date().toISOString() });
    }

    if (req.method === 'GET' && url.pathname === '/api/config') {
      return send(res, 200, state.config);
    }

    if (req.method === 'PUT' && url.pathname === '/api/config') {
      if (!authorized(req)) return send(res, 401, { error: 'unauthorized' });
      const body = await readJson(req);
      if (!validConfig(body)) return send(res, 422, { error: 'invalid_config' });
      state.config = {
        ...state.config,
        ...body,
        intervalMs: Math.max(500, Math.min(10000, Number(body.intervalMs ?? state.config.intervalMs))),
        updatedAt: new Date().toISOString()
      };
      return send(res, 200, state.config);
    }

    if (req.method === 'POST' && url.pathname === '/api/events') {
      if (!authorized(req)) return send(res, 401, { error: 'unauthorized' });
      const body = await readJson(req);
      state.events.push({ ...body, receivedAt: new Date().toISOString() });
      if (state.events.length > 500) state.events.splice(0, state.events.length - 500);
      return send(res, 201, { ok: true });
    }

    if (req.method === 'GET' && url.pathname === '/api/events') {
      return send(res, 200, state.events.slice(-100));
    }

    return send(res, 404, { error: 'not_found' });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'unknown_error';
    return send(res, message === 'payload_too_large' ? 413 : 400, { error: 'bad_request', message });
  }
});

server.listen(port, '0.0.0.0', () => {
  console.log(`Rbx.m backend listening on ${port}`);
});
