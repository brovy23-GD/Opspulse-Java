# Security policy

OpsPulse is a portfolio demonstration, not an authenticated production monitoring service.

- Do **not** expose the application to the public Internet without authentication, authorization, TLS and rate limiting.
- The PostgreSQL password in `compose.yml` is a **local-only example**, not an acceptable production secret.
- Use environment variables or a secret manager for actual credentials; never commit `.env` or tokens.
- Endpoints accept manual health observations. The application **does not** actively poll services.
- Report security concerns privately to the repository maintainer rather than sharing secrets in issues.

## Browser dashboard security boundary

The dashboard introduces UI forms that can create service inventory, report health and open or advance incidents. These requests use the existing unauthenticated API endpoints. **Do not deploy this version to an untrusted or public network.** Any visitor who can reach the API can currently change data. The dashboard is for local development and portfolio demonstration only until Spring Security, access control, CSRF considerations and deployment hardening are added.

The frontend renders user-supplied service names, keys and notes with DOM `textContent` to avoid treating user data as HTML. It does not store passwords or API tokens.
