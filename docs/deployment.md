# Deployment

[← Back to README](../README.md)

The service and web UI are deployed on [Render](https://render.com/) (free tier for the pilot). The OpenRouter API key and model name are supplied as environment variables.

For a production build:

1. Build the frontend: `cd frontend && npm run build`. This produces static files in `frontend/dist`.
2. Serve `frontend/dist` from your host (a static site, or the web server in front of the app).
3. Route `/api` requests to the Spring Boot service, as the Vite dev server does locally.
4. Build and run the backend with `mvn package` and `OPENROUTER_API_KEY` set.

## Environment variables

| Variable | Purpose |
|---|---|
| `OPENROUTER_API_KEY` | OpenRouter API key (set in the host's dashboard, never in the repo). |
| `OPENROUTER_MODEL` (optional) | Model ID override. |

## Free-tier notes

Render's free tier sleeps after about 15 minutes of inactivity, so the first request afterwards can take around a minute. OpenRouter's free models are rate limited; use a paid model for real users.
