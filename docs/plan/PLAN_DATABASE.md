# StatusSync Wear — Plan del agente DATABASE

> **Carpeta propia:** `supabase/` · **Contrato:** `CONTRATO.md` §3

## Estructura (como `Proyecto mobil/supabase/sql`)
`00_schemas · 01_tables · 02_indexes · 03_functions · 04_grants · 05_rls · 06_views · 07_seed_demo · 90_dev_reset · 99_verify · _all.sql · README.md`

## Decisiones
- RLS activa en todas las tablas. Un usuario solo escribe su propia fila de `statuses`.
- Un miembro lee los `statuses` de los grupos a los que pertenece (vista `group_statuses`).
- `statuses` publicada en Realtime (`supabase_realtime`).
- Función `expire_statuses()` + pg_cron cada minuto: `state='free', source='timer'` donde `until < now()`.
- `motion_features` solo agregados; retención 14 días.

## Fases
- **D0** Esquemas y tablas · **D1** índices, funciones, grants · **D2** RLS + vistas · **D3** seed demo + verify · **D4** Realtime y pg_cron.
