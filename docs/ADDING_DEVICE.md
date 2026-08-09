# Dodawanie urządzenia

1. Adapter w device/<model> (WatchAdapter) z deklaracją caps — tylko
   potwierdzone; 2. WatchFactory (supports/create); 3. własny auth jeśli inny
   niż Huami; 4. parsery (czysty JVM) + podpięcie w fazie PARSE pipeline'u;
   5. fixtures i testy; 6. rejestracja w WatchRegistry (App.kt); 7. docs.
   Nie zakładaj identycznych UUID dla modeli z tej samej serii.
