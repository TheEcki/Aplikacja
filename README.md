# VitalTrack 1.0

Natywna aplikacja Android (Kotlin) do łączenia i synchronizacji **Amazfit Bip 6**
przez BLE — zaprojektowana od zera na podstawie analizy działania aplikacji do
zegarków (Gadgetbridge, Zepp) i wytycznych Material 3. Bez kont, bez serwerów —
dane i klucz parowania pozostają na telefonie.

```
minSdk 28 · targetSdk 34 (kompatybilny z POCO X7 Pro / HyperOS) · Kotlin 2.0.21
struktura: app / core / feature / device (22 moduły Gradle)
APK: dist/VitalTrack.apk (2,8 MB — systemowa czcionka, bez bundlowanych assetów)
```

## Co odróżnia VitalTrack (decyzje z researchu)

- **Układ „poranne podsumowanie”** jak w aplikacji Zepp: hero z powitaniem i
  statusem sync, pierścień celu kroków, karty metryk (tętno, sen, kalorie,
  dystans, ruch), wykres 7 dni i 24 h tętna [1](https://xiaomiforall.com/zepp-app-zepp-os-amazfit-xiaomi-2025-guide/)
- **Zarządzanie urządzeniem jak w Gadgetbridge**: skan → klucz → połącz →
  sync; log synchronizacji; alarmy i ustawienia zegarka — wszystko lokalnie,
  bez konta [1](https://github.com/Freeyourgadget/Gadgetbridge/blob/master/README.md)
- **Material 3**: pełne role kolorów (light/dark), tonalne powierzchnie,
  karty, chipy, przyciski, NavigationBar — implementacja offline 1:1 ze spec
  (docs/DESIGN.md); systemowa czcionka (Roboto) jak w natywnym Material
- **Minimalny start**: zero usług i harmonogramów przy uruchomieniu — baza
  i reszta tworzone leniwie; przy błędzie ekran błędu + `VitalTrack_crash.txt`
  w Pobranych

## Funkcje

- BLE: skanowanie (nazwa/FEE1/zapisany MAC), GATT z kolejką i timeoutami,
  autoryzacja Huami (FEE1/00000009, AES-128-ECB), tętno na żywo, bateria,
  info o urządzeniu, alarmy (0x0003), ustawienia zegarka, „znajdź zegarek”,
  test powiadomienia (ANS)
- SyncPipeline: CONNECT→AUTH→INFO→BATERIA→ZAKRES→DANE→PARSE→WALIDACJA→
  DEDUPE→BAZA→AGREGACJE→HC→KONIEC; przyrostowość i deduplikacja (SHA-256)
- Ekrany: Dziś, Aktywność (7/30 dni), Zdrowie (tętno: dzień/tydzień/miesiąc,
  strefy), Sen, Trendy (7/30/90/365), Urządzenia, Profil (profil, motyw,
  eksport CSV/JSON, Health Connect, prywatność), Onboarding
- Klucz parowania: Android Keystore (AES-GCM), wykluczony z backupu i eksportu
- Eksport: CSV/JSON bez klucza; trening GPS w usłudze pierwszoplanowej
- i18n: PL + EN (StringsPort + zasoby)

## Ograniczenia (uczciwie)

- **Historia aktywności Bip 6** (kroki/sen/SpO₂ z zegarka): protokół Zepp OS 5
  nieudokumentowany — surowe pakiety czytane i logowane, ale nie interpretowane
  (docs/BLE_PROTOCOL.md). Ekrany pokazują empty state, nigdy fikcyjne wartości.
- **Health Connect**: build offline bez androidx (brak Mavena w sandboxie) —
  status `Unavailable` z wyjaśnieniem; kod integracji docs/HEALTH_CONNECT.md.
- **Compose/androidx**: niemożliwe offline — M3 zaimplementowany 1:1 na
  Canvas/View; porty pozwalają podmienić na Compose w buildzie Gradle.

## Budowanie

```bash
tools/setup_toolchain.sh          # toolchain offline
python3 tools/build_apk.py        # → dist/VitalTrack.apk (targetSdk 34)
python3 tools/build_apk.py --demo # → dist/VitalTrack-demo.apk
tools/run_tests.sh                # 34 testy JVM
./gradlew assembleDebug           # środowisko z Mavenem
```

## Testy

34 testy JVM: wektor AES FIPS-197, handshake Huami, HR/bateria/alarmy, dedupe,
zakresy przyrostowe, kolejka GATT (timeouty), pełny pipeline synchronizacji.

## Dokumentacja

docs/ARCHITECTURE.md · docs/BLE_PROTOCOL.md · docs/HEALTH_CONNECT.md ·
docs/DESIGN.md · docs/PRIVACY.md · docs/TESTING.md · docs/ADDING_DEVICE.md

## Licencja

Kod własny. Wiedza o protokole Huami z publicznych projektów interoperacyjnych
(Gadgetbridge, Amazfish) — implementacja własna. Niezwiązane z Amazfit/Zepp.
