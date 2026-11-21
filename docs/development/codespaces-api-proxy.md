# Codespaces API Proxy

The Angular development server proxies browser requests from the frontend origin to the Spring Boot API running in the same Codespaces workspace container.

- Browser request: `/api/v1/...` (same origin as port 4200)
- Development proxy target: `http://localhost:8080`
- Proxy configuration: `frontend/proxy.conf.json`
- Angular CLI serve configuration: `frontend/angular.json`

This avoids cross-origin requests from the browser to the separate Codespaces 8080 forwarded-port hostname, including private-port authentication redirects that can surface as 302/CORS errors.

Run Angular with `npm start -- --host 0.0.0.0 --port 4200`. Restart the dev server after changing proxy configuration.

This is a local development proxy, not a production reverse-proxy deployment configuration. Production deployments must configure the frontend and API behind an intentional same-origin gateway or configure explicit trusted origins and API base URLs.
