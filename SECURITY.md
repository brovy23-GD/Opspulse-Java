# Security policy

OpsPulse is a portfolio demonstration, not an authenticated production monitoring service.

- Do **not** expose the application to the public Internet without authentication, authorization, TLS and rate limiting.
- The PostgreSQL password in `compose.yml` is a **local-only example**, not an acceptable production secret.
- Use environment variables or a secret manager for actual credentials; never commit `.env` or tokens.
- Endpoints accept manual health observations. Optional monitoring performs scheduled HTTPS checks of trusted, operator-configured targets only when explicitly enabled. The browser cannot submit monitoring target URLs. Standard HTTPS port 443, no redirects, and timeouts reduce exposure but do not prevent DNS-based SSRF; DNS names can resolve to internal networks. Never allow untrusted parties to modify configuration.
- Report security concerns privately to the repository maintainer rather than sharing secrets in issues.

## Browser dashboard security boundary

The dashboard introduces UI forms that can create service inventory, report health and open or advance incidents. These requests use the existing unauthenticated API endpoints. **Do not deploy this version to an untrusted or public network.** Any visitor who can reach the API can currently change data. The dashboard is for local development and portfolio demonstration only until Spring Security, access control, CSRF considerations and deployment hardening are added.

The frontend renders user-supplied service names, keys and notes with DOM `textContent` to avoid treating user data as HTML. It does not store passwords or API tokens.

## Automated incident lifecycle

After repeated check failures, the monitor creates a HIGH-severity incident owned by the monitoring workflow, with a boolean source flag. After repeated successful checks, it closes only monitor-created incidents. Manually created incidents are not automatically closed. The service does not send notifications, and the API has no authentication; it must remain a trusted local demonstration.
