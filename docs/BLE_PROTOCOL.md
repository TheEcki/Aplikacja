# Protokół BLE — Amazfit Bip 6

Tylko elementy potwierdzone (Gadgetbridge/Amazfish + Bluetooth SIG).

| Element | UUID | Status |
|---|---|---|
| HR Measurement (notify) | 0x180D / 0x2A37 | CONFIRMED |
| Battery Level (read) | 0x180F / 0x2A19 | CONFIRMED |
| Device Info (read) | 0x180A (2A29/2A24/2A26/2A25/2A27) | CONFIRMED |
| Immediate Alert (write) | 0x1802 / 0x2A06 | CONFIRMED |
| ANS (write) | 0x1811 / 0x2A46 | CONFIRMED |
| Huami auth (write+notify) | 0xFEE1 / 00000009 | CONFIRMED |
| Huami config (write) | 0xFEE1 / 00000003 | CONFIRMED |
| Aktywność (read) | 0xFEE1 / 0007, 0006, 0001 | **UNVERIFIED** (format Zepp OS 5) |

Handshake auth: 01 08+key → 10 01 01 → 02 08 → 10 02 01+random →
03 08+AES-128-ECB → 10 03 01. Testowany wektorem FIPS-197.

Brakujące dane do pełnej synchronizacji: przechwycone pakiety aktywności z
realnego Bip 6 (lokalny log `Char XXXX = …`), weryfikacja na ≥2 sesjach,
potem parser w device/amazfit/Codec i podpięcie w fazie PARSE pipeline'u.
