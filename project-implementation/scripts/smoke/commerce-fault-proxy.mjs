// Local-only transport fault harness. Never place this proxy in a deployed environment.
// Start: COMMERCE_FAULT_PROXY_ENABLED=isolated-test node commerce-fault-proxy.mjs
// Set COMMERCE_FAULT_CONTROL to an absolute local JSON file. Write one of:
// {"mode":"request-not-arrived","path":"/api/v1/orders"}
// {"mode":"response-lost","path":"/api/v1/orders"}
// The matching POST consumes the file before any effect, so retries pass normally.
import http from 'node:http'
import fs from 'node:fs/promises'
import path from 'node:path'
if (process.env.COMMERCE_FAULT_PROXY_ENABLED !== 'isolated-test') throw new Error('Explicit isolated-test mode required')
const control = process.env.COMMERCE_FAULT_CONTROL
if (!control || !path.isAbsolute(control)) throw new Error('Absolute COMMERCE_FAULT_CONTROL path required')
const target = new URL(process.env.COMMERCE_FAULT_UPSTREAM ?? 'http://127.0.0.1:18087')
if (!['127.0.0.1', 'localhost'].includes(target.hostname)) throw new Error('Only local upstreams are allowed')
const server = http.createServer(async (req, res) => {
  let fault
  try {
    const candidate = JSON.parse(await fs.readFile(control, 'utf8'))
    if (req.method === 'POST' && req.url === candidate.path) {
      // Serialize consumption on this event loop before forwarding.
      await fs.rename(control, control + '.consumed')
      fault = candidate.mode
    }
  } catch (error) { if (error.code !== 'ENOENT') console.error('Fault control could not be consumed:', error.message) }
  if (fault === 'request-not-arrived') {
    console.log(JSON.stringify({ mode: fault, path: req.url, key: req.headers['idempotency-key'] }))
    req.socket.destroy(); return
  }
  const upstream = http.request(target, { method: req.method, path: req.url, headers: req.headers }, (reply) => {
    if (fault === 'response-lost') {
      reply.resume()
      reply.on('end', () => {
        console.log(JSON.stringify({ mode: fault, path: req.url, key: req.headers['idempotency-key'], upstreamStatus: reply.statusCode }))
        req.socket.destroy()
      })
    } else { res.writeHead(reply.statusCode, reply.headers); reply.pipe(res) }
  })
  upstream.on('error', () => { if (!res.headersSent) res.writeHead(502); res.end('Local test upstream unavailable') })
  req.pipe(upstream)
})
server.listen(Number(process.env.COMMERCE_FAULT_PORT ?? 18086), '127.0.0.1', () => console.log('Local commerce fault proxy ready'))
