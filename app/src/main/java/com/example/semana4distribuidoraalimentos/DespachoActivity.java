package com.example.semana4distribuidoraalimentos;  // Paquete de la app

// ===== IMPORTS ANDROID =====
import android.os.Bundle;                          // Ciclo de vida (onCreate)
import android.text.TextUtils;                     // Validar campo vacío
import android.widget.Button;                      // Botones
import android.widget.EditText;                    // Campo ID pedido
import android.widget.TextView;                    // Labels
import android.widget.Toast;                       // Mensajes cortos

import androidx.appcompat.app.AppCompatActivity;   // Base Activity

// ===== IMPORTS FIREBASE (opcional para guardar/leer) =====
import com.google.firebase.auth.FirebaseAuth;               // Para UID del usuario
import com.google.firebase.database.DatabaseReference;      // Referencia a RTDB
import com.google.firebase.database.FirebaseDatabase;       // Instancia RTDB

import java.util.HashMap;                         // Para guardar datos simples
import java.util.Locale;                          // Formateo local
import java.util.Map;                             // Map genérico

public class DespachoActivity extends AppCompatActivity {

    // ----- Views principales -----
    private EditText etPedidoId;          // Input: ID del pedido a consultar/guardar
    private TextView tvPedidoMostrado;    // Muestra el ID activo
    private TextView tvEstadoActual;      // Texto con el estado actual

    // Bolitas (puntos) y etiquetas de los 4 pasos
    private TextView dot1, dot2, dot3, dot4;   // ● coloreados
    private TextView txt1, txt2, txt3, txt4;   // Etiquetas "Recibido"...

    // Botones de acción
    private Button btnBuscar, btnSimular, btnGuardar, btnLeer;

    // ----- Modelo de estados -----
    private final String[] ESTADOS = new String[]{
            "Recibido", "Preparando", "En camino", "Entregado"
    };
    private int estadoIndex = 0;          // 0..3 (posición en ESTADOS)

    // ----- Firebase -----
    private FirebaseAuth auth;            // Para obtener UID (si estás logueado)
    private DatabaseReference trackRef;   // /tracking/{uid}/{pedidoId}

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método al crear la pantalla
        super.onCreate(savedInstanceState);              // Inicializa Activity base
        setContentView(R.layout.activity_despacho);      // Vincula el XML de arriba

        // 1) Referencias UI
        etPedidoId       = findViewById(R.id.etPedidoId);         // Caja para escribir ID
        tvPedidoMostrado = findViewById(R.id.tvPedidoMostrado);   // "Pedido: XYZ"
        tvEstadoActual   = findViewById(R.id.tvEstadoActual);     // "Estado actual: ..."

        dot1 = findViewById(R.id.dot1);  // Bolita 1
        dot2 = findViewById(R.id.dot2);  // Bolita 2
        dot3 = findViewById(R.id.dot3);  // Bolita 3
        dot4 = findViewById(R.id.dot4);  // Bolita 4

        txt1 = findViewById(R.id.txt1);  // Etiqueta 1
        txt2 = findViewById(R.id.txt2);  // Etiqueta 2
        txt3 = findViewById(R.id.txt3);  // Etiqueta 3
        txt4 = findViewById(R.id.txt4);  // Etiqueta 4

        btnBuscar   = findViewById(R.id.btnBuscar);            // Botón Buscar pedido
        btnSimular  = findViewById(R.id.btnSimularSiguiente);  // Botón Siguiente estado
        btnGuardar  = findViewById(R.id.btnGuardarFirebase);   // Guardar en Firebase
        btnLeer     = findViewById(R.id.btnLeerFirebase);      // Leer de Firebase

        // 2) Firebase (opcional, pero ya lo tienes en el proyecto)
        auth = FirebaseAuth.getInstance();                              // Sesión actual
        trackRef = FirebaseDatabase.getInstance().getReference("tracking"); // Nodo base

        // 3) Estado inicial del timeline (todo apagado)
        aplicarEstadoVisual(estadoIndex);   // Pinta según estadoIndex (inicia en 0: "Recibido")

        // 4) Buscar por ID escrito (si existe en Firebase, lo cargamos)
        btnBuscar.setOnClickListener(v -> {
            String id = etPedidoId.getText().toString().trim(); // Leemos ID
            if (TextUtils.isEmpty(id)) {                         // Validación simple
                etPedidoId.setError("Ingresa un ID de pedido");
                etPedidoId.requestFocus();
                return;
            }
            tvPedidoMostrado.setText("Pedido: " + id);           // Muestra ID en header
            Toast.makeText(this, "Listo. Puedes leer/guardar el estado", Toast.LENGTH_SHORT).show();
        });

        // 5) Simular siguiente estado (solo UI local)
        btnSimular.setOnClickListener(v -> {
            // Avanza 0→1→2→3 y al llegar a 3 se queda en "Entregado"
            if (estadoIndex < ESTADOS.length - 1) {
                estadoIndex++;
                aplicarEstadoVisual(estadoIndex);
            } else {
                Toast.makeText(this, "Pedido ya Entregado", Toast.LENGTH_SHORT).show();
            }
        });

        // 6) Guardar estado actual en Firebase /tracking/{uid}/{pedidoId}
        btnGuardar.setOnClickListener(v -> {
            String id = etPedidoId.getText().toString().trim();       // ID del pedido
            if (TextUtils.isEmpty(id)) {
                etPedidoId.setError("Ingresa un ID de pedido");
                etPedidoId.requestFocus();
                return;
            }
            if (auth.getCurrentUser() == null) {                      // Requiere login
                Toast.makeText(this, "Inicia sesión para guardar", Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = auth.getCurrentUser().getUid();              // UID
            String estado = ESTADOS[estadoIndex];                     // Texto del estado
            long ts = System.currentTimeMillis();                     // Marca de tiempo

            // Pequeño mapa para guardar
            Map<String, Object> data = new HashMap<>();
            data.put("estado", estado);
            data.put("indice", estadoIndex);
            data.put("timestamp", ts);

            // Escribimos en /tracking/{uid}/{pedidoId}
            trackRef.child(uid).child(id).setValue(data)
                    .addOnSuccessListener(a -> Toast.makeText(this, "Estado guardado", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // 7) Leer estado desde Firebase y pintar UI
        btnLeer.setOnClickListener(v -> {
            String id = etPedidoId.getText().toString().trim();       // ID del pedido
            if (TextUtils.isEmpty(id)) {
                etPedidoId.setError("Ingresa un ID de pedido");
                etPedidoId.requestFocus();
                return;
            }
            if (auth.getCurrentUser() == null) {                      // Requiere login
                Toast.makeText(this, "Inicia sesión para leer", Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = auth.getCurrentUser().getUid();              // UID

            trackRef.child(uid).child(id).get().addOnSuccessListener(snap -> {
                if (!snap.exists()) {
                    Toast.makeText(this, "No hay estado guardado para ese ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Leemos "indice" (si no está, calculamos por "estado")
                Long idx = snap.child("indice").getValue(Long.class);
                String estado = snap.child("estado").getValue(String.class);

                if (idx != null) {
                    estadoIndex = Math.max(0, Math.min(3, idx.intValue()));  // Clamp 0..3
                } else if (estado != null) {
                    estadoIndex = nombreAIndice(estado); // Fallback si solo guardaron "estado"
                } else {
                    estadoIndex = 0; // Por defecto
                }
                aplicarEstadoVisual(estadoIndex);                       // Pinta la UI
                Toast.makeText(this, "Estado cargado: " + ESTADOS[estadoIndex], Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Error al leer: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });
    }

    // Convierte texto a índice (por si llegó solo "estado" desde Firebase)
    private int nombreAIndice(String estado) {
        for (int i = 0; i < ESTADOS.length; i++) {
            if (ESTADOS[i].equalsIgnoreCase(estado)) return i;
        }
        return 0; // Desconocido → "Recibido"
    }

    // Colorea dots/labels según el estado actual (0..3) y actualiza el texto principal
    private void aplicarEstadoVisual(int idx) {
        // Colores básicos (hex): activo (verde), semi-activo (azul), apagado (gris)
        final int COLOR_OK   = 0xFF2E7D32; // Verde
        final int COLOR_STEP = 0xFF1565C0; // Azul
        final int COLOR_OFF  = 0xFFBDBDBD; // Gris

        // 1) Reset: todo a gris
        dot1.setTextColor(COLOR_OFF); dot2.setTextColor(COLOR_OFF);
        dot3.setTextColor(COLOR_OFF); dot4.setTextColor(COLOR_OFF);
        txt1.setTextColor(COLOR_OFF); txt2.setTextColor(COLOR_OFF);
        txt3.setTextColor(COLOR_OFF); txt4.setTextColor(COLOR_OFF);

        // 2) Enciende progresivamente hasta idx
        if (idx >= 0) { dot1.setTextColor(COLOR_STEP); txt1.setTextColor(COLOR_STEP); }
        if (idx >= 1) { dot2.setTextColor(COLOR_STEP); txt2.setTextColor(COLOR_STEP); }
        if (idx >= 2) { dot3.setTextColor(COLOR_STEP); txt3.setTextColor(COLOR_STEP); }
        if (idx >= 3) { dot4.setTextColor(COLOR_OK);   txt4.setTextColor(COLOR_OK);   }

        // 3) Texto grande del estado actual
        tvEstadoActual.setText(String.format(Locale.getDefault(),
                "Estado actual: %s", ESTADOS[idx]));
    }
}
