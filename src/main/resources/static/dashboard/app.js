'use strict';
(function () {
  const apiBase = '/api/v1';
  const $ = (id) => document.getElementById(id);
  const state = { services: [], incidents: [], dashboard: null, busy: false, refreshId: 0, requestSequence: 0 };
  const fmt = new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  const colors = { HEALTHY: '#35d0a0', DEGRADED: '#f6bc62', DOWN: '#f17c87', UNKNOWN: '#77859a' };

  function el(tag, text, cls) {
    const node = document.createElement(tag);
    if (cls) node.className = cls;
    if (text !== undefined && text !== null) node.textContent = String(text);
    return node;
  }
  function dateLabel(instant) {
    if (!instant) return '—';
    const time = new Date(instant);
    return Number.isNaN(time.getTime()) ? '—' : fmt.format(time);
  }
  function number(id, value) { $(id).textContent = String(value); }
  function alertMessage(message, isError) {
    const node = $('notice');
    node.textContent = message;
    node.className = isError ? 'error' : '';
    node.hidden = false;
  }
  function clearMessage() { $('notice').hidden = true; }
  async function api(path, options) {
    const settings = Object.assign({ headers: { Accept: 'application/json' } }, options || {});
    const isGet = !settings.method || settings.method.toUpperCase() === 'GET';
    if (isGet) settings.cache = 'no-store';
    // A unique GET URL also bypasses stale intermediary caches, not just browser caches.
    const url = apiBase + path + (isGet ? (path.includes('?') ? '&' : '?') + '_refresh=' + Date.now() + '-' + (++state.requestSequence) : '');
    const response = await fetch(url, settings);
    let payload = null;
    try { payload = await response.json(); } catch (_) { /* empty/invalid JSON */ }
    if (!response.ok) {
      const message = payload && (payload.message || payload.error);
      throw new Error(message || ('Request failed (HTTP ' + response.status + ')'));
    }
    return payload;
  }
  function jsonOptions(method, data) {
    return { method: method, headers: { 'Content-Type': 'application/json', Accept: 'application/json' }, body: JSON.stringify(data) };
  }
  function badge(value, prefix) {
    const tag = el('span', value, (prefix === 'health' ? 'status-tag ' : 'severity-tag ') + prefix + '-' + value);
    if (prefix === 'health') tag.prepend(el('span', '', 'dot ' + ({ HEALTHY: 'green', DEGRADED: 'amber', DOWN: 'red', UNKNOWN: 'gray' }[value] || 'gray')));
    return tag;
  }
  function option(select, value, name) { const o = el('option', name); o.value = String(value); select.append(o); }
  function fillServiceSelectors() {
    for (const id of ['observe-service', 'incident-service']) {
      const select = $(id);
      const existing = select.value;
      select.replaceChildren();
      option(select, '', 'Choose a service');
      state.services.forEach((service) => option(select, service.id, service.name + ' [' + service.environment + ']'));
      if (Array.from(select.options).some((o) => o.value === existing)) select.value = existing;
    }
  }
  function updateMetrics() {
    const stats = state.dashboard;
    number('count-total', stats.totalServices);
    number('count-healthy', stats.healthy);
    number('count-unhealthy', stats.degraded + stats.down);
    number('count-incidents', stats.activeIncidents);
    const parts = [['HEALTHY', stats.healthy], ['DEGRADED', stats.degraded], ['DOWN', stats.down], ['UNKNOWN', stats.unknown]];
    for (const [name, value] of parts) number('legend-' + name.toLowerCase(), value);
    const total = stats.totalServices;
    const fraction = total ? Math.round(100 * stats.healthy / total) : 0;
    number('donut-percent', total ? fraction + '%' : '—');
    let index = 0;
    const sections = [];
    for (const [name, value] of parts) {
      if (!value) continue;
      const next = index + (value / total) * 100;
      sections.push(colors[name] + ' ' + index.toFixed(2) + '% ' + next.toFixed(2) + '%');
      index = next;
    }
    $('health-donut').style.background = total ? 'conic-gradient(' + sections.join(', ') + ')' : '#34455b';
    $('health-donut').setAttribute('aria-label', total ? 'Reported healthy: ' + stats.healthy + ' of ' + total + ' services' : 'No service health data');
  }
  function displayServices() {
    const query = $('search-services').value.trim().toLowerCase();
    const filter = $('filter-health').value;
    const rows = state.services.filter((service) => {
      const matches = (service.name + ' ' + service.serviceKey + ' ' + service.environment).toLowerCase().includes(query);
      return matches && (!filter || service.health === filter);
    });
    const tbody = $('services-body');
    tbody.replaceChildren();
    number('service-count', '(' + rows.length + ')');
    if (!rows.length) { const tr = el('tr'); const td = el('td', 'No matching services. Register a service or adjust your filters.', 'empty'); td.colSpan = 5; tr.append(td); tbody.append(tr); return; }
    rows.sort((a,b) => a.name.localeCompare(b.name)).forEach((service) => {
      const tr = el('tr');
      const name = el('td');
      name.append(el('div', service.name, 'service-primary'), el('div', service.serviceKey, 'service-sub'));
      const environment = el('td', service.environment);
      const health = el('td'); health.append(badge(service.health, 'health'));
      const registered = el('td', dateLabel(service.createdAt), 'timestamp-cell');
      const action = el('td');
      const button = el('button', 'Observe →', 'row-action');
      button.type = 'button';
      button.addEventListener('click', () => { $('observe-service').value = service.id; location.hash = '#overview'; $('observe-status').focus(); });
      action.append(button);
      tr.append(name, environment, health, registered, action);
      tbody.append(tr);
    });
  }
  function displayIncidents() {
    const tbody = $('incidents-body');
    tbody.replaceChildren();
    number('incident-count', '(' + state.incidents.length + ')');
    if (!state.incidents.length) { const tr = el('tr'); const td = el('td', 'No incidents recorded. Open an incident to start tracking.', 'empty'); td.colSpan = 6; tr.append(td); tbody.append(tr); return; }
    const names = new Map(state.services.map((s) => [s.id, s.name]));
    state.incidents.sort((a,b) => Date.parse(b.openedAt) - Date.parse(a.openedAt)).forEach((incident) => {
      const tr = el('tr');
      const title = el('td'); title.append(el('div', incident.title, 'incident-primary'), el('div', 'INC-' + String(incident.id).padStart(4, '0'), 'service-sub'));
      const service = el('td', names.get(incident.serviceId) || ('Service #' + incident.serviceId));
      const severity = el('td'); severity.append(badge(incident.severity, 'severity'));
      const status = el('td'); status.append(badge(incident.status, 'status'));
      const opened = el('td', dateLabel(incident.openedAt), 'timestamp-cell');
      const action = el('td');
      const next = incident.status === 'OPEN' ? 'INVESTIGATING' : 'RESOLVED';
      const button = el('button', incident.status === 'RESOLVED' ? 'Resolved ✓' : (next === 'INVESTIGATING' ? 'Investigate →' : 'Resolve ✓'), 'row-action');
      button.type = 'button';
      button.disabled = incident.status === 'RESOLVED';
      if (!button.disabled) button.addEventListener('click', () => mutate(() => api('/incidents/' + incident.id, jsonOptions('PATCH', { status: next })), 'Incident updated to ' + next.toLowerCase() + '.'));
      action.append(button);
      tr.append(title, service, severity, status, opened, action);
      tbody.append(tr);
    });
  }
  async function refresh(quiet) {
    // Only the newest refresh may paint the dashboard: slower prior requests are ignored.
    const refreshId = ++state.refreshId;
    try {
      const dashboard = await api('/dashboard');
      if (refreshId !== state.refreshId) return;
      // Update counters immediately. A later inventory or incident-history error must
      // never leave an already-resolved incident visible as an active summary count.
      state.dashboard = dashboard;
      updateMetrics();
      $('time-label').textContent = 'Refreshed ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const services = await api('/services');
      if (refreshId !== state.refreshId) return;
      const incidentGroups = await Promise.all(services.map((s) => api('/services/' + encodeURIComponent(s.id) + '/incidents')));
      if (refreshId !== state.refreshId) return;
      state.services = services;
      state.incidents = incidentGroups.flat();
      fillServiceSelectors();
      displayServices();
      displayIncidents();
      if (!quiet) alertMessage('Dashboard refreshed successfully.', false);
    } catch (error) {
      if (refreshId === state.refreshId) alertMessage('Unable to load operations data: ' + error.message, true);
    }
  }
  async function mutate(action, message, after) {
    if (state.busy) return;
    state.busy = true;
    clearMessage();
    document.querySelectorAll('button[type="submit"], .row-action').forEach((button) => { button.disabled = true; });
    try {
      await action();
      if (after) after();
      await refresh(true);
      if ($('notice').hidden || !$('notice').classList.contains('error')) alertMessage(message, false);
    } catch (error) { alertMessage(error.message, true); }
    finally {
      state.busy = false;
      document.querySelectorAll('button[type="submit"]').forEach((button) => { button.disabled = false; });
      displayIncidents();
      displayServices();
    }
  }
  function setup() {
    $('refresh').addEventListener('click', () => refresh(false));
    $('load-demo-data').addEventListener('click', () => mutate(() => api('/demo/load', { method: 'POST' }), 'Demo services and incidents loaded. Existing records were preserved.'));
    $('search-services').addEventListener('input', displayServices);
    $('filter-health').addEventListener('change', displayServices);
    $('create-service-toggle').addEventListener('click', () => { $('register-panel').hidden = false; $('service-key').focus(); });
    $('close-register').addEventListener('click', () => { $('register-panel').hidden = true; });
    $('new-incident-toggle').addEventListener('click', () => { $('incident-form').hidden = !$('incident-form').hidden; if (!$('incident-form').hidden) $('incident-service').focus(); });
    $('register-form').addEventListener('submit', (event) => {
      event.preventDefault();
      const payload = { serviceKey: $('service-key').value.trim(), name: $('service-name').value.trim(), environment: $('service-env').value.trim() };
      mutate(() => api('/services', jsonOptions('POST', payload)), 'Service registered.', () => { $('register-form').reset(); $('register-panel').hidden = true; });
    });
    $('observe-form').addEventListener('submit', (event) => {
      event.preventDefault();
      const id = $('observe-service').value;
      if (!id) { alertMessage('Choose a service first.', true); return; }
      const payload = { health: $('observe-status').value, note: $('observe-note').value.trim() || null };
      mutate(() => api('/services/' + encodeURIComponent(id) + '/observations', jsonOptions('POST', payload)), 'Health observation recorded.', () => { $('observe-note').value = ''; });
    });
    $('incident-form').addEventListener('submit', (event) => {
      event.preventDefault();
      const id = Number($('incident-service').value);
      if (!id) { alertMessage('Choose a service first.', true); return; }
      const payload = { serviceId: id, title: $('incident-title').value.trim(), severity: $('incident-severity').value };
      mutate(() => api('/incidents', jsonOptions('POST', payload)), 'Incident opened.', () => { $('incident-form').reset(); $('incident-form').hidden = true; });
    });
    refresh(true);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', setup);
  else setup();
})();
