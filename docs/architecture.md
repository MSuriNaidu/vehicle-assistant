# Architecture

[← Back to README](../README.md)

## System overview

```mermaid
flowchart TB
    U([Car owner]) --> UI[React web app]
    UI -->|POST /api/ai/chat| API[Spring Boot REST controller<br/>+ Advisor service]
    API --> SG{{SafetyGuard<br/>plain Java, no LLM}}

    SG -- dangerous --> STOP[Stop-driving message<br/>EN / HI / TE<br/>returned immediately]
    SG -- safe --> AGENTS

    subgraph AGENTS [Coordinator - sequential workflow, LangChain4j]
        direction LR
        IA[Intake Agent] --> DA[Diagnosis Agent] --> AA[Advisor Agent]
    end

    AGENTS -->|chat calls| LLM[[OpenRouter API<br/>LLM]]
    AGENTS -->|lookupDtc, estimateCost| TOOLS[VehicleTools]
    TOOLS --> REPO[Repository interfaces]
    REPO --> JSON[(JSON data files<br/>DTC codes, repair costs,<br/>model segments)]
```

Both paths return the same `AdviceResponse` (urgency, answer, `safetyTriggered`) to the UI.

## Request flow

```mermaid
sequenceDiagram
    actor User
    participant API as REST controller
    participant SG as SafetyGuard
    participant IN as Intake Agent
    participant DX as Diagnosis Agent
    participant AD as Advisor Agent
    participant T as VehicleTools
    participant G as LLM (OpenRouter)

    User->>API: "Brake pedal goes to the floor"
    API->>SG: check(message)
    alt dangerous symptom
        SG-->>API: STOP_NOW + tow message
        Note over SG,API: No LLM call is made
    else not dangerous
        API->>IN: message
        IN->>G: extract vehicle issue
        G-->>IN: VehicleIssue
        API->>SG: checkDtcCodes(codes)
        IN->>DX: vehicleIssue
        DX->>T: lookupDtc(code)
        DX->>G: rank causes, set urgency
        G-->>DX: Diagnosis
        DX->>AD: diagnosis
        AD->>T: estimateCost(repair, model)
        AD->>G: write plain-language answer
        G-->>AD: answer
        AD-->>API: AdviceResponse
    end
    API-->>User: urgency + answer
```

## Components

| Component | Responsibility |
|---|---|
| **SafetyGuard** | Plain-Java regex rules for brake failure, steering loss, overheating, fuel smell, smoke and oil pressure (English, Hindi, Telugu), plus a list of dangerous fault codes. Runs before any LLM call. |
| **Intake Agent** | LLM agent. Extracts make, model, year, symptoms and fault codes into a `VehicleIssue`. Leaves unknown fields null. |
| **Diagnosis Agent** | LLM agent. Ranks likely causes and sets urgency. Calls `lookupDtc` for fault codes. |
| **Advisor Agent** | LLM agent. Writes the final answer in the user's language: urgency, causes, cost range, mechanic questions, next step. Calls `estimateCost`. |
| **Coordinator** | Workflow orchestrator. Runs the agents in a fixed sequence and passes results through shared state. It makes no LLM call and decides nothing, by design. |
| **VehicleTools** | Tools exposed to the LLM. They depend on repository interfaces, not on the data files. |
| **Repositories** | `DtcRepository`, `RepairCostRepository`, `ModelSegmentRepository`. JSON-backed today. |

## Design decisions

- **Safety rules live outside the LLM.** For dangerous symptoms, the answer must be the same every time and must still work if the LLM service is down. `SafetyGuard` is deterministic Java and short-circuits the pipeline.
- **Tools instead of prompts for data.** Fault-code meanings and prices come from tools backed by data files. When data is missing, the tool returns an explicit "no information" message, and the prompts forbid inventing values.
- **Repository interfaces.** Reference data is read through interfaces, so the JSON files can be replaced by MongoDB or another store without changing the tools or agents.
- **Structured outputs.** Agents pass typed records (`VehicleIssue`, `Diagnosis`) with an `Urgency` enum, so urgency can't drift into free text and can be checked in tests.
- **Sequential workflow first.** Three agents plus a fixed-order coordinator are easier to debug and cheaper than a dynamic supervisor agent. No case has needed dynamic routing yet.
