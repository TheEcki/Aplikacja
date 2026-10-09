# BTSearch – aplikacja Android (natywna)

Natywna aplikacja Android (Kotlin, Jetpack Compose, MapLibre). Nie jest nakładką WWW:
mapa jest renderowana natywnie, a dane pobierane z publicznego REST API btsearch.

## Funkcje (wersja 0.1.0)

- **Mapa** – stacje bazowe w widoku mapy (MapLibre, kafelki OpenFreeMap). Dane ładowane dla aktualnie widocznego obszaru. Dotknięcie punktu otwiera listę stacji w lokalizacji.
- **Szukaj** – wyszukiwanie po kodzie stacji, mieście lub adresie.
- **Szczegóły stacji** – operator, adres, województwo, komórki (RAT, pasmo), pozwolenia UKE, nawigacja do stacji.
- **O aplikacji** – informacje, licencja, link do kodu źródłowego.

Planowane (poza zakresem 0.1.0): warstwa radiolinii, filtry, listy użytkownika, logowanie.

## Adres API

Domyślnie: `https://btsearch.pl/api/v1`. Można go zmienić bez edycji kodu:

```bash
gradle :app:assembleDebug -PBTSEARCH_API_URL=https://twoj-serwer/api/v1
```

Serwer btsearch (Fastify + PostgreSQL/PostGIS) uruchamia się zgodnie z instrukcją w głównym `README.md`.

## Budowanie APK

### Opcja 1 – GitHub Actions (bez instalowania niczego)

Workflow `android/ci/android-apk.workflow.yml` buduje APK (aby działał, przenieś go do `.github/workflows/android-apk.yml`) przy każdym pushu zmieniającym `android/`.
Plik znajdziesz w zakładce **Actions → Android APK → Artifacts** (`btsearch-debug-apk`).
Ręcznie: **Actions → Android APK → Run workflow** i podaj adres API.

### Opcja 2 – Android Studio

1. Otwórz katalog `android/` w Android Studio (Ladybug lub nowszy).
2. Wymagany JDK 17 i SDK Platform 35.
3. Android Studio wygeneruje Gradle Wrapper; następnie **Build → Build APK(s)**.

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Wymagania

- Android 7.0 (API 24) lub nowszy.
- Połączenie z internetem.

## Licencja

GPL-3.0, zgodnie z repozytorium btsearch.
