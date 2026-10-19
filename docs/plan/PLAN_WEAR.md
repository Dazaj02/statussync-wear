# StatusSync Wear — Plan del agente WEAR (reloj)

> **Carpeta propia:** `wear-app/` · **Contrato:** `CONTRATO.md` · **Planes hermanos:** `PLAN_BACKEND.md`, `PLAN_DATABASE.md`

## Contexto
App Wear OS (Kotlin + Jetpack Compose for Wear OS) que cambia tu estado de disponibilidad con un toque, lo sincroniza en tiempo real y lo devuelve a "Libre" con un temporizador. Capa IA: sugiere/aplica estado automático desde agenda y patrones de movimiento.

## Decisiones cerradas
| Tema | Decisión |
|---|---|
| Plataforma | Wear OS 4+, Kotlin, Compose for Wear OS, Material3 Wear |
| Arquitectura | Clean + MVVM: `ui/ · domain/ · data/ · di/` (puertos y adaptadores, como en FocusRead) |
| DI | Hilt · Red: supabase-kt / Ktor · Persistencia local: DataStore |
| Superficies | App, **Tile** (cambio rápido), **Complication** (estado actual en la esfera) |
| Modo desarrollo | `mock` hasta H2 (repositorio en memoria); real en W6 |
| Accesibilidad | Color + ícono + texto, contraste AA, haptics en cambios, fuente escalable |

## Fases
- **W0** Scaffold Gradle, tema (tokens de color del contrato), CI de lint/test.
- **W1** Pantalla principal: selector de estado en 1 toque (anillo de color + ícono), modo mock.
- **W2** Temporizador de "No Molestar": selección de duración, cuenta regresiva, vuelve a Libre, notificación háptica.
- **W3** Tile y Complication con el estado actual.
- **W4** Lista de compañeros del grupo (estados en vivo, mock).
- **W5** IA local: lectura de agenda y movimiento (Health Services) → `state_hint`; UI de confirmar/deshacer auto-cambio.
- **W6** Integración real: Supabase Auth + Realtime + API `/suggest`.
- **W7** Pulido (pantallas redondas/cuadradas, ahorro de batería), pruebas, APK firmado.

## Verificaciones por fase
`./gradlew lint testDebugUnitTest assembleDebug` verdes · captura en emulador Wear Round pequeño y grande.

## Formato de reporte
Qué se hizo · archivos tocados · verificaciones (salida real) · pendientes · riesgos.
