package com.example.semana4distribuidoraalimentos;
// Paquete declarado para esta Activity (se crea con el proyecto).
// ===== IMPORTS NECESARIOS =====
import android.os.Bundle;               // Manejo del ciclo de vida (onCreate)
import android.util.Log;                // Escribir mensajes en Logcat
import android.widget.Button;           // Botón para disparar los cálculos
import android.widget.EditText;         // Entradas de texto: monto, distancia, grados
import android.widget.TextView;         // Etiquetas y salida de resultados
import android.widget.Toast;            // Mensajes breves al usuario (validaciones)
import androidx.appcompat.app.AppCompatActivity; // Clase base de la pantalla (Activity)

public class MainActivity extends AppCompatActivity {
    // Clase principal de la app. Cada Activity representa una pantalla.

    private static final String TAG = "Semana4App";
    // TAG para filtrar fácilmente los mensajes en Logcat.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Método que se ejecuta al crear la Activity (cuando abre la pantalla).
        super.onCreate(savedInstanceState);              // Inicializa la Activity
        setContentView(R.layout.activity_main);          // Asocia el layout XML a esta pantalla

        // ===== Referencias a los controles de la vista (por id) =====
        EditText editMonto     = findViewById(R.id.editMonto);        // Campo: monto de compra
        EditText editDistancia = findViewById(R.id.editDistancia);    // Campo: distancia en km
        EditText editGrados    = findViewById(R.id.editGrados);       // Campo: grados (decimal)
        TextView textResultado = findViewById(R.id.textResultado);    // Salida: resumen de resultados
        Button btnCalcular     = findViewById(R.id.btnCalcular);      // Botón: ejecutar cálculos

        // ===== Listener del botón (qué ocurre al presionarlo) =====
        btnCalcular.setOnClickListener(v -> {
            // Antes de leer, limpiamos posibles errores previos en los EditText.
            editMonto.setError(null);
            editDistancia.setError(null);
            editGrados.setError(null);

            // ---- 1) Leemos y validamos Monto de compra (entero positivo) ----
            Integer monto = parseEnteroPositivo(editMonto, "Monto de compra");
            if (monto == null) return; // Si no es válido, abortamos (ya mostramos error)

            // ---- 2) Leemos y validamos Distancia (entero en rango 0..20) ----
            Integer distancia = parseEnteroEnRango(editDistancia, "Distancia (km)", 0, 20);
            if (distancia == null) return; // Si no es válida, abortamos

            // ---- 3) Leemos y validamos Grados (double; si vacío, tomamos 180.0 por defecto) ----
            Double grados = parseDoubleOpcional(editGrados, 180.0);
            // Si está vacío usa 180.0; si hay texto inválido, muestra error y retorna null
            if (grados == null) return;

            // ===== Cálculo de despacho según las reglas del caso =====
            int costoDespacho = calcularDespacho(monto, distancia);

            // ===== Conversión grados → radianes (requisito de la actividad) =====
            double radianes = aRadianes(grados);

            // ===== Armamos un texto de salida prolijo =====
            StringBuilder sb = new StringBuilder();
            sb.append("=== Distribuidora ===\n");
            sb.append("Compra: $").append(monto).append("\n");
            sb.append("Distancia: ").append(distancia).append(" km\n");
            sb.append("Despacho: $").append(costoDespacho).append("\n");
            sb.append("TOTAL: $").append(monto + costoDespacho).append("\n\n");
            sb.append("Conversión a radianes:\n");
            sb.append(String.format("%.4f° = %.6f rad", grados, radianes));

            String resumen = sb.toString(); // Convertimos el builder a String final

            // ===== Mostramos en pantalla =====
            textResultado.setText(resumen);

            // ===== Mostramos en consola estándar (System.out) =====
            System.out.println(resumen);

            // ===== Registramos en Logcat (nivel DEBUG) =====
            Log.d(TAG, "Compra=" + monto + ", km=" + distancia + ", despacho=" + costoDespacho);
            Log.d(TAG, "Grados=" + grados + " -> radianes=" + radianes);
        });
    }

    // ---------- Reglas del despacho (según el caso) ----------
    private int calcularDespacho(int monto, int km) {
        // Si la compra es >= $50.000 → despacho gratis
        if (monto >= 50000) {
            return 0;
        }
        // Si está entre $25.000 y $49.999 → $150 por km
        else if (monto >= 25000) {
            return 150 * km;
        }
        // Si es menor a $25.000 → $300 por km
        else {
            return 300 * km;
        }
    }

    // ---------- Conversión grados → radianes ----------
    private double aRadianes(double grados) {
        // Fórmula matemática: radianes = grados * PI / 180
        return grados * Math.PI / 180.0;
        // Alternativa equivalente: return Math.toRadians(grados);
    }

    // ---------- Helpers de validación / parseo ----------

    // Lee un entero > 0 desde un EditText. Si hay error, marca el campo y avisa con Toast.
    private Integer parseEnteroPositivo(EditText et, String nombreCampo) {
        String txt = et.getText().toString().trim();   // Tomamos el texto y quitamos espacios
        if (txt.isEmpty()) {                           // Vacío → error
            et.setError("Requerido");
            et.requestFocus();
            Toast.makeText(this, nombreCampo + " es requerido", Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            int valor = Integer.parseInt(txt);         // Intentamos convertir a entero
            if (valor <= 0) {                          // Debe ser > 0
                et.setError("Debe ser mayor a 0");
                et.requestFocus();
                Toast.makeText(this, nombreCampo + " debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return null;
            }
            return valor;                              // OK
        } catch (NumberFormatException e) {
            et.setError("Ingrese un número válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en " + nombreCampo, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Lee un entero dentro de un rango [min..max]. Marca error si no cumple.
    private Integer parseEnteroEnRango(EditText et, String nombreCampo, int min, int max) {
        String txt = et.getText().toString().trim();
        if (txt.isEmpty()) {
            et.setError("Requerido");
            et.requestFocus();
            Toast.makeText(this, nombreCampo + " es requerido", Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            int valor = Integer.parseInt(txt);
            if (valor < min || valor > max) {          // Verifica límites
                et.setError("Debe estar entre " + min + " y " + max);
                et.requestFocus();
                Toast.makeText(this, nombreCampo + " debe estar entre " + min + " y " + max, Toast.LENGTH_SHORT).show();
                return null;
            }
            return valor;
        } catch (NumberFormatException e) {
            et.setError("Ingrese un número válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en " + nombreCampo, Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    // Lee un double opcional. Si está vacío, usa un valor por defecto; si hay texto inválido, marca error.
    private Double parseDoubleOpcional(EditText et, double valorPorDefecto) {
        String txt = et.getText().toString().trim();
        if (txt.isEmpty()) {                            // Vacío → usar el valor por defecto
            return valorPorDefecto;
        }
        try {
            return Double.parseDouble(txt);             // Convierte a double
        } catch (NumberFormatException e) {
            et.setError("Ingrese un decimal válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en Grados", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
