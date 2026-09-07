# Residential Housing Society API

Spring Boot 2.7.18 / Java 17 backend for blocks, flats, residents, amenities, bookings, maintenance bills, payments, complaints, and API-key access.

## Run locally

```bash
chmod +x start.sh
bash start.sh
```

The launcher loads `.env_842c7de8-cd98-4525-9429-c0943e421fc9` and defaults to port `29976`. Override with `SERVER_PORT=29976 bash start.sh`.

## Configuration

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |

Use `X-Admin-Key` from `application.properties` only to mint/revoke keys at `/api/v1/api-keys`. Send minted keys as `X-API-Key` for business API calls.

## API

Swagger: `/docs`; OpenAPI JSON: `/api-docs`; health: `/actuator/health`.

| Resource | Endpoints |
|---|---|
| API keys | POST/GET `/api/v1/api-keys`, POST `/api/v1/api-keys/{id}/revoke` |
| Blocks, flats, residents, amenities | POST and GET `/api/v1/{resource}` |
| Bookings, bills, payments, complaints | POST and GET (payments are write-only) `/api/v1/{resource}` |

Business rules include one active resident per flat, duplicate-period bill rejection, payment balance validation, amenity slot/capacity/overlap validation, and complaint category validation.

## Tests

Live curl validation results are in `api_tests/test_results.md`; the final machine-readable report is `api_test_report.xlsx`.

Docker, Compose, Makefile, CI, and websocket infrastructure are intentionally not generated because they are disabled by the requested configuration.
