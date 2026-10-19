# StatusSync Wear — Plan del agente BACKEND

> **Carpeta propia:** `back-wear/` · **Contrato:** `CONTRATO.md`

## Stack
Node ≥ 22, TypeScript, Hono, zod, supabase-js, jose (verificación JWT), pino, vitest. Misma estructura que `back-mobile`:
`src/{config,contract,lib,middleware,repositories/{memory,supabase},routes,services}`.

## Decisiones
- Repositorios detrás de **puertos** con adaptador `memory` (tests/dev) y `supabase` (real).
- `service_role` solo en `.env` del backend.
- **IA (`services/suggest`)**: primero reglas determinísticas (agenda activa → `class`; quietud sostenida + sin pasos → `focus`; resto → `free`), con `confidence` y `reason`; un LLM opcional puede refinar. Nunca recibe datos crudos de sensores, solo agregados.
- Vencimiento de temporizadores: job `expireStatuses()` cada minuto (o pg_cron en la BD).

## Fases
- **B0** Scaffold, `/health`, config con zod, logger, errorHandler, requestId.
- **B1** Auth JWT + `PUT /status` (memory) con tests.
- **B2** `GET /groups/:id/statuses`, membresía y autorización.
- **B3** `PUT /schedule` y servicio `suggest` por reglas + tests.
- **B4** Adaptador Supabase + expiración de estados.
- **B5** Rate limit, Dockerfile, `render.yaml`, DEPLOY.md.

Verificaciones: `npm run typecheck && npm run lint && npm test`.
