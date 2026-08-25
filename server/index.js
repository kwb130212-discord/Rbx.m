import http from 'node:http';

const port = Number(process.env.PORT || 8080);
const state = {
  config: { enabled: false, intervalMs: 5000, updatedAt: new Date().toISOString() },
  events: []
};

function send(res, status, body) {
  const data = JSON.stringify(body);
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8' });
  res.end(data);
}

async function readJson(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  if (!chunks.length) return {};
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
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
      const body = await readJson(req);
      state.config = { ...state.config, ...body, updatedAt: new Date().toISOString() };
      return send(res, 200, state.config);
    }

    if (req.method === 'POST' && url.pathname === '/api/events') {
      const body = await readJson(req);
      state.events.push({ ...body, receivedAt: new Date().toISOString() });
      if (state.events.length > 500) state.events.shift();
      return send(res, 201, { ok: true });
    }

    if (req.method === 'GET' && url.pathname === '/api/events') {
      return send(res, 200, state.events.slice(-100));
    }

    return send(res, 404, { error: 'not_found' });
  } catch (error) {
    return send(res, 400, { error: 'bad_request', message: error instanceof Error ? error.message : 'unknown_error' });
  }
});

server.listen(port, '0.0.0.0', () => {
  console.log(`Rbx.m backend listening on ${port}`);
});
