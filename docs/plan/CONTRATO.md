# Contrato compartido (CONGELADO)

## 1. Estados de disponibilidad
| `state` | Etiqueta UI | Color | Ícono | Duración por defecto |
|---|---|---|---|---|
| `free` | Libre | Verde `#2E9E5B` | check | — |
| `focus` | En Enfoque | Rojo `#D64545` | target | 50 min |
| `class` | En Clase | Ámbar `#E0A100` | book | bloque de agenda |
| `call` | En llamada | Morado `#7A4FD6` | phone | 30 min |

Regla de temporizador: todo estado distinto de `free` lleva `until` (UTC). Al vencer, el backend/pg_cron lo devuelve a `free`.

## 2. Origen del cambio (`source`)
`manual` (toque en reloj) · `timer` (vencimiento) · `auto_calendar` (agenda) · `auto_motion` (patrón de movimiento).
Un cambio `manual` siempre gana a `auto_*` durante el bloque en curso.

## 3. Modelo de datos
- `profiles(id, display_name, created_at)`
- `groups(id, name, owner_id, invite_code)` · `group_members(group_id, user_id)`
- `statuses(user_id PK, state, until, source, updated_at)`  ← 1 fila por usuario, upsert
- `schedule_blocks(id, user_id, state, starts_at, ends_at)`  ← agenda
- `motion_features(id, user_id, window_start, steps, stillness_ratio, state_hint)` ← agregados, nunca datos crudos

## 4. API (prefijo `/v1`, JWT de Supabase)
| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| PUT | `/status` | `{state, durationMin?, source}` | `Status` |
| GET | `/groups/:id/statuses` | — | `Status[]` |
| POST | `/suggest` | `{motion: Feature[], now}` | `{state, confidence, reason}` |
| PUT | `/schedule` | `ScheduleBlock[]` | `{count}` |
| GET | `/health` | — | `{ok:true}` |

`Status = {userId, displayName, state, until|null, source, updatedAt}`
Tiempo real: canal Supabase Realtime `statuses` filtrado por `group_id` (vista `group_statuses`).

## 5. Errores
`{error:{code, message}}` con `code` ∈ `invalid_input | unauthorized | forbidden | not_found | rate_limited | internal`.
