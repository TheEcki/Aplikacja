# Amazfit Health Bridge

Natywna aplikacja Android (APK) do łączenia zegarków Amazfit/Huami/Zepp przez BLE z użyciem 16-bajtowego klucza parowania (`auth_key`).

> Uwaga: projekt nie klonuje 1:1 chronionego wyglądu ani prywatnych algorytmów Google/Fitbit. Udostępnia podobny pulpit zdrowia i jawne, audytowalne obliczenia: punkty kardio według publicznych zaleceń AHA/WHO oraz estymację VO2max na podstawie znanych wzorów fizjologicznych.

## Funkcje MVP

- skanowanie BLE i filtrowanie urządzeń Amazfit/Huami/Zepp,
- zapis klucza parowania Amazfit w telefonie,
- uwierzytelnianie Huami/Amazfit przez usługę `FEE1` i charakterystykę auth,
- odczyt baterii i tętna przez standardowe usługi BLE, jeśli zegarek je udostępnia,
- pulpit: kroki, minuty ruchu, punkty kardio, tętno, bateria, sen, kalorie, treningi,
- transparentny kalkulator cardio fitness/VO2max.

## Budowanie APK

```bash
gradle assembleDebug
```

Gotowy APK: `app/build/outputs/apk/debug/app-debug.apk`.
W repo jest też GitHub Actions workflow `Build Android APK`, który publikuje APK jako artifact.

## Klucz Amazfit

Klucz ma format 32 znaków hex, np. `00112233445566778899aabbccddeeff`. Można go uzyskać z konta Zepp/Amazfit po wcześniejszym sparowaniu zegarka w oficjalnej aplikacji.
