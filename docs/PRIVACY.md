# Prywatność

- Dane lokalne; brak serwera, konta i telemetrii zdrowia (analityka no-op).
- Klucz parowania: Android Keystore AES-GCM, prefs vitaltrack_auth.xml
  wykluczone z backupu (res/xml/*rules.xml); nigdy w logach/eksporcie.
- MAC maskowany; surowe pakiety tylko debug (flaga verboseBle).
- User może: wyczyścić dane, zapomnieć zegarek+klucz, wyeksportować wszystko.
- Demo data tylko w buildzie --demo (weryfikacja w dex: klasa Demo nie
  istnieje w produkcji).
