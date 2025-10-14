package com.example.semana4distribuidoraalimentos;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class TemperaturaActivity extends AppCompatActivity {

    private static final String TAG = "TemperaturaActivity";
    private static final int REQ_POST_NOTIF = 500;
    private static final String CH_ID = "temp_alerts";

    // Rango de simulación: -20 a +10
    private static final int MIN_C = -20;
    private static final int MAX_C = 10;

    // Umbrales cadena de frío
    private static final float MIN_OK = -18f;
    private static final float MAX_OK = 8f;

    private TextView tvTempActual, tvEstado;
    private SeekBar seekTemp;
    private Button btnProbar;

    // ===== Firebase (leyendo ÚLTIMO valor de /iot/temperatures/{USER_ID}) =====
    private DatabaseReference iotNode;   // /iot/temperatures/{USER_ID}
    private Query tempQuery;             // orderByKey().limitToLast(1)
    private ValueEventListener tempListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperatura);

        tvTempActual = findViewById(R.id.tvTempValor);
        tvEstado     = findViewById(R.id.tvEstado);
        seekTemp     = findViewById(R.id.seekSimulador);
        btnProbar    = findViewById(R.id.btnProbarAlarma);

        if (tvTempActual == null || tvEstado == null || seekTemp == null || btnProbar == null) {
            throw new IllegalStateException("IDs del layout no coinciden con activity_temperatura.xml");
        }

        // Inicial SeekBar (vista previa local)
        seekTemp.setMax(MAX_C - MIN_C);
        seekTemp.setProgress(-MIN_C); // arranca en ~0 °C
        seekTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float t = MIN_C + progress;
                renderTemp(t); // vista previa local (NO escribe en Firebase)
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        renderTemp(MIN_C + seekTemp.getProgress());

        // Canal de notificaciones (Android 8+)
        ensureChannel();

        // ====== Firebase: escuchar el último dato que manda Python ======

        final String USER_ID = "demo"; //

        iotNode = FirebaseDatabase.getInstance()
                .getReference("iot")
                .child("temperatures")
                .child(USER_ID);

        tempQuery = iotNode.orderByKey().limitToLast(1);
        attachFirebaseListener();

        // Botón: probar alarma SOLO local (no pisa lo que envía Python)
        btnProbar.setOnClickListener(v -> {
            float temp = MIN_C + seekTemp.getProgress();
            triggerAlarm(temp);
        });
    }

    private void attachFirebaseListener() {
        if (tempListener != null) return;

        tempListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // snapshot contendrá 0 o 1 hijos (el último timestamp)
                for (DataSnapshot child : snapshot.getChildren()) {
                    // Estructura: { value: double, timestamp: long, unit: "°C" }
                    Double val = child.child("value").getValue(Double.class);
                    if (val == null) {
                        Log.w(TAG, "Nodo sin 'value' en iot/temperatures");
                        continue;
                    }
                    float t = val.floatValue();
                    Log.d(TAG, "Temperatura Firebase (último iot): " + t);
                    renderTemp(t);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase cancelado: " + error.getMessage());
                Toast.makeText(TemperaturaActivity.this,
                        "Error leyendo temperatura: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };
        tempQuery.addValueEventListener(tempListener);
    }

    private void detachFirebaseListener() {
        if (tempListener != null && tempQuery != null) {
            tempQuery.removeEventListener(tempListener);
            tempListener = null;
        }
    }

    private void renderTemp(float tempC) {
        boolean fueraRango = (tempC < MIN_OK || tempC > MAX_OK);

        // (Si quieres usar la mini-animación, descomenta y agrega el método que te pasé)
        // animateTemperatureChange(tempC, fueraRango);

        tvTempActual.setText(String.format("%.0f °C", tempC));
        tvEstado.setText(fueraRango
                ? "¡Fuera de rango de cadena de frío!"
                : "En rango de cadena de frío");
        tvEstado.setTextColor(ContextCompat.getColor(
                this, fueraRango ? android.R.color.holo_red_dark : android.R.color.holo_green_dark
        ));

        // Si prefieres alertar también al llegar un push de Firebase, deja esto.
        // Si te genera “spam” de notificaciones, coméntalo.
        if (fueraRango) {
            triggerAlarm(tempC);
        }
    }

    private void triggerAlarm(float tempC) {
        // Android 13+: permiso para mostrar notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIF);
                Toast.makeText(this, "Concede permiso de notificaciones y vuelve a probar.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        boolean fueraRango = (tempC < MIN_OK || tempC > MAX_OK);

        new AlertDialog.Builder(this)
                .setTitle(fueraRango ? "Alerta de temperatura" : "Estado de temperatura")
                .setMessage("Temperatura actual: " + tempC + " °C\n" +
                        (fueraRango ? "Fuera de rango de cadena de frío." : "En rango."))
                .setPositiveButton("OK", null)
                .show();

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(fueraRango ? "ALERTA: Temperatura" : "Temperatura")
                .setContentText("Actual: " + tempC + " °C")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(1001, nb.build());
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_ID, "Alertas de temperatura", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Notificaciones cuando la temperatura sale de rango");
                nm.createNotificationChannel(ch);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachFirebaseListener();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIF) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de notificaciones otorgado. Vuelve a probar.",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Sin permiso de notificaciones, solo verás el diálogo in-app.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
