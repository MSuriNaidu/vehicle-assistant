# API

[← Back to README](../README.md)

<!-- TODO: confirm the path and field names against your controller -->
`POST /api/ai/chat`

Request:

```json
{
  "message": "Engine light is on and the car jerks when I accelerate. Swift 2018.",
  "language": "en"
}
```

Response:

```json
{
  "urgency": "SERVICE_SOON",
  "answer": "Urgency: ...\nLikely causes: ...\nEstimated cost: ...\nQuestions to ask your mechanic: ...\nNext step: ...",
  "safetyTriggered": false
}
```

`urgency` is one of `STOP_NOW`, `DRIVE_CAREFULLY`, `SERVICE_SOON`, `ROUTINE`. When the safety layer fires, `urgency` is `STOP_NOW` and `safetyTriggered` is `true`.

Interactive docs are available through Swagger UI at `/swagger-ui.html` when enabled.
