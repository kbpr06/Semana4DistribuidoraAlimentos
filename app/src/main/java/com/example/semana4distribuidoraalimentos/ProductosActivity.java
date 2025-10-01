package com.example.semana4distribuidoraalimentos; // Paquete del proyecto

// ===== IMPORTS BÁSICOS UI / ANDROID =====
import android.Manifest;                       // Permiso de ubicación
import android.content.pm.PackageManager;      // Para revisar permisos
import android.location.Criteria;              // Criterio para elegir proveedor de ubicación
import android.location.Location;              // Objeto de ubicación (lat/lon)
import android.location.LocationListener;      // Callback para requestSingleUpdate
import android.location.LocationManager;       // Servicio de ubicación del sistema
import android.os.Bundle;                      // Ciclo de vida (onCreate)
import android.text.TextUtils;                 // Validar texto vacío
import android.widget.ArrayAdapter;            // Adaptadores para Spinner/ListView
import android.widget.Button;                  // Botones
import android.widget.EditText;                // Inputs (cantidad, monto)
import android.widget.ListView;                // Lista del carrito
import android.widget.Spinner;                 // Selección de producto/ciudad
import android.widget.TextView;                // Textos (total, ubicación, resultado)
import android.widget.Toast;                   // Mensajes cortos

import androidx.annotation.NonNull;            // Anotación de null-safety
import androidx.appcompat.app.AppCompatActivity; // Activity base
import androidx.core.app.ActivityCompat;       // Pedir permisos en runtime
import androidx.core.content.ContextCompat;    // Consultar permisos en runtime

// ===== IMPORTS FIREBASE =====
import com.google.firebase.auth.FirebaseAuth;         // Sesión (para obtener UID)
import com.google.firebase.auth.FirebaseUser;         // Usuario autenticado
import com.google.firebase.database.DatabaseReference;// Referencia a Realtime Database
import com.google.firebase.database.FirebaseDatabase; // Instancia de la DB

// ===== IMPORTS JAVA =====
import java.util.ArrayList;                   // Lista en memoria para el carrito
import java.util.HashMap;                    // Map simple para guardar carritos
import java.util.LinkedHashMap;              // Map que preserva orden (para ciudades)
import java.util.List;                       // Interface List
import java.util.Locale;                     // Formateo de strings
import java.util.Map;                        // Estructuras clave-valor

public class ProductosActivity extends AppCompatActivity {

    // ========= (A) CATÁLOGO SIMPLE: nombres y precios (como tu versión previa) =========
    private static final String[] NOMBRES = new String[]{
            "Arroz 1 kg", "Leche 1 L", "Aceite 1 L", "Azúcar 1 kg", "Harina 1 kg"
    }; // Nombres visibles en el Spinner
    private static final int[] PRECIOS = new int[]{
            1200, 900, 2500, 1100, 1300
    }; // Precio unitario correspondiente por índice

    // ========= (B) VISTAS DEL XML (ids que nos compartiste) =========
    private Spinner spProducto;               // Selector de producto (catálogo)
    private EditText etCantidad;              // Cantidad para agregar
    private Button btnAgregar;                // Agregar al carrito

    private ListView lvCarrito;               // Lista con líneas del carrito
    private TextView tvTotal;                 // Total acumulado del carrito
    private Button btnGuardar;                // Guardar carrito en Firebase
    private Button btnAplicarSubtotal;        // Copiar total del carrito al monto del despacho

    // --- Card de Despacho:
    private EditText editMontoCarrito;        // Monto que llega del carrito (o manual)
    private Spinner spinnerCiudad;            // Ciudad (Plaza de Armas destino)
    private TextView tvMiUbicacion;           // Muestra mi lat/lon
    private Button btnObtenerUbicacion;       // Botón para obtener ubicación
    private Button btnCalcularTotal;          // Botón para calcular costo y total final
    private TextView tvResultadoDespacho;     // Resultado: Distancia | Despacho | Total
    private Button btnConfirmarPedido;        // Guarda pedido completo (carrito + despacho)

    // ========= (C) ESTADO EN MEMORIA PARA EL CARRITO =========
    private final List<String> lineas = new ArrayList<>(); // Líneas formateadas "Producto xN = $X"
    private ArrayAdapter<String> adaptador;                // Adapter para el ListView
    private int total = 0;                                 // Total acumulado en CLP

    // ========= (D) UBICACIÓN (como en tu MainActivity) =========
    private static final int REQ_LOC = 1001;      // Código para request de permisos
    private LocationManager locationManager;      // Servicio de ubicación del sistema
    private double miLat = Double.NaN;            // Latitud actual (NaN si no disponible)
    private double miLon = Double.NaN;            // Longitud actual (NaN si no disponible)

    // ========= (E) MAPA DE CIUDADES → COORDENADAS (Plaza de Armas) =========
    private Map<String, double[]> plazas;         // "Ciudad" -> {lat, lon}

    // ========= (F) FIREBASE =========
    private FirebaseAuth auth;                    // Para obtener UID del usuario
    private DatabaseReference ordersRef;          // Nodo /orders en Realtime DB

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Punto de entrada de la pantalla
        super.onCreate(savedInstanceState);              // Llama a la superclase
        setContentView(R.layout.activity_productos);     // Vincula con tu XML

        // --- (1) Referencias a vistas del Card "Carrito"
        spProducto = findViewById(R.id.spProducto);      // Spinner catálogo
        etCantidad = findViewById(R.id.etCantidad);      // Cantidad
        btnAgregar = findViewById(R.id.btnAgregar);      // Botón Agregar

        lvCarrito  = findViewById(R.id.lvCarrito);       // ListView del carrito
        tvTotal    = findViewById(R.id.tvTotal);         // Texto Total
        btnGuardar = findViewById(R.id.btnGuardar);      // Botón Guardar carrito

        btnAplicarSubtotal = findViewById(R.id.btnAplicarSubtotal); // Copiar total al despacho

        // --- (2) Referencias a vistas del Card "Despacho"
        editMontoCarrito    = findViewById(R.id.editMontoCarrito);  // Monto del carrito
        spinnerCiudad       = findViewById(R.id.spinnerCiudad);     // Ciudad destino
        tvMiUbicacion       = findViewById(R.id.tvMiUbicacion);     // Mi lat/lon
        btnObtenerUbicacion = findViewById(R.id.btnObtenerUbicacion);// Pedir ubicación
        btnCalcularTotal    = findViewById(R.id.btnCalcularTotal);  // Calcular despacho+total
        tvResultadoDespacho = findViewById(R.id.tvResultadoDespacho);// Mostrar resultado
        btnConfirmarPedido  = findViewById(R.id.btnConfirmarPedido);// Guardar pedido completo

        // --- (3) Inicializar servicios: ubicación y Firebase
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);           // Ubicación
        auth = FirebaseAuth.getInstance();                                                // Sesión
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");                // /orders

        // --- (4) Cargar catálogo en Spinner (mismo enfoque que tu versión previa)
        ArrayAdapter<String> adProductos = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, NOMBRES);                    // Nombres
        adProductos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);// Dropdown
        spProducto.setAdapter(adProductos);                                               // Aplica al Spinner

        // --- (5) Preparar adapter para la lista del carrito
        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lineas);// Lista simple
        lvCarrito.setAdapter(adaptador);                                                  // Enlaza a ListView

        // --- (6) Llenar mapa de ciudades y montar Spinner de ciudad (despacho)
        plazas = new LinkedHashMap<>();                                                   // Mantiene el orden
        plazas.put("Santiago",   new double[]{-33.4372, -70.6506});
        plazas.put("Valparaíso", new double[]{-33.0458, -71.6197});
        plazas.put("Concepción", new double[]{-36.8270, -73.0503});
        plazas.put("Frutillar",  new double[]{-41.1258, -73.0604});

        ArrayAdapter<String> adCiudades = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(plazas.keySet())); // Nombres
        adCiudades.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);      // Dropdown
        spinnerCiudad.setAdapter(adCiudades);                                                    // Aplica

        // --- (7) Click "Agregar": valida, arma línea, suma al carrito, actualiza total
        btnAgregar.setOnClickListener(v -> {
            String cantTxt = etCantidad.getText().toString().trim();                   // Lee cantidad
            if (TextUtils.isEmpty(cantTxt)) {                                          // Vacío -> error
                etCantidad.setError("Ingrese cantidad");
                etCantidad.requestFocus();
                return;                                                                // Corta el flujo
            }
            int cant;                                                                   // Valor entero
            try {
                cant = Integer.parseInt(cantTxt);                                      // Convierte a int
            } catch (NumberFormatException nfe) {
                etCantidad.setError("Cantidad inválida");
                etCantidad.requestFocus();
                return;                                                                // Corta si inválido
            }
            if (cant <= 0) {                                                           // Debe ser > 0
                etCantidad.setError("Debe ser mayor a 0");
                etCantidad.requestFocus();
                return;
            }

            int idx = spProducto.getSelectedItemPosition();                             // Índice producto
            String nombre = NOMBRES[idx];                                              // Nombre
            int precio = PRECIOS[idx];                                                 // Precio unitario
            int subtotal = precio * cant;                                              // Subtotal línea

            String linea = String.format(Locale.getDefault(),
                    "%s x%d = $%d", nombre, cant, subtotal);                           // Formatea línea

            lineas.add(linea);                                                         // Agrega al carrito
            adaptador.notifyDataSetChanged();                                          // Refresca lista
            total += subtotal;                                                         // Suma al total

            tvTotal.setText(String.format(Locale.getDefault(), "Total: $%d", total));  // Muestra total
            etCantidad.setText("");                                                    // Limpia input
        });

        // --- (8) Guardar carrito simple en Firebase (como ya hacías)
        btnGuardar.setOnClickListener(v -> {
            if (lineas.isEmpty()) {                                                    // Nada que guardar
                Toast.makeText(this, "Agrega productos antes de guardar", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = auth.getCurrentUser();                                 // Usuario actual
            if (user == null) {                                                        // Sin sesión
                Toast.makeText(this, "Inicia sesión para guardar", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();                                                // UID
            long ts = System.currentTimeMillis();                                       // Timestamp

            Map<String, Object> pedido = new HashMap<>();                               // Estructura simple
            pedido.put("lines", new ArrayList<>(lineas));                               // Copia de líneas
            pedido.put("total", total);                                                 // Total carrito
            pedido.put("timestamp", ts);                                                // Marca de tiempo

            ordersRef.child(uid).child("carritos").child(String.valueOf(ts))            // /orders/{uid}/carritos/ts
                    .setValue(pedido)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Carrito guardado en Firebase", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // --- (9) Pasar el subtotal del carrito al campo del despacho
        btnAplicarSubtotal.setOnClickListener(v ->
                editMontoCarrito.setText(String.valueOf(total))                         // Copia el total
        );

        // --- (10) Obtener mi ubicación (como en tu MainActivity)
        btnObtenerUbicacion.setOnClickListener(v -> solicitarUbicacion());              // Pide fix

        // --- (11) Calcular distancia + costo despacho + total final
        btnCalcularTotal.setOnClickListener(v -> calcularDespachoYTotal());             // Haversine + reglas

        // --- (12) Confirmar pedido (guarda carrito + despacho en una sola estructura)
        btnConfirmarPedido.setOnClickListener(v -> guardarPedidoCompleto());            // Persiste todo
    }

    // ===================== UBICACIÓN (misma estrategia que en MainActivity) =====================

    private void solicitarUbicacion() {                                                 // Pide permisos/fix
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {                                 // Si falta permiso…
            ActivityCompat.requestPermissions(this,                                       // Pide al usuario
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
            return;                                                                     // Salimos (vuelve por callback)
        }

        Location last = getLastKnownFineLocation();                                     // Última conocida (rápida)
        if (last != null) {                                                             // Si existe…
            actualizarUbicacion(last);                                                  // Actualiza UI/variables
        }

        try {                                                                           // Pide un fix único
            Criteria c = new Criteria();                                                // Criterio de proveedor
            c.setAccuracy(Criteria.ACCURACY_FINE);                                      // Precisión fina
            String provider = locationManager.getBestProvider(c, true);                 // Mejor proveedor activo
            if (provider != null) {
                locationManager.requestSingleUpdate(provider, new LocationListener() {  // Una sola actualización
                    @Override public void onLocationChanged(Location location) {
                        actualizarUbicacion(location);                                  // Actualiza cuando llegue
                    }
                    @Override public void onStatusChanged(String p, int s, Bundle b) {}
                    @Override public void onProviderEnabled(String p) {}
                    @Override public void onProviderDisabled(String p) {}
                }, null);
            }
        } catch (SecurityException ignored) { /* Si quitaron el permiso en medio del proceso */ }
    }

    private Location getLastKnownFineLocation() {                                       // Recorre proveedores activos
        try {
            for (String p : locationManager.getProviders(true)) {                       // true = solo activos
                Location l = locationManager.getLastKnownLocation(p);                   // Última por proveedor
                if (l != null) return l;                                               // Si hay una, basta
            }
        } catch (SecurityException ignored) { }
        return null;                                                                    // No hay caché
    }

    private void actualizarUbicacion(Location loc) {                                    // Refresca lat/lon y UI
        miLat = loc.getLatitude();                                                      // Guarda lat
        miLon = loc.getLongitude();                                                     // Guarda lon
        tvMiUbicacion.setText(String.format(Locale.getDefault(),                        // Muestra en pantalla
                "Mi ubicación: lat=%.6f, lon=%.6f", miLat, miLon));
    }

    // ===================== DESPACHO: cálculo distancia/costo/total =====================

    private void calcularDespachoYTotal() {                                             // Trigger del botón
        if (Double.isNaN(miLat) || Double.isNaN(miLon)) {                               // Sin ubicación aún
            Toast.makeText(this, "Primero obtén tu ubicación", Toast.LENGTH_SHORT).show();
            return;
        }

        String montoStr = editMontoCarrito.getText().toString().trim();                 // Lee monto
        if (TextUtils.isEmpty(montoStr)) {                                              // Requerido
            editMontoCarrito.setError("Ingrese monto");
            editMontoCarrito.requestFocus();
            return;
        }
        int monto;                                                                      // Parse a int
        try {
            monto = Integer.parseInt(montoStr);
        } catch (NumberFormatException nfe) {
            editMontoCarrito.setError("Monto inválido");
            editMontoCarrito.requestFocus();
            return;
        }

        String ciudad = (String) spinnerCiudad.getSelectedItem();                       // Ciudad elegida
        double[] dest = plazas.get(ciudad);                                             // Lat/lon destino
        double dKm = haversineKm(miLat, miLon, dest[0], dest[1]);                       // Distancia en km
        int kmRedondeados = (int) Math.round(dKm);                                      // Para reglas por km
        int costo = costoDespacho(monto, kmRedondeados);                                // Costo según reglas
        int totalFinal = monto + costo;                                                 // Suma final

        tvResultadoDespacho.setText(String.format(Locale.getDefault(),                  // Muestra resultado
                "Distancia: %.2f km  |  Despacho: $%d  |  Total: $%d",
                dKm, costo, totalFinal));
    }

    private int costoDespacho(int monto, int km) {                                      // Reglas semana 4
        if (monto >= 50000) return 0;                                                   // ≥ 50.000 → gratis
        if (monto >= 25000) return 150 * km;                                            // 25.000..49.999 → $150/km
        return 300 * km;                                                                // < 25.000 → $300/km
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {    // Distancia km
        final double R = 6371.0;                                                        // Radio Tierra (km)
        double dLat = Math.toRadians(lat2 - lat1);                                      // Delta lat en radianes
        double dLon = Math.toRadians(lon2 - lon1);                                      // Delta lon en radianes
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);                                  // Componente 'a'
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));                      // Ángulo central
        return R * c;                                                                    // Distancia
    }

    // ===================== CONFIRMAR PEDIDO (carrito + despacho) =====================

    private void guardarPedidoCompleto() {                                              // Persiste todo el pedido
        if (lineas.isEmpty()) {                                                         // Requiere carrito
            Toast.makeText(this, "Agrega productos antes de confirmar", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser user = auth.getCurrentUser();                                      // Usuario actual
        if (user == null) {
            Toast.makeText(this, "Inicia sesión para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        String montoStr = editMontoCarrito.getText().toString().trim();                 // Monto desde UI
        if (TextUtils.isEmpty(montoStr)) {
            editMontoCarrito.setError("Ingrese monto");
            editMontoCarrito.requestFocus();
            return;
        }
        int monto;
        try {
            monto = Integer.parseInt(montoStr);                                         // Parse monto
        } catch (NumberFormatException nfe) {
            editMontoCarrito.setError("Monto inválido");
            editMontoCarrito.requestFocus();
            return;
        }

        if (Double.isNaN(miLat) || Double.isNaN(miLon)) {                               // Requiere ubicación
            Toast.makeText(this, "Obtén tu ubicación antes de confirmar", Toast.LENGTH_SHORT).show();
            return;
        }

        String ciudad = (String) spinnerCiudad.getSelectedItem();                       // Ciudad elegida
        double[] dest = plazas.get(ciudad);                                             // Lat/lon destino
        double dKm = haversineKm(miLat, miLon, dest[0], dest[1]);                       // Distancia
        int kmRedondeados = (int) Math.round(dKm);                                      // Entero para costo
        int costo = costoDespacho(monto, kmRedondeados);                                // Costo
        int totalFinal = monto + costo;                                                 // Total

        long ts = System.currentTimeMillis();                                           // Id del pedido
        Pedido pedido = new Pedido(                                                     // Objeto a guardar
                new ArrayList<>(lineas),                                                // Copia del carrito
                monto, ciudad, miLat, miLon, dKm, costo, totalFinal, "CREATED"          // Datos despacho
        );

        ordersRef.child(user.getUid())                                                  // /orders/{uid}
                .child("pedidos")                                                       // /pedidos
                .child(String.valueOf(ts))                                              // /{ts}
                .setValue(pedido)                                                       // Guarda JSON
                .addOnSuccessListener(a -> Toast.makeText(this, "Pedido guardado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ===================== PERMISOS (callback) =====================

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {              // Respuesta del diálogo
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {             // Si aceptó…
            solicitarUbicacion();                                                      // Reintenta obtener fix
        }
    }

    // ===================== MODELO PARA GUARDAR PEDIDO COMPLETO =====================

    public static class Pedido {                                                       // POJO compatible Firebase
        public List<String> items;      // Líneas del carrito (texto)
        public int subtotal;            // Monto del carrito
        public String ciudadDestino;    // Ciudad/plaza elegida
        public double lat, lon;         // Coordenadas del cliente
        public double distanciaKm;      // Distancia calculada (double)
        public int costoDespacho;       // Costo según reglas
        public int totalFinal;          // subtotal + costoDespacho
        public String status;           // Estado (CREATED, etc.)

        public Pedido() {}              // Constructor vacío requerido por Firebase

        public Pedido(List<String> items, int subtotal, String ciudadDestino,
                      double lat, double lon, double distanciaKm,
                      int costoDespacho, int totalFinal, String status) {             // Constructor útil
            this.items = items;
            this.subtotal = subtotal;
            this.ciudadDestino = ciudadDestino;
            this.lat = lat;
            this.lon = lon;
            this.distanciaKm = distanciaKm;
            this.costoDespacho = costoDespacho;
            this.totalFinal = totalFinal;
            this.status = status;
        }
    }
}
