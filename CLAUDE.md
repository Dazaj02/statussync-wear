# StatusSync Wear — reglas globales para agentes
Planes: docs/plan/PLAN_WEAR.md · PLAN_BACKEND.md · PLAN_DATABASE.md
Propiedad: reloj → wear-app/ · backend → back-wear/ · base de datos → supabase/
- Nunca edites fuera de tu carpeta. Nunca edites los planes.
- El contrato compartido (docs/plan/CONTRATO.md) está congelado. Si necesitas cambiarlo, detente y repórtalo.
- Ningún secreto en código ni en git. Solo en .env / local.properties locales (ignorados) o en el hosting.
- La service_role de Supabase vive únicamente en el .env del backend; nunca en el reloj.
- En cada fase: plan corto → esperar aprobación → implementar → verificaciones → reporte.
- Commits: `git add <tu-carpeta>` únicamente. Mensajes: `wear(W1): ...`, `api(B2): ...`, `db(D3): ...`.
- Textos de UI y mensajes al usuario en español; código e identificadores en inglés.
- UI de reloj: un toque = una acción, objetivos táctiles ≥ 48dp, nunca depender solo del color (siempre ícono + texto), pantallas redondas y pequeñas primero.
