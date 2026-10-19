# wear-app — StatusSync Wear (Wear OS)

Kotlin + Compose for Wear OS (Material3). Plan: `../docs/plan/PLAN_WEAR.md`.

## Estructura
```
app/src/main/java/com/statussync/wear/
  domain/    modelos y reglas puras (AvailabilityState)
  data/      repositorios (mock → real en W6)
  di/        inyección (Hilt llega en W1)
  ui/theme/  tokens y tema
```

## Requisitos
Android Studio, JDK 17, Android SDK 36, emulador "Wear OS Large/Small Round".

## Comandos
```
gradle wrapper --gradle-version 8.14.3   # una sola vez (o deja que Android Studio lo genere)
./gradlew lint testDebugUnitTest assembleDebug
```
