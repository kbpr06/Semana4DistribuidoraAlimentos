package com.example.semana4distribuidoraalimentos; // Paquete del proyecto

// ===== IMPORTS =====
import android.os.Bundle;                        // Ciclo de vida (onCreate)
import android.widget.SeekBar;                   // Control deslizante para simular temperatura
import android.widget.TextView;                  // Para mostrar temperatura y estado
import android.widget.Button;                    // Botón "Probar alarma"
import android.widget.Toast;                     // Mensajes cortos (feedback al usuario)
import androidx.appcompat.app.AppCompatActivity; // Activity base de Android
import androidx.cardview.widget.CardView;        // Tarjetas visuales (coherente con tu diseño)

// Clase de la nueva pantalla de "Monitoreo de Temperatura"
public class TemperaturaActivity extends AppCompatActivity {

    // ===== Constantes de simulación/negocio =====
    private static final int MIN_C = -20;    // Temperatura mínima del SeekBar (simulación)
    private static final int MAX_C = 10;     // Temperatura máxima del SeekBar (simulación)
    private static final int DEFAULT_C = -15;// Valor inicial (ej. cámara de frío estable)
    private static final int UMBRAL_ALERTA = -10; // Umbral de seguridad: por sobre esto, alerta

    // ===== Referencias a vistas =====
    private TextView tvTempActual;   // Muestra la temperatura actual (formateada con °C)
    private TextView tvEstado;       // Muestra "En rango" o "ALERTA"
    private SeekBar seekTemp;        // Control para mover la temperatura simulada

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método que se ejecuta al crear la Activity
        super.onCreate(savedInstanceState);              // Inicializa la Activity base
        setContentView(R.layout.activity_temperatura);   // Vincula con el layout XML de esta pantalla

        // 1) Conectar vistas por id (definidas en el XML)
        tvTempActual = findViewById(R.id.tvTempActual);  // Texto grande con °C actuales
        tvEstado     = findViewById(R.id.tvEstado);      // Estado: "En rango" / "ALERTA"
        seekTemp     = findViewById(R.id.seekTemp);      // SeekBar que simula la temperatura
        Button btnProbarAlarma = findViewById(R.id.btnProbarAlarma); // Botón para forzar validación

        // 2) Configurar el SeekBar para el rango [-20, +10]
        //    SeekBar sólo maneja [0..max], así que desplazamos valores:
        //    progressReal = MIN_C + progress
        seekTemp.setMax(MAX_C - MIN_C);                  // Ej.: 30 pasos (de -20 a +10 => 31 valores)
        seekTemp.setProgress(DEFAULT_C - MIN_C);         // Colocamos el valor inicial (-15°C)

        // 3) Mostrar la temperatura inicial y evaluar estado
        actualizarUI( DEFAULT_C );                       // Pinta textos y estado con el default

        // 4) Listener del SeekBar: cada vez que el usuario lo mueva, actualizamos UI
        seekTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int temp = MIN_C + progress;            // Convertimos progreso a temperatura real
                actualizarUI(temp);                      // Actualiza °C y estado en pantalla
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { /* No usado */ }
            @Override public void onStopTrackingTouch(SeekBar seekBar)  { /* No usado */ }
        });

        // 5) Botón "Probar alarma": valida el valor actual y muestra un Toast
        btnProbarAlarma.setOnClickListener(v -> {
            int temp = MIN_C + seekTemp.getProgress();   // Tomamos la temperatura actual simulada
            if (temp > UMBRAL_ALERTA) {                  // Si supera el umbral (ej. -10°C) => alerta
                Toast.makeText(this,
                        "⚠️ Alerta: temperatura fuera de rango (" + temp + "°C)",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        "✅ En rango seguro (" + temp + "°C)",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método auxiliar para actualizar textos y estado visual según la temperatura
    private void actualizarUI(int tempC) {
        // 1) Muestra la temperatura formateada
        tvTempActual.setText(tempC + " °C");

        // 2) Evalúa si está en rango o en alerta
        if (tempC > UMBRAL_ALERTA) {
            // Fuera de rango (por encima de -10°C)
            tvEstado.setText("ALERTA: fuera de rango");
            tvEstado.setTextColor(getColor(android.R.color.holo_red_dark)); // Rojo (alerta)
        } else {
            // Dentro del rango
            tvEstado.setText("En rango de cadena de frío");
            tvEstado.setTextColor(getColor(android.R.color.holo_green_dark)); // Verde (OK)
        }
    }
}
