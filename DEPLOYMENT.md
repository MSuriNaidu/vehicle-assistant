# Render deployment

The Render Blueprint in `render.yaml` deploys the React frontend and Spring Boot
API as one Docker web service. Spring Boot serves the frontend build and handles
`/api` requests on the same origin, so production does not use the Vite dev
proxy or require CORS configuration.

1. Push the repository to GitHub and create a Render Blueprint from it.
2. Enter `GROQ_API_KEY` when Render prompts for the secret. `GROQ_BASE_URL`
   defaults to Groq's OpenAI-compatible endpoint and `GROQ_MODEL` defaults to
   `openai/gpt-oss-120b`. Override those values for another compatible provider.
3. Wait for the Docker build and deploy to finish. Render supplies `PORT`; the
   app binds to that port.
4. Verify `/actuator/health`, load the app, and submit a chat request.

The `render` Spring profile enables proxy-aware request handling and keeps
actuator health details hidden. For local development, activate the `local`
profile with `SPRING_PROFILES_ACTIVE=local`; it enables application debug logs
and health details. Do not use the `local` profile in a public deployment.

The Vite proxy remains for local development. Set `VITE_API_PROXY_TARGET` in
`frontend/.env.local` only when the development backend is not on
`http://localhost:8080`. In the production Docker deployment, browser requests
remain same-origin and are served directly by Spring Boot.

Render's free service may sleep when idle and cold-start on the next visit.
The model provider still has its own quotas, rate limits, and possible charges.
Keep provider credentials in Render environment variables; never commit them or
expose them through frontend `VITE_` variables.
