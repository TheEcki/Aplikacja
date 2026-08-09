# Testy

tools/run_tests.sh — 34 testy JVM bez emulatora/zegarka: Time, Hex, Dedupe,
Ranges, Auth (AES FIPS-197), Codec, GattGateway (FakeGattTransport + timeouty),
SyncPipeline (fake watch + fake store: happy path, DeviceUnavailable,
AuthenticationFailed, surowe pakiety, przyrostowość, HC skip).

Checklista urządzenia (fizyczny Bip 6): scan, connect, auth, battery,
initial/incremental sync, HR, reconnect, BT off/on, reboot, permission revoked.
