# Architektura

UI → porty (core/common: Hub) → Db/SyncEngine → adapter Bip 6 → GattGateway
(kolejka + timeouty) → AndroidGatt. Czysty rdzeń JVM (parsery, auth, pipeline,
GATT) testowany bez Androida. DI: ręczny ServiceLocator (Hub). Baza SQLite —
źródło prawdy dla UI; migracje bez destrukcji. Porty pozwalają podmienić
androidx (Compose, Room, WorkManager, Health Connect) w buildzie Gradle.
