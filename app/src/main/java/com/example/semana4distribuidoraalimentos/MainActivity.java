package com.example.semana4distribuidoraalimentos; // Paquete de tu app

// ===== IMPORTS (UI, ciclo de vida, logs) =====
import android.os.Bundle;               // onCreate / ciclo de vida
import android.util.Log;                // Mensajes a Logcat (evidencias)
import android.widget.Button;           // Botones
import android.widget.EditText;         // Entradas de texto
import android.widget.TextView;         // Salida de texto
import android.widget.Toast;            // Mensajes breves

import androidx.appcompat.app.AppCompatActivity; // Base Activity

// ===== IMPORTS (Ubicación + Spinner) =====
import android.Manifest;                      // Constantes de permisos
import android.content.pm.PackageManager;     // Consulta de permisos
import android.location.Criteria;             // Criterios para proveedor de ubicación
import android.location.Location;             // Estructura lat/lon
import android.location.LocationListener;     // Callback de ubicación
import android.location.LocationManager;      // Servicio nativo de ubicación
import android.widget.ArrayAdapter;           // Adaptador para Spinner
import android.widget.Spinner;                // Lista desplegable

import androidx.core.app.ActivityCompat;      // Solicitar permisos en runtime
import androidx.core.content.ContextCompat;   // Chequear permisos en runtime

import java.util.ArrayList;                   // Lista de nombres de ciudades
import java.util.LinkedHashMap;               // Mapa ordenado (ciudad -> coords)
import java.util.Map;                         // Interface Map

public class MainActivity extends AppCompatActivity {

    // ===== Constantes generales =====
    private static final String TAG = "Semana4App"; // TAG para logs de la parte “cálculos”
    private static final String TAG_LOC = "Semana5"; // TAG para logs de la parte “ubicación”

    // ===== (Semana 5) Campos de ubicación =====
    private static final int REQ_LOC = 1001;       // Código de request de permisos
    private LocationManager locationManager;       // Servicio de ubicación del sistema
    private TextView tvMiUbicacion;                // Muestra lat/lon del dispositivo
    private Spinner spinnerCiudad;                 // Selección de Plaza de Armas (ciudad)
    private TextView tvResultadoDistancia;         // Resultado de distancia en km

    private double miLat = Double.NaN;             // Lat actual (NaN si no hay fix aún)
    private double miLon = Double.NaN;             // Lon actual (NaN si no hay fix aún)
    private Map<String, double[]> plazas;          // "Ciudad" -> {lat, lon} de su Plaza

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Al crear la pantalla
        super.onCreate(savedInstanceState);              // Llama a la superclase
        setContentView(R.layout.activity_main);          // Vincula layout XML a esta Activity

        // ======================== (SEMANA 4) CÁLCULOS ========================

        // Referencias a inputs de la sección de cálculos
        EditText editMonto     = findViewById(R.id.editMonto);      // Monto (> 0)
        EditText editDistancia = findViewById(R.id.editDistancia);  // Distancia (0..20)
        EditText editGrados    = findViewById(R.id.editGrados);     // Grados (double; 180 por defecto)

        // Salida de resultados y botón de calcular
        TextView textResultado = findViewById(R.id.textResultado);  // Salida formateada
        Button btnCalcular     = findViewById(R.id.btnCalcular);    // Dispara cálculos

        // Click: valida → calcula despacho → convierte a radianes → muestra resultados
        btnCalcular.setOnClickListener(v -> {
            // Limpia errores previos en los campos
            editMonto.setError(null);
            editDistancia.setError(null);
            editGrados.setError(null);

            // 1) Monto (> 0)
            Integer monto = parseEnteroPositivo(editMonto, "Monto de compra");
            if (monto == null) return;

            // 2) Distancia (0..20)
            Integer distancia = parseEnteroEnRango(editDistancia, "Distancia (km)", 0, 20);
            if (distancia == null) return;

            // 3) Grados (double, 180.0 si vacío)
            Double grados = parseDoubleOpcional(editGrados, 180.0);
            if (grados == null) return;

            // 4) Reglas del despacho según el caso
            int costoDespacho = calcularDespacho(monto, distancia);

            // 5) Conversión a radianes
            double radianes = aRadianes(grados);

            // 6) Armar texto de salida
            StringBuilder sb = new StringBuilder();
            sb.append("=== Distribuidora ===\n");
            sb.append("Compra: $").append(monto).append("\n");
            sb.append("Distancia: ").append(distancia).append(" km\n");
            sb.append("Despacho: $").append(costoDespacho).append("\n");
            sb.append("TOTAL: $").append(monto + costoDespacho).append("\n\n");
            sb.append("Conversión a radianes:\n");
            sb.append(String.format("%.4f° = %.6f rad", grados, radianes));

            // 7) Mostrar en la UI
            textResultado.setText(sb.toString());

            // 8) Evidencia por consola
            System.out.println(sb.toString());

            // 9) Evidencia en Logcat
            Log.d(TAG, "Compra=" + monto + ", km=" + distancia + ", despacho=" + costoDespacho);
            Log.d(TAG, "Grados=" + grados + " -> radianes=" + radianes);
        });

        // ==================== (SEMANA 5) UBICACIÓN + HAVERSINE ====================

        // Referencias de UI para la sección de ubicación
        tvMiUbicacion        = findViewById(R.id.tvMiUbicacion);        // Muestra mi lat/lon
        spinnerCiudad        = findViewById(R.id.spinnerCiudad);        // Lista de ciudades
        tvResultadoDistancia = findViewById(R.id.tvResultadoDistancia); // Distancia en km

        Button btnObtenerUbicacion  = findViewById(R.id.btnObtenerUbicacion);   // Pide fix
        Button btnCalcularDistancia = findViewById(R.id.btnCalcularDistancia);  // Calcula km

        // Servicio de ubicación
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Mapa de ciudades → coordenadas de su Plaza
        plazas = new LinkedHashMap<>(); // Conserva el orden de inserción
        plazas.put("Santiago",    new double[]{-33.4372, -70.6506});
        plazas.put("Valparaíso",  new double[]{-33.0458, -71.6197});
        plazas.put("Concepción",  new double[]{-36.8270, -73.0503});
        plazas.put("La Serena",   new double[]{-29.9045, -71.2489});
        plazas.put("Temuco",      new double[]{-38.7359, -72.5904});
        plazas.put("Frutillar",   new double[]{-41.1258, -73.0604});

        // Nombres para el Spinner
        ArrayList<String> nombres = new ArrayList<>(plazas.keySet());

        // Adaptador del Spinner
        ArrayAdapter<String> ad = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                nombres
        );
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCiudad.setAdapter(ad);

        // Botón: obtener mi ubicación (pide permiso si falta, toma last known y luego single update)
        btnObtenerUbicacion.setOnClickListener(v -> solicitarUbicacion());

        // Botón: calcular distancia a la Plaza de la ciudad seleccionada (Haversine)
        btnCalcularDistancia.setOnClickListener(v -> {
            if (Double.isNaN(miLat) || Double.isNaN(miLon)) { // Validar que ya haya fix
                Toast.makeText(this, "Primero obtén tu ubicación", Toast.LENGTH_SHORT).show();
                return;
            }
            String ciudad = (String) spinnerCiudad.getSelectedItem(); // Ciudad elegida
            double[] destino = plazas.get(ciudad);                    // {lat, lon} de la Plaza
            double dKm = haversineKm(miLat, miLon, destino[0], destino[1]); // Distancia km
            tvResultadoDistancia.setText(String.format("Distancia: %.3f km", dKm)); // UI
            System.out.println("Haversine (" + ciudad + "): " + dKm + " km");      // Consola
            Log.d(TAG_LOC, "Haversine (" + ciudad + "): " + dKm + " km");          // Logcat
        });
    }

    // ===== Reglas del despacho (caso) =====
    private int calcularDespacho(int monto, int km) {
        if (monto >= 50000) {          // Si compra >= 50.000
            return 0;                  // → despacho gratis
        } else if (monto >= 25000) {   // 25.000..49.999
            return 150 * km;           // → $150 por km
        } else {                       // < 25.000
            return 300 * km;           // → $300 por km
        }
    }

    // ===== Conversión grados → radianes =====
    private double aRadianes(double grados) {
        return grados * Math.PI / 180.0; // Fórmula matemática
        // Alternativa: return Math.toRadians(grados);
    }

    // ===== Validaciones de entrada =====

    // Entero > 0
    private Integer parseEnteroPositivo(EditText et, String nombreCampo) {
        String txt = et.getText().toString().trim();   // Texto sin espacios laterales
        if (txt.isEmpty()) {                           // Vacío → error
            et.setError("Requerido");
            et.requestFocus();
            Toast.makeText(this, nombreCampo + " es requerido", Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            int valor = Integer.parseInt(txt);         // Parse a entero
            if (valor <= 0) {                          // Debe ser > 0
                et.setError("Debe ser mayor a 0");
                et.requestFocus();
                Toast.makeText(this, nombreCampo + " debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return null;
            }
            return valor;                              // OK
        } catch (NumberFormatException e) {            // No es número válido
            et.setError("Ingrese un número válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en " + nombreCampo, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Entero en rango [min..max]
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
            if (valor < min || valor > max) {          // Límite inferior/superior
                et.setError("Debe estar entre " + min + " y " + max);
                et.requestFocus();
                Toast.makeText(this, nombreCampo + " debe estar entre " + min + " y " + max, Toast.LENGTH_SHORT).show();
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

    // Double opcional (usa valor por defecto si está vacío)
    private Double parseDoubleOpcional(EditText et, double valorPorDefecto) {
        String txt = et.getText().toString().trim();
        if (txt.isEmpty()) return valorPorDefecto;     // Vacío → usa default
        try {
            return Double.parseDouble(txt);            // Parse a double
        } catch (NumberFormatException e) {            // No es double válido
            et.setError("Ingrese un decimal válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en Grados", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ===== Ubicación: solicitar fix (permiso → lastKnown → singleUpdate) =====
    private void solicitarUbicacion() {
        // 1) ¿Tenemos permiso?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    REQ_LOC
            );
            return; // Salimos y esperamos el callback de permisos
        }

        // 2) Last known (si existe, es instantáneo)
        Location last = getLastKnownFineLocation();
        if (last != null) {
            actualizarUbicacion(last);
        }

        // 3) Single update: pedimos un fix nuevo
        try {
            Criteria c = new Criteria();                  // Criterio de precisión
            c.setAccuracy(Criteria.ACCURACY_FINE);        // Precisión fina (GPS)
            String provider = locationManager.getBestProvider(c, true); // Mejor proveedor activo
            if (provider != null) {
                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) { // Nueva ubicación
                        actualizarUbicacion(location);                // Actualiza UI/estado
                    }
                    @Override public void onStatusChanged(String p, int s, Bundle b) { /* no usado */ }
                    @Override public void onProviderEnabled(String p) { /* no usado */ }
                    @Override public void onProviderDisabled(String p) { /* no usado */ }
                }, null);
                Toast.makeText(this, "Obteniendo ubicación…", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sin proveedor de ubicación activo", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException se) {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
        }
    }

    // Recorre proveedores activos y devuelve la última ubicación conocida (o null)
    private Location getLastKnownFineLocation() {
        try {
            for (String p : locationManager.getProviders(true)) { // true = sólo activos
                Location l = locationManager.getLastKnownLocation(p);
                if (l != null) return l; // Retorna el primero válido
            }
        } catch (SecurityException ignored) { /* Si no hay permiso, retorna null */ }
        return null;
    }

    // Actualiza variables y UI, y deja evidencia en Logcat
    private void actualizarUbicacion(Location loc) {
        miLat = loc.getLatitude();   // Guarda latitud
        miLon = loc.getLongitude();  // Guarda longitud
        tvMiUbicacion.setText(String.format("Mi ubicación: lat=%.6f, lon=%.6f", miLat, miLon));
        Log.d(TAG_LOC, "Mi ubicación: " + miLat + ", " + miLon);
    }

    // Haversine (distancia entre dos puntos geográficos en km)
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;                             // Radio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);           // Delta de latitud (rad)
        double dLon = Math.toRadians(lon2 - lon1);           // Delta de longitud (rad)
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)   // Fórmula Haversine
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); // Ángulo central
        return R * c;                                         // Distancia resultante (km)
    }

    // Callback del diálogo de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC) { // ¿Es nuestra solicitud?
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                solicitarUbicacion(); // Vuelve a intentar si el usuario aceptó
            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
