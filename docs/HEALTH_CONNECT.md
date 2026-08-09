# Health Connect — integracja

Build offline nie zawiera androidx.health.connect (brak Mavena): bramka
UnavailableHc zwraca HcResult.Failed(HcError.Unavailable) i nigdy nie udaje
synchronizacji; SyncPipeline loguje „HC niedostępny — pominięto” i kończy się
sukcesem. Eksport CSV/JSON działa zawsze.

W środowisku z Mavenem: odkomentuj `implementation(libs.androidx.health.connect)`
w app/build.gradle.kts, dodaj uprawnienia health.*, zaimplementuj HcGateway
(eksport Steps/ExerciseSession z clientRecordId "vitaltrack:…", odrzucanie
własnych rekordów przy imporcie — ochrona przed pętlą).
