package pl.theecki.amazfithealth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * MVP aplikacji zdrowotnej dla Amazfit/Huami/Zepp.
 *
 * BLE auth używa znanego z urządzeń Huami wariantu auth_key: FEE1/00000009 + AES-128-ECB.
 * Niektóre nowe modele mogą wymagać innej procedury lub dodatkowych usług vendorowych.
 */
public class MainActivity extends Activity {
    private static final String PREF = "amazfit_health_bridge";
    private static final String KEY_AUTH = "auth_key_hex";
    private static final String KEY_MAC = "device_mac";

    private static final UUID UUID_HUAMI_SERVICE = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_HUAMI_AUTH = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    private static final UUID UUID_HR_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_HR_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<BluetoothDevice> foundDevices = new ArrayList<>();
    private SharedPreferences prefs;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private byte[] authKey;

    private LinearLayout cards;
    private TextView status;
    private EditText authInput;
    private EditText ageInput;
    private EditText restingHrInput;
    private EditText maxHrInput;

    private int heartRate = 0;
    private int battery = -1;
    private int moveMinutes = 0;
    private int vigorousMinutes = 0;
    private int steps = 0;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null || !hasBtConnect()) return;
            String name = safeName(device);
            String address = device.getAddress();
            if (!isLikelyAmazfit(name) && !address.equalsIgnoreCase(prefs.getString(KEY_MAC, ""))) return;
            for (BluetoothDevice d : foundDevices) if (d.getAddress().equals(address)) return;
            foundDevices.add(device);
            addDeviceRow(device, result.getRssi());
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int statusCode, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setStatus("Połączono. Wyszukuję usługi…");
                if (hasBtConnect()) g.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                setStatus("Rozłączono z zegarkiem.");
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt g, int statusCode) {
            setStatus("Usługi wykryte. Uruchamiam autoryzację Amazfit…");
            BluetoothGattCharacteristic auth = getChar(g, UUID_HUAMI_SERVICE, UUID_HUAMI_AUTH);
            if (auth != null) {
                enableNotify(g, auth);
                write(auth, prepend(new byte[]{0x01, 0x08}, authKey));
                main.postDelayed(() -> write(auth, new byte[]{0x02, 0x08}), 450);
            } else {
                setStatus("Brak usługi auth FEE1. Próbuję standardowe odczyty BLE bez autoryzacji.");
                startHealthReads(g);
            }
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            byte[] v = c.getValue();
            if (UUID_HUAMI_AUTH.equals(c.getUuid())) handleAuthPacket(g, c, v);
            if (UUID_HR_MEASUREMENT.equals(c.getUuid())) {
                heartRate = parseHeartRate(v);
                updateActivityFromHr();
                refreshCards();
            }
        }

        @Override public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, int statusCode) {
            if (UUID_BATTERY_LEVEL.equals(c.getUuid()) && c.getValue() != null && c.getValue().length > 0) {
                battery = c.getValue()[0] & 0xff;
                refreshCards();
            }
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        requestNeededPermissions();
        buildUi();
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        scanner = adapter == null ? null : adapter.getBluetoothLeScanner();
        refreshCards();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(32));
        root.setBackgroundColor(Color.rgb(248, 250, 253));
        scroll.addView(root);

        TextView title = text("Amazfit Health", 30, true, Color.rgb(32, 33, 36));
        root.addView(title);
        root.addView(text("Pulpit zdrowia inspirowany prostotą Google Fit — bez klonowania prywatnych elementów i z jawnie opisanymi obliczeniami.", 14, false, Color.rgb(95, 99, 104)));

        status = text("Wklej klucz parowania i wyszukaj zegarek.", 14, false, Color.rgb(26, 115, 232));
        status.setPadding(0, dp(14), 0, dp(10));
        root.addView(status);

        authInput = new EditText(this);
        authInput.setHint("Klucz Amazfit: 32 znaki HEX");
        authInput.setSingleLine(true);
        authInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        authInput.setText(prefs.getString(KEY_AUTH, ""));
        root.addView(authInput, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = row();
        Button save = button("Zapisz klucz");
        save.setOnClickListener(v -> saveAuthKey());
        Button scan = button("Skanuj BLE");
        scan.setOnClickListener(v -> startScan());
        actions.addView(save, weightParams());
        actions.addView(scan, weightParams());
        root.addView(actions);

        cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        root.addView(cards);

        TextView calcTitle = text("Kalkulator cardio fitness", 22, true, Color.rgb(32, 33, 36));
        calcTitle.setPadding(0, dp(18), 0, dp(4));
        root.addView(calcTitle);
        ageInput = smallInput("Wiek", "35");
        restingHrInput = smallInput("Tętno spoczynkowe", "60");
        maxHrInput = smallInput("Tętno maksymalne (puste = 220-wiek)", "");
        root.addView(ageInput); root.addView(restingHrInput); root.addView(maxHrInput);
        Button calc = button("Przelicz VO₂max");
        calc.setOnClickListener(v -> refreshCards());
        root.addView(calc);

        root.addView(infoBlock());
        setContentView(scroll);
    }

    private View infoBlock() {
        TextView t = text("Algorytmy w tej wersji:\n" +
                "• Punkty kardio: 1 pkt/min aktywności umiarkowanej, 2 pkt/min intensywnej.\n" +
                "• Intensywność z tętna zgodnie z publiczną pomocą Google Fit: umiarkowana 50–69% HRmax, intensywna od 70% HRmax.\n" +
                "• HRmax zgodnie z publiczną pomocą Google Fit: 205,8 − 0,685 × wiek.\n" +
                "• Cardio fitness: estymacja VO₂max = 15,3 × HRmax / HR spoczynkowe. To nie jest prywatny wynik Fitbit/Google, tylko jawny wzór orientacyjny.\n" +
                "• Kroki/sen/kalorie są przygotowane jako moduły pulpitu; pełna synchronizacja wymaga obsługi konkretnych vendorowych charakterystyk danego modelu Amazfit.", 13, false, Color.rgb(95, 99, 104));
        t.setPadding(0, dp(14), 0, 0);
        return t;
    }

    private void refreshCards() {
        main.post(() -> {
            cards.removeAllViews();
            double vo2 = estimateVo2();
            int heartPoints = moveMinutes + vigorousMinutes;
            cards.addView(card("Kroki", String.valueOf(steps), "Moduł gotowy; vendorowa synchronizacja Amazfit wymaga mapy dla modelu."));
            cards.addView(card("Punkty kardio", String.valueOf(heartPoints), moveMinutes + " min umiarkowane + " + vigorousMinutes + " dodatkowych pkt za intensywne."));
            cards.addView(card("Tętno", heartRate > 0 ? heartRate + " bpm" : "—", heartRate > 0 ? "Odczyt z BLE Heart Rate Service." : "Połącz zegarek i włącz pomiar tętna."));
            cards.addView(card("Cardio fitness", String.format(Locale.US, "%.1f", vo2), classifyVo2(vo2) + " · szacunek VO₂max ml/kg/min"));
            cards.addView(card("Bateria zegarka", battery >= 0 ? battery + "%" : "—", "Standardowa usługa Battery Service, jeśli dostępna."));
            cards.addView(card("Sen", "—", "Widok przygotowany; wymaga historii snu z vendorowych danych Amazfit."));
            cards.addView(card("Kalorie", String.valueOf(estimateCalories()), "Szacunek z aktywności i tętna; nie do decyzji medycznych."));
            cards.addView(card("Treningi", "0", "W kolejnej iteracji: import sesji z GPS/Zepp lub zapis własnych treningów."));
        });
    }

    private void startScan() {
        if (!saveAuthKey()) return;
        if (scanner == null) { toast("Bluetooth LE niedostępny"); return; }
        if (!hasBtScan()) { requestNeededPermissions(); return; }
        foundDevices.clear();
        setStatus("Skanuję urządzenia Amazfit/Huami/Zepp przez 12 s…");
        scanner.startScan(scanCallback);
        main.postDelayed(() -> {
            if (hasBtScan()) scanner.stopScan(scanCallback);
            if (foundDevices.isEmpty()) setStatus("Nie znaleziono zegarka. Upewnij się, że Bluetooth jest włączony i zegarek jest blisko.");
        }, 12_000);
    }

    private void addDeviceRow(BluetoothDevice device, int rssi) {
        main.post(() -> {
            String label = safeName(device) + "\n" + device.getAddress() + " · RSSI " + rssi;
            Button b = button(label);
            b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v -> connect(device));
            cards.addView(b, 0);
            setStatus("Znaleziono zegarek. Dotknij, aby połączyć.");
        });
    }

    private void connect(BluetoothDevice device) {
        if (!hasBtConnect()) { requestNeededPermissions(); return; }
        prefs.edit().putString(KEY_MAC, device.getAddress()).apply();
        setStatus("Łączę z " + safeName(device) + "…");
        if (gatt != null) gatt.close();
        gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    private void handleAuthPacket(BluetoothGatt g, BluetoothGattCharacteristic auth, byte[] v) {
        if (v == null || v.length < 3) return;
        if (v[0] == 0x10 && v[1] == 0x01 && v[2] == 0x01) {
            setStatus("Klucz przyjęty. Pobieram wyzwanie…");
            write(auth, new byte[]{0x02, 0x08});
        } else if (v[0] == 0x10 && v[1] == 0x02 && v[2] == 0x01 && v.length >= 19) {
            byte[] random = new byte[16];
            System.arraycopy(v, 3, random, 0, 16);
            try {
                write(auth, prepend(new byte[]{0x03, 0x08}, aes(random, authKey)));
                setStatus("Wysłano odpowiedź AES. Czekam na potwierdzenie…");
            } catch (GeneralSecurityException e) {
                setStatus("Błąd AES: " + e.getMessage());
            }
        } else if (v[0] == 0x10 && v[1] == 0x03 && v[2] == 0x01) {
            setStatus("Autoryzacja Amazfit OK. Uruchamiam pomiary.");
            startHealthReads(g);
        } else if (v[0] == 0x10 && v.length >= 3) {
            setStatus("Autoryzacja odrzucona przez zegarek. Sprawdź klucz parowania/model.");
        }
    }

    private void startHealthReads(BluetoothGatt g) {
        BluetoothGattCharacteristic hr = getChar(g, UUID_HR_SERVICE, UUID_HR_MEASUREMENT);
        if (hr != null) enableNotify(g, hr);
        BluetoothGattCharacteristic bat = getChar(g, UUID_BATTERY_SERVICE, UUID_BATTERY_LEVEL);
        if (bat != null && hasBtConnect()) g.readCharacteristic(bat);
        refreshCards();
    }

    private BluetoothGattCharacteristic getChar(BluetoothGatt g, UUID service, UUID characteristic) {
        BluetoothGattService s = g.getService(service);
        return s == null ? null : s.getCharacteristic(characteristic);
    }

    private void enableNotify(BluetoothGatt g, BluetoothGattCharacteristic c) {
        if (!hasBtConnect()) return;
        g.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor d = c.getDescriptor(UUID_CCCD);
        if (d != null) {
            d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            g.writeDescriptor(d);
        }
    }

    private void write(BluetoothGattCharacteristic c, byte[] value) {
        if (gatt == null || c == null || !hasBtConnect()) return;
        c.setValue(value);
        c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        gatt.writeCharacteristic(c);
    }

    private boolean saveAuthKey() {
        String hex = authInput.getText().toString().trim().replace("0x", "").replace(" ", "");
        if (!hex.matches("(?i)[0-9a-f]{32}")) {
            toast("Klucz musi mieć 32 znaki HEX (16 bajtów).");
            return false;
        }
        authKey = hexToBytes(hex);
        prefs.edit().putString(KEY_AUTH, hex.toLowerCase(Locale.ROOT)).apply();
        setStatus("Klucz zapisany lokalnie. Nie wysyłam go na żaden serwer.");
        return true;
    }

    private void updateActivityFromHr() {
        int max = getInt(maxHrInput, 0);
        int age = getInt(ageInput, 35);
        if (max <= 0) max = estimatedGoogleFitMaxHr(age);
        if (heartRate >= 0.70 * max) { moveMinutes++; vigorousMinutes++; }
        else if (heartRate >= 0.50 * max) moveMinutes++;
    }

    private double estimateVo2() {
        int age = getInt(ageInput, 35);
        int max = getInt(maxHrInput, 0);
        int rhr = getInt(restingHrInput, 60);
        if (max <= 0) max = estimatedGoogleFitMaxHr(age);
        if (rhr < 35) rhr = 35;
        return 15.3 * max / rhr;
    }

    private int estimatedGoogleFitMaxHr(int age) {
        return (int) Math.round(205.8 - (0.685 * age));
    }

    private String classifyVo2(double v) {
        if (v < 28) return "niski";
        if (v < 36) return "przeciętny";
        if (v < 46) return "dobry";
        return "bardzo dobry";
    }

    private int estimateCalories() {
        return Math.max(0, (moveMinutes * 5) + (vigorousMinutes * 4));
    }

    private int parseHeartRate(byte[] v) {
        if (v == null || v.length < 2) return 0;
        boolean sixteenBit = (v[0] & 0x01) == 1;
        return sixteenBit && v.length >= 3 ? ((v[2] & 0xff) << 8) | (v[1] & 0xff) : (v[1] & 0xff);
    }

    private static byte[] aes(byte[] block, byte[] key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(block);
    }

    private static byte[] prepend(byte[] prefix, byte[] data) {
        byte[] out = new byte[prefix.length + data.length];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        System.arraycopy(data, 0, out, prefix.length, data.length);
        return out;
    }

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    private boolean isLikelyAmazfit(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("amazfit") || n.contains("huami") || n.contains("zepp") || n.contains("gtr") || n.contains("gts") || n.contains("bip") || n.contains("band");
    }

    private String safeName(BluetoothDevice d) {
        try {
            if (!hasBtConnect()) return "Urządzenie BLE";
            String name = d.getName();
            return name == null || name.trim().isEmpty() ? "Amazfit/Zepp" : name;
        } catch (SecurityException e) { return "Urządzenie BLE"; }
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION}, 7);
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS}, 7);
        }
    }

    private boolean hasBtScan() {
        return Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBtConnect() {
        return Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout.LayoutParams weightParams() { return new LinearLayout.LayoutParams(0, dp(48), 1); }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private EditText smallInput(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    private View card(String title, String value, String sub) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(14), dp(18), dp(14));
        c.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(10), 0, 0);
        c.setLayoutParams(lp);
        c.addView(text(title, 14, true, Color.rgb(95, 99, 104)));
        c.addView(text(value, 28, true, Color.rgb(32, 33, 36)));
        c.addView(text(sub, 13, false, Color.rgb(95, 99, 104)));
        return c;
    }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setLineSpacing(0, 1.08f);
        return t;
    }

    private int getInt(EditText e, int def) {
        try { return Integer.parseInt(e.getText().toString().trim()); } catch (Exception ignored) { return def; }
    }

    private void setStatus(String s) { main.post(() -> status.setText(s)); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
