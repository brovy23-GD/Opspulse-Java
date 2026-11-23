'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const test = require('node:test');
const path = require('node:path');

class Element {
  constructor(tag = 'div') {
    this.tagName = tag;
    this.children = [];
    this.listeners = {};
    this.textContent = '';
    this.value = '';
    this.hidden = false;
    this.disabled = false;
    this.style = {};
    this.attributes = {};
    this.className = '';
    this.classList = { contains: () => false };
  }
  append(...children) { this.children.push(...children); }
  prepend(...children) { this.children.unshift(...children); }
  replaceChildren(...children) { this.children = [...children]; }
  setAttribute(name, value) { this.attributes[name] = value; }
  addEventListener(type, callback) { this.listeners[type] = callback; }
  focus() {}
  reset() {}
  get options() { return this.children.filter((node) => node.tagName === 'option'); }
}

async function drain() {
  for (let i = 0; i < 15; i++) await new Promise((resolve) => setImmediate(resolve));
}

test('a resolved incident updates the dashboard count to zero with uncached API reads', async () => {
  const nodes = new Map();
  const document = {
    readyState: 'complete',
    getElementById(id) {
      if (!nodes.has(id)) nodes.set(id, new Element());
      return nodes.get(id);
    },
    createElement(tag) { return new Element(tag); },
    querySelectorAll() { return []; },
    addEventListener() {}
  };
  const requests = [];
  let activeIncidents = 1;
  const services = [{ id: 1, name: 'Payments API', serviceKey: 'payments-api', environment: 'production', health: 'HEALTHY', createdAt: '2026-11-23T12:00:00Z' }];
  const incident = { id: 1, serviceId: 1, title: 'Timeout', severity: 'HIGH', status: 'RESOLVED', openedAt: '2026-11-23T12:00:00Z' };
  const fetch = async (url, opts) => {
    requests.push({ url, opts });
    let data;
    if (url.startsWith('/api/v1/dashboard?')) {
      data = { totalServices: 1, healthy: 1, degraded: 0, down: 0, unknown: 0, activeIncidents };
    } else if (url.startsWith('/api/v1/services?')) {
      data = services;
    } else if (url.startsWith('/api/v1/services/1/incidents?')) {
      data = [incident];
    } else throw Error('Unexpected API request: ' + url);
    return { ok: true, json: async () => data };
  };
  const code = fs.readFileSync(path.join(__dirname, '../src/main/resources/static/dashboard/app.js'), 'utf8');
  vm.runInNewContext(code, { document, fetch, location: { hash: '' }, console, Intl, Date, Map, Promise, Number, String, Array, setTimeout }, { filename: 'app.js' });
  await drain();
  assert.equal(nodes.get('count-incidents').textContent, '1', 'initial active count');
  activeIncidents = 0;
  nodes.get('refresh').listeners.click();
  await drain();
  assert.equal(nodes.get('count-incidents').textContent, '0', 'counter reflects resolved API count');
  assert.equal(nodes.get('incident-count').textContent, '(1)', 'historical incident stays in the table');
  assert.ok(requests.every(({ url, opts }) => opts.cache === 'no-store' && url.includes('_refresh=')), 'every GET bypasses caches');
  assert.equal(new Set(requests.map(({ url }) => url)).size, requests.length, 'cache keys are unique');
});

test('summary count updates even when incident-history fetching fails', async () => {
  const nodes = new Map();
  const document = {
    readyState: 'complete',
    getElementById(id) { if (!nodes.has(id)) nodes.set(id, new Element()); return nodes.get(id); },
    createElement(tag) { return new Element(tag); },
    querySelectorAll() { return []; },
    addEventListener() {}
  };
  const fetch = async (url) => {
    if (url.startsWith('/api/v1/dashboard?')) return { ok: true, json: async () => ({ totalServices: 1, healthy: 1, degraded: 0, down: 0, unknown: 0, activeIncidents: 0 }) };
    if (url.startsWith('/api/v1/services?')) return { ok: true, json: async () => [{ id: 1, name: 'Payments', serviceKey: 'payments', environment: 'dev', health: 'HEALTHY', createdAt: '2026-11-23T12:00:00Z' }] };
    throw Error('Simulated incident history outage');
  };
  const code = fs.readFileSync(path.join(__dirname, '../src/main/resources/static/dashboard/app.js'), 'utf8');
  vm.runInNewContext(code, { document, fetch, location: { hash: '' }, console, Intl, Date, Map, Promise, Number, String, Array, setTimeout }, { filename: 'app.js' });
  await drain();
  assert.equal(nodes.get('count-incidents').textContent, '0');
  assert.equal(nodes.get('notice').className, 'error', 'partial history error is visible');
});
