# Algorytmy cardio i ograniczenia prawne/techniczne

## Co wiadomo publicznie o Google/Fitbit

- **Heart Points / Punkty kardio w Google Fit**: publicznie opisywany model to 1 punkt za minutę aktywności umiarkowanej i 2 punkty za minutę aktywności intensywnej. Cel jest powiązany z rekomendacjami AHA/WHO: ok. 150 min umiarkowanej albo 75 min intensywnej aktywności tygodniowo.
- **Fitbit Cardio Fitness Score**: Google/Fitbit opisuje go jako estymację VO₂max zależną m.in. od tętna spoczynkowego, wieku, płci, masy i profilu użytkownika. Dokładniejszy wynik jest wyliczany z relacji tempa oraz tętna podczas biegów GPS na płaskim terenie.

Dokładne implementacje Fitbit/Google, wagi modeli, filtry sygnału, progi jakości i mapowanie na zakresy są prywatne. Nie da się ich wiernie odtworzyć bez dokumentacji lub licencji.

## Co implementuje aplikacja

### Punkty kardio

Aplikacja liczy punkty w jawny sposób:

```text
punkt_kardio = 1 za minutę umiarkowaną
punkt_kardio = 2 za minutę intensywną
```

W MVP intensywność jest szacowana z procentu HRmax według publicznie opisanych zasad Google Fit:

```text
HRmax = wartość użytkownika albo 205,8 - (0,685 × wiek)
umiarkowana: 50%–69% HRmax = 1 punkt/min
intensywna: >= 70% HRmax = 2 punkty/min
```

Dla aktywności bez tętna Google Fit opisuje również reguły MET: 3,0–5,9 MET = 1 punkt/min, >= 6,0 MET = 2 punkty/min. W aplikacji przygotowano miejsce na tę warstwę, ale w MVP priorytetem jest tętno z zegarka.

### Cardio fitness / VO₂max

W MVP aplikacja podaje orientacyjny VO₂max z publicznego wzoru Uth-Sørensen-Overgaard-Pedersen:

```text
VO₂max = 15,3 × HRmax / HR_spoczynkowe
```

To nie jest medyczna diagnoza i nie jest prywatny algorytm Fitbit. W kolejnej iteracji można dodać lepszy model dla treningów GPS, np. oszacowanie kosztu tlenowego biegu z tempa i korektą na tętno, po zapisaniu płci, masy, wieku, wysokości oraz sesji GPS.

## Amazfit / Huami auth_key

MVP implementuje popularną procedurę dla starszych i wielu zgodnych urządzeń Amazfit/Huami:

1. połączenie GATT,
2. usługa `0000fee1-0000-1000-8000-00805f9b34fb`,
3. charakterystyka auth `00000009-0000-3512-2118-0009af100700`,
4. wysłanie klucza 16 B,
5. pobranie losowego wyzwania,
6. odpowiedź `AES-128-ECB(random, auth_key)`.

Niektóre nowsze zegarki Zepp OS mogą wymagać innego handshake, szyfrowanych ramek lub dodatkowych charakterystyk vendorowych.

## Następne kroki dla pełnej obsługi konkretnego zegarka

1. Podaj model zegarka i wersję firmware.
2. Podaj przykładowy klucz auth_key lub testowy zegarek do walidacji.
3. Dodać mapowanie vendorowych charakterystyk dla kroków, snu, historii tętna, SpO₂, stresu i treningów.
4. Dodać bazę Room/SQLite oraz eksport/import przez Health Connect.
5. Zbudować wariant release APK podpisany Twoim keystore.
