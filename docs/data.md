# Data

[← Back to README](../README.md)

Reference data lives in `src/main/resources/data/`:

| File | Contents |
|---|---|
| `dtc-codes.json` | Common OBD-II fault codes with meaning, severity and common causes. |
| `model-segments.json` | Maps popular Indian car models to a segment (hatchback, sedan, compact SUV, SUV, MPV). |
| `repair-costs.json` | Repair cost ranges in INR by repair and segment, each with its source and date. |

Cost ranges are built from published Indian parts prices (catalogues and price guides) plus assumed labour. Each row's `source` field says which parts are sourced and which are assumed. Where no usable source was found, the row is left out and the assistant says it cannot estimate the cost.

## Updating prices

1. Edit `src/main/resources/data/repair-costs.json`. Each row has `repair`, `segment`, `minInr`, `maxInr`, `source` and `asOf`.
2. Say in `source` which parts of the range are sourced and which are assumed. Don't add a row without a source.
3. Update `asOf` and run `mvn test`.
