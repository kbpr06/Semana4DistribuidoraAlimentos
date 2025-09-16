package com.example.semana4distribuidoraalimentos; // Paquete del proyecto (minúsculas)

// ========== IMPORTS (SEMANA 4: UI, ciclo de vida, logs) ==========
import android.os.Bundle;               // Para onCreate y ciclo de vida de la Activity
import android.util.Log;                // Para escribir en Logcat (evidencias)
import android.widget.Button;           // Botón en la interfaz
import android.widget.EditText;         // Entradas de texto (monto, distancia, grados)
import android.widget.TextView;         // Texto de salida (resultados)
import android.widget.Toast;            // Mensajes cortos al usuario (validaciones)

import androidx.appcompat.app.AppCompatActivity; // Clase base de la Activity

// ========== IMPORTS EXTRA (SEMANA 5: ubicación + spinner) ==========
import android.Manifest;                      // Constantes de permisos (ACCESS_FINE_LOCATION)
import android.content.pm.PackageManager;     // Verificar si el permiso está concedido
import android.location.Criteria;             // Criterios para elegir proveedor de ubicación
import android.location.Location;             // Estructura con latitud/longitud
import android.location.LocationListener;     // Callback al recibir ubicación
import android.location.LocationManager;      // Servicio de ubicación del sistema
import android.widget.ArrayAdapter;           // Adaptador para el Spinner (ciudades)
import android.widget.Spinner;                // Control desplegable para seleccionar ciudad

import androidx.core.app.ActivityCompat;      // Pedir permisos en tiempo de ejecución
import androidx.core.content.ContextCompat;   // Consultar permisos en tiempo de ejecución

import java.util.ArrayList;                   // Lista para nombres de ciudades
import java.util.LinkedHashMap;               // Mapa que preserva orden de inserción
import java.util.Map;                         // Interface Map para ciudad->coordenadas

public class MainActivity extends AppCompatActivity {
    // ===================== CONSTANTES Y CAMPOS COMUNES =====================
    private static final String TAG = "Semana4App"; // TAG para filtrar logs de Semana 4

    // ===================== (SEMANA 5) CAMPOS DE UBICACIÓN ==================
    private static final int REQ_LOC = 1001;  // Código para el callback de permisos
    private LocationManager locationManager;  // Servicio del sistema que entrega ubicación
    private TextView tvMiUbicacion;           // Muestra la lat/lon del dispositivo
    private Spinner spinnerCiudad;            // Permite elegir la Plaza de Armas (ciudad)
    private TextView tvResultadoDistancia;    // Muestra la distancia calculada en km
    private double miLat = Double.NaN;        // Latitud del dispositivo (NaN si aún no disponible)
    private double miLon = Double.NaN;        // Longitud del dispositivo (NaN si aún no disponible)
    private Map<String, double[]> plazas;     // Mapa: "Ciudad" -> {lat, lon} de su Plaza de Armas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                 // Inicializa la Activity
        setContentView(R.layout.activity_main);             // Vincula esta pantalla con el XML

        // ======================== SEMANA 4 (CÁLCULOS) ======================

        // Referencias a los campos de entrada
        EditText editMonto     = findViewById(R.id.editMonto);      // Monto de compra (entero > 0)
        EditText editDistancia = findViewById(R.id.editDistancia);  // Distancia (entero entre 0..20)
        EditText editGrados    = findViewById(R.id.editGrados);     // Grados (double; 180.0 por defecto)

        // Referencia al TextView de salida
        TextView textResultado = findViewById(R.id.textResultado);  // Resultado formateado

        // Botón que dispara los cálculos
        Button btnCalcular     = findViewById(R.id.btnCalcular);

        // Listener del botón de Semana 4: valida, calcula despacho, convierte a radianes y muestra todo
        btnCalcular.setOnClickListener(v -> {
            // Limpia errores anteriores en los campos (si quedaron marcados)
            editMonto.setError(null);
            editDistancia.setError(null);
            editGrados.setError(null);

            // 1) Leer y validar el monto (> 0). Si falla, la función muestra error y retorna null.
            Integer monto = parseEnteroPositivo(editMonto, "Monto de compra");
            if (monto == null) return; // Si no es válido, detenemos el flujo

            // 2) Leer y validar la distancia (0..20). Si falla, retorna null.
            Integer distancia = parseEnteroEnRango(editDistancia, "Distancia (km)", 0, 20);
            if (distancia == null) return;

            // 3) Leer grados; si el campo está vacío, se usa 180.0 por defecto.
            Double grados = parseDoubleOpcional(editGrados, 180.0);
            if (grados == null) return; // Si el formato es inválido, ya se mostró error

            // 4) Calcular costo de despacho según reglas del caso
            int costoDespacho = calcularDespacho(monto, distancia);

            // 5) Convertir grados a radianes con fórmula: rad = grados * PI / 180
            double radianes = aRadianes(grados);

            // 6) Arma texto de salida con toda la información
            StringBuilder sb = new StringBuilder();
            sb.append("=== Distribuidora ===\n");
            sb.append("Compra: $").append(monto).append("\n");
            sb.append("Distancia: ").append(distancia).append(" km\n");
            sb.append("Despacho: $").append(costoDespacho).append("\n");
            sb.append("TOTAL: $").append(monto + costoDespacho).append("\n\n");
            sb.append("Conversión a radianes:\n");
            sb.append(String.format("%.4f° = %.6f rad", grados, radianes));

            String resumen = sb.toString(); // Convierte el builder a String final

            // 7) Muestra el resumen en pantalla
            textResultado.setText(resumen);

            // 8) Evidencia en consola estándar (System.out)
            System.out.println(resumen);

            // 9) Evidencia en Logcat (nivel DEBUG) para la rúbrica
            Log.d(TAG, "Compra=" + monto + ", km=" + distancia + ", despacho=" + costoDespacho);
            Log.d(TAG, "Grados=" + grados + " -> radianes=" + radianes);
        });


        // ==================== SEMANA 5 (UBICACIÓN + KM) =====================

        // Referencias a los controles de la nueva sección (Semana 5)
        tvMiUbicacion        = findViewById(R.id.tvMiUbicacion);        // Texto con mi lat/lon
        spinnerCiudad        = findViewById(R.id.spinnerCiudad);        // Selector de ciudad (Plaza)
        tvResultadoDistancia = findViewById(R.id.tvResultadoDistancia); // Resultado en km

        Button btnObtenerUbicacion  = findViewById(R.id.btnObtenerUbicacion);   // Obtener mi posición
        Button btnCalcularDistancia = findViewById(R.id.btnCalcularDistancia);  // Calcular Haversine

        // Instancia del servicio de ubicación del sistema
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Llenamos el mapa de ciudades -> coordenadas de su Plaza de Armas (lat, lon)
        plazas = new LinkedHashMap<>();                      // Mantiene orden de inserción
        plazas.put("Santiago",    new double[]{-33.4372, -70.6506}); // Plaza de Armas de Santiago
        plazas.put("Valparaíso",  new double[]{-33.0458, -71.6197}); // (aprox.) Plaza/Sotomayor
        plazas.put("Concepción",  new double[]{-36.8270, -73.0503}); // Plaza de la Independencia
        plazas.put("La Serena",   new double[]{-29.9045, -71.2489}); // Plaza de Armas La Serena
        plazas.put("Temuco",      new double[]{-38.7359, -72.5904}); // Plaza Aníbal Pinto
        plazas.put("Frutillar",   new double[]{-41.1258, -73.0604}); // Plaza de Armas de Frutillar

        // Creamos la lista de nombres de ciudades para poblar el Spinner
        ArrayList<String> nombres = new ArrayList<>(plazas.keySet()); // Nombres en el orden insertado

        // Adaptador visual simple para el Spinner
        ArrayAdapter<String> ad = new ArrayAdapter<>(
                this,                                      // Contexto
                android.R.layout.simple_spinner_item,      // Layout de ítem simple
                nombres);                                  // Datos (lista de ciudades)
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // Layout del desplegable
        spinnerCiudad.setAdapter(ad);                      // Enlaza el adaptador al Spinner

        // Al presionar "Obtener mi ubicación": solicita permisos si faltan y pide un fix de ubicación
        btnObtenerUbicacion.setOnClickListener(v -> solicitarUbicacion());

        // Al presionar "Calcular distancia": aplica Haversine entre mi posición y la ciudad elegida
        btnCalcularDistancia.setOnClickListener(v -> {
            // Primero, verificar que ya tengamos la lat/lon del dispositivo
            if (Double.isNaN(miLat) || Double.isNaN(miLon)) {
                Toast.makeText(this, "Primero obtén tu ubicación", Toast.LENGTH_SHORT).show();
                return; // Salimos si no hay ubicación
            }

            // Tomamos la ciudad seleccionada en el Spinner
            String ciudad = (String) spinnerCiudad.getSelectedItem();
            double[] destino = plazas.get(ciudad); // Coordenadas {lat, lon} de la Plaza de esa ciudad

            // Calculamos la distancia con Haversine
            double dKm = haversineKm(miLat, miLon, destino[0], destino[1]);

            // Mostramos el resultado en la UI
            tvResultadoDistancia.setText(String.format("Distancia: %.3f km", dKm));

            // Evidencias (consola + Logcat) para el informe
            System.out.println("Haversine (" + ciudad + "): " + dKm + " km");
            Log.d("Semana5", "Haversine (" + ciudad + "): " + dKm + " km");
        });
    }

    // ===================== (SEMANA 4) REGLAS DE NEGOCIO =====================

    // Cálculo del costo de despacho:
    // - Si monto >= 50.000 → $0
    // - Si 25.000 <= monto <= 49.999 → $150 por km
    // - Si monto < 25.000 → $300 por km
    private int calcularDespacho(int monto, int km) {
        if (monto >= 50000) {
            return 0;               // Despacho gratis
        } else if (monto >= 25000) {
            return 150 * km;        // Tarifa media
        } else {
            return 300 * km;        // Tarifa alta
        }
    }

    // Conversión grados → radianes (fórmula: grados * PI / 180)
    private double aRadianes(double grados) {
        return grados * Math.PI / 180.0; // Devuelve el valor en radianes
        // Alternativa equivalente: return Math.toRadians(grados);
    }

    // ===================== (SEMANA 4) VALIDACIONES =====================

    // Lee un entero > 0 desde un EditText; si falla, marca error y retorna null
    private Integer parseEnteroPositivo(EditText et, String nombreCampo) {
        String txt = et.getText().toString().trim();   // Texto ingresado sin espacios laterales
        if (txt.isEmpty()) {                           // Si está vacío, error
            et.setError("Requerido");
            et.requestFocus();
            Toast.makeText(this, nombreCampo + " es requerido", Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            int valor = Integer.parseInt(txt);         // Intenta convertir a entero
            if (valor <= 0) {                          // Debe ser > 0
                et.setError("Debe ser mayor a 0");
                et.requestFocus();
                Toast.makeText(this, nombreCampo + " debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return null;
            }
            return valor;                              // Si pasó validación, retorna el valor
        } catch (NumberFormatException e) {            // Si no es un número válido
            et.setError("Ingrese un número válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en " + nombreCampo, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Lee un entero en rango [min..max]; si falla, marca error y retorna null
    private Integer parseEnteroEnRango(EditText et, String nombreCampo, int min, int max) {
        String txt = et.getText().toString().trim();
        if (txt.isEmpty()) {                           // Campo vacío
            et.setError("Requerido");
            et.requestFocus();
            Toast.makeText(this, nombreCampo + " es requerido", Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            int valor = Integer.parseInt(txt);         // Convierto a entero
            if (valor < min || valor > max) {          // Chequeo de límites
                et.setError("Debe estar entre " + min + " y " + max);
                et.requestFocus();
                Toast.makeText(this, nombreCampo + " debe estar entre " + min + " y " + max, Toast.LENGTH_SHORT).show();
                return null;
            }
            return valor;                              // Valor aceptado
        } catch (NumberFormatException e) {            // No es número
            et.setError("Ingrese un número válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en " + nombreCampo, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Lee un double; si está vacío, usa valorPorDefecto; si formato inválido, retorna null
    private Double parseDoubleOpcional(EditText et, double valorPorDefecto) {
        String txt = et.getText().toString().trim();
        if (txt.isEmpty()) {                // Sin valor: se usa el por defecto (ej. 180.0)
            return valorPorDefecto;
        }
        try {
            return Double.parseDouble(txt); // Convierte a double
        } catch (NumberFormatException e) { // Formato inválido
            et.setError("Ingrese un decimal válido");
            et.requestFocus();
            Toast.makeText(this, "Formato no válido en Grados", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ===================== (SEMANA 5) UBICACIÓN + HAVERSINE =====================

    // Solicita un fix de ubicación: pide permisos si faltan, toma last known y luego un update
    private void solicitarUbicacion() {
        // 1) Verificar si ya tenemos el permiso de ubicación fina
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Si no, solicitarlo al usuario (aparecerá el diálogo del sistema)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC);
            return; // Salimos; cuando el usuario responda, vuelve por onRequestPermissionsResult
        }

        // 2) Intentar obtener la última ubicación conocida (más rápida si existe)
        Location last = getLastKnownFineLocation();
        if (last != null) {              // Si existe un último fix, lo usamos de inmediato
            actualizarUbicacion(last);   // Actualiza UI (lat/lon) y variables miLat/miLon
        }

        // 3) Pedir una actualización única al proveedor más adecuado
        try {
            Criteria c = new Criteria();                  // Criterio para elegir proveedor
            c.setAccuracy(Criteria.ACCURACY_FINE);        // Precisión fina (GPS si está disponible)
            String provider = locationManager.getBestProvider(c, true); // Mejor proveedor activo

            if (provider != null) {                       // Si existe un proveedor válido
                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        actualizarUbicacion(location);    // Al recibir nueva ubicación, actualiza todo
                    }
                    @Override public void onStatusChanged(String p, int s, Bundle b) { /* No usado */ }
                    @Override public void onProviderEnabled(String p) { /* No usado */ }
                    @Override public void onProviderDisabled(String p) { /* No usado */ }
                }, null);
                Toast.makeText(this, "Obteniendo ubicación…", Toast.LENGTH_SHORT).show(); // Feedback al usuario
            } else {
                Toast.makeText(this, "Sin proveedor de ubicación activo", Toast.LENGTH_SHORT).show(); // No hay GPS/red
            }
        } catch (SecurityException se) {
            // Si el permiso se revoca en medio del proceso
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
        }
    }

    // Recorre proveedores activos y retorna la última ubicación conocida (o null si no hay)
    private Location getLastKnownFineLocation() {
        try {
            for (String p : locationManager.getProviders(true)) { // true = solo proveedores activos
                Location l = locationManager.getLastKnownLocation(p);
                if (l != null) return l; // Si encuentra una, la retorna
            }
        } catch (SecurityException ignored) { /* Si llega sin permiso, cae aquí y retornará null */ }
        return null; // No hay última ubicación disponible
    }

    // Actualiza variables (miLat/miLon) y muestra la posición del dispositivo en la UI + Logcat
    private void actualizarUbicacion(Location loc) {
        miLat = loc.getLatitude();  // Guarda latitud actual
        miLon = loc.getLongitude(); // Guarda longitud actual
        tvMiUbicacion.setText(String.format("Mi ubicación: lat=%.6f, lon=%.6f", miLat, miLon)); // Muestra con 6 decimales
        Log.d("Semana5", "Mi ubicación: " + miLat + ", " + miLon); // Evidencia para Logcat
    }

    // Implementación de la fórmula de Haversine (distancia en km)
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;                              // Radio aproximado de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);            // Delta de latitud en radianes
        double dLon = Math.toRadians(lon2 - lon1);            // Delta de longitud en radianes
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)    // Componente 'a' de Haversine
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); // Ángulo central
        return R * c;                                          // Distancia final en kilómetros
    }

    // Callback del resultado de la solicitud de permisos (Android llama a este método)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Llamar a la superclase
        if (requestCode == REQ_LOC) { // Verifica que sea nuestra solicitud
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                solicitarUbicacion(); // Si otorgado, volvemos a intentar obtener la ubicación
            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación", Toast.LENGTH_SHORT).show(); // Aviso si se negó
            }
        }
    }
}

