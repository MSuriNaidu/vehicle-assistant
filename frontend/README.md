# VigiDrive web app

The React + TypeScript frontend runs separately from Spring Boot during development.
It sends `POST /api/ai/chat` requests to the backend; Vite proxies `/api` to
`http://localhost:8080`.

```bash
cd frontend
npm install
npm run dev
```

Start the Spring Boot application separately on port 8080. For a production
static build, run `npm run build`; the deploy target must serve `frontend/dist`
and proxy `/api` requests to the Spring Boot service.
