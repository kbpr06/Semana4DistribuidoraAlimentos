package com.example.semana4distribuidoraalimentos; // Paquete de la app (debe coincidir con tu proyecto)

// ===== IMPORTS =====
import android.Manifest;                  // Constantes de permisos (ACCESS_FINE_LOCATION)
import android.content.Intent;            // Para navegar entre Activities
import android.content.pm.PackageManager; // Para chequear si el permiso está otorgado
import android.location.Criteria;         // Criterios para elegir proveedor de ubicación
import android.location.Location;         // Estructura que contiene lat/lon
import android.location.LocationListener; // Callback al recibir una ubicación
import android.location.LocationManager;  // Servicio nativo de ubicación del sistema
import android.net.Uri;                   // Para abrir Google Maps con un geo: URI
import android.os.Bundle;                 // Ciclo de vida (onCreate / onStart)
import android.util.Log;                  // Logs a Logcat (depuración)
import android.widget.Button;             // Referencias a botones del layout
import android.widget.TextView;           // Referencias a textos del layout
import android.widget.Toast;              // Mensajes breves al usuario

import androidx.annotation.NonNull;       // Anotación para callbacks de permisos
import androidx.appcompat.app.AppCompatActivity; // Base para Activities
import androidx.core.app.ActivityCompat;  // Para solicitar permisos en tiempo de ejecución
import androidx.core.content.ContextCompat; // Para consultar permisos en tiempo de ejecución

import com.example.semana4distribuidoraalimentos.models.UserLocation; // POJO para guardar en Firebase
import com.google.firebase.auth.FirebaseAuth;     // Autenticación (estado de sesión)
import com.google.firebase.auth.FirebaseUser;     // Usuario autenticado actual
import com.google.firebase.database.DatabaseReference; // Referencia a un nodo de Realtime DB
import com.google.firebase.database.FirebaseDatabase;  // Instancia de Realtime DB

import java.text.DateFormat;              // Para formatear fecha/hora legible
import java.util.Date;                    // Marca de tiempo (timestamp readable)

public class MenuActivity extends AppCompatActivity {

    // ===== Constantes / campos =====
    private static final String TAG = "MenuActivity"; // TAG para ver el flujo en Logcat
    private static final int REQ_LOC = 100;           // Código de petición de permiso de ubicación

    private FirebaseAuth auth;               // Manejador de autenticación (sesión)
    private DatabaseReference dbRef;         // Referencia a /users en Realtime Database
    private LocationManager locationManager; // Servicio nativo de ubicación (igual que en MainActivity)

    // Vistas del layout
    private TextView tvCorreo;               // Muestra el email del usuario autenticado
    private TextView tvUbicacion;            // Muestra lat/lon en pantalla
    private TextView tvHora;                 // Muestra última hora de actualización

    // Últimas coordenadas guardadas en memoria (para botón "Ver en mapa")
    private Double lastLat = null;           // Última latitud
    private Double lastLon = null;           // Última longitud

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Se ejecuta al crear la Activity
        super.onCreate(savedInstanceState);              // Llama a la superclase
        setContentView(R.layout.activity_menu);          // Infla el layout de esta pantalla

        // 1) Autenticación: obtenemos instancia y chequeamos si hay usuario logueado
        auth = FirebaseAuth.getInstance();               // Instancia de FirebaseAuth
        FirebaseUser user = auth.getCurrentUser();       // Usuario actual (puede ser null)
        if (user == null) {                              // Si no hay sesión activa…
            startActivity(new Intent(this, LoginActivity.class)); // Volvemos al login
            finish();                                    // Cerramos esta pantalla
            return;                                      // Evitamos seguir
        }

        // 2) Realtime Database y LocationManager (mismo enfoque que en MainActivity)
        dbRef = FirebaseDatabase.getInstance().getReference("users"); // Apunta a /users
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); // Servicio nativo

        // 3) Referenciamos vistas del layout (por id)
        tvCorreo    = findViewById(R.id.tvCorreo);            // Texto con el correo del usuario
        tvUbicacion = findViewById(R.id.tvUbicacion);         // Texto con lat/lon formateado
        tvHora      = findViewById(R.id.tvHora);              // Texto con fecha/hora formateada
        Button btnUb     = findViewById(R.id.btnUbicacion);   // Botón "Actualizar / Guardar ubicación"
        Button btnMapa   = findViewById(R.id.btnVerEnMapa);   // Botón "Ver en Google Maps"
        Button btnCalc   = findViewById(R.id.btnAbrirCalculadora); // Botón "Ir a Calculadora"
        Button btnLogout = findViewById(R.id.btnLogout);      // Botón "Cerrar sesión"
        Button btnTemp = findViewById(R.id.btnTemperatura);   // Botón "Temperatura"
        Button btnProd = findViewById(R.id.btnProductos);     // Botón "Productos"
        Button btnDespacho = findViewById(R.id.btnDespacho);  // Botón "despacho"

        // 4) Header: mostramos correo (si existe) en la parte superior
        String correo = (user.getEmail() != null) ? user.getEmail() : "(sin correo)";
        tvCorreo.setText("Correo: " + correo);                // Renderiza el correo

        // 5) Acción: al tocar "Actualizar / Guardar ubicación" pedimos un fix y guardamos en Firebase
        btnUb.setOnClickListener(v -> solicitarUbicacion(user.getUid())); // Pasa el UID

        // 6) Acción: abrir Google Maps centrado en la última ubicación guardada
        btnMapa.setOnClickListener(v -> {
            // Validamos que ya tengamos coordenadas
            if (lastLat == null || lastLon == null) {
                Toast.makeText(this, "Aún no hay coordenadas para mostrar", Toast.LENGTH_SHORT).show();
                return; // Salimos si no hay datos
            }
            // geo:lat,lon?q=lat,lon(Título)
            String geo = "geo:" + lastLat + "," + lastLon
                    + "?q=" + lastLat + "," + lastLon + "(Mi ubicación guardada)";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(geo))); // Abre Maps
        });

        // 7) Acción: abrir la Calculadora (MainActivity)
        btnCalc.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        // 8) Abrir TemperaturaActivity
        btnTemp.setOnClickListener(v ->
                startActivity(new Intent(this, TemperaturaActivity.class)));
        // 9) Abrir ProductosActivity
        btnProd.setOnClickListener(v ->
                startActivity(new Intent(this, ProductosActivity.class)));
        // 10) Acción: abrir despacho
        btnDespacho.setOnClickListener(v ->
                startActivity(new Intent(this, DespachoActivity.class)));
        // 11) Acción: cerrar sesión y volver al Login
        btnLogout.setOnClickListener(v -> {
            auth.signOut();                                        // Cierra la sesión en Firebase
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show(); // Feedback
            startActivity(new Intent(this, LoginActivity.class));   // Vuelve al Login
            finish();                                               // Evita volver con "atrás"
        });
    }

    @Override
    protected void onStart() {               // Se llama cada vez que la Activity entra en pantalla
        super.onStart();                     // Llama a la superclase
        leerUltimaUbicacionDesdeFirebase();  // Trae (si existe) la última ubicación guardada
    }

    // ======= Ubicación: MISMO ENFOQUE QUE EN MainActivity (last known + single update) =======

    private void solicitarUbicacion(String uid) { // Se ejecuta al tocar el botón "Actualizar ubicación"
        Log.d(TAG, "Solicitando ubicación…");     // Log para ver el flujo

        // 1) Verificamos el permiso de ubicación fina
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Si falta el permiso, lo solicitamos (diálogo del sistema)
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQ_LOC);
            return; // Salimos y esperamos el callback onRequestPermissionsResult
        }

        // 2) Intentamos primero con la última ubicación conocida (si existe, es instantánea)
        Location last = getLastKnownFineLocation(); // Recorre proveedores activos
        if (last != null) {                         // Si hay una ubicación en caché…
            Log.d(TAG, "LastKnown OK: " + last.getLatitude() + "," + last.getLongitude());
            procesarYGuardar(uid, last);           // Guardamos en Firebase y actualizamos UI
            return;                                 // Ya terminamos
        }

        // 3) Si no hay last known, pedimos un "fix" único con alta precisión
        try {
            Criteria c = new Criteria();               // Criterio de proveedor
            c.setAccuracy(Criteria.ACCURACY_FINE);     // Precisión fina (GPS si está disponible)
            String provider = locationManager.getBestProvider(c, true); // Mejor proveedor activo

            if (provider != null) {                    // Verificamos que exista proveedor
                Toast.makeText(this, "Obteniendo ubicación…", Toast.LENGTH_SHORT).show(); // Feedback
                locationManager.requestSingleUpdate(provider, new LocationListener() {     // Pide un fix único
                    @Override
                    public void onLocationChanged(Location location) { // Llega una ubicación
                        Log.d(TAG, "requestSingleUpdate OK: "
                                + location.getLatitude() + "," + location.getLongitude());
                        procesarYGuardar(uid, location);              // Guardar + UI
                    }
                    @Override public void onStatusChanged(String p, int s, Bundle b) { /* No usado */ }
                    @Override public void onProviderEnabled(String p) { /* No usado */ }
                    @Override public void onProviderDisabled(String p) { /* No usado */ }
                }, null);
            } else {
                // No hay proveedor activo (GPS apagado / sin Google APIs / etc.)
                Toast.makeText(this, "Sin proveedor de ubicación activo", Toast.LENGTH_SHORT).show();
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
                Location l = locationManager.getLastKnownLocation(p); // Última ubicación de ese proveedor
                if (l != null) return l;                          // Si hay una válida, la retorno
            }
        } catch (SecurityException ignored) { /* Si llega sin permiso, vuelve null */ }
        return null; // No hay last known
    }

    // Actualiza UI + guarda {lat, lon, timestamp} en /users/{uid}/lastLocation
    private void procesarYGuardar(String uid, Location location) {
        double lat = location.getLatitude();      // Obtiene latitud
        double lon = location.getLongitude();     // Obtiene longitud
        long ts = System.currentTimeMillis();     // Timestamp actual (ms desde 1970)

        lastLat = lat;                            // Guarda en memoria para "Ver en mapa"
        lastLon = lon;

        tvUbicacion.setText(String.format("Ubicación: %.6f, %.6f", lat, lon)); // Muestra en la UI
        tvHora.setText("Última actualización: " + formatearHora(ts));          // Muestra hora legible

        UserLocation ul = new UserLocation(lat, lon, ts); // Objeto plano para Firebase

        // Escribimos en Realtime Database: /users/{uid}/lastLocation = ul
        dbRef.child(uid).child("lastLocation").setValue(ul)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Ubicación guardada en Firebase", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Lee la última ubicación guardada para rellenar la UI al entrar al menú
    private void leerUltimaUbicacionDesdeFirebase() {
        FirebaseUser user = auth.getCurrentUser(); // Usuario actual
        if (user == null) return;                  // Si no hay, no seguimos

        // Leemos /users/{uid}/lastLocation una vez
        dbRef.child(user.getUid()).child("lastLocation").get()
                .addOnSuccessListener(snap -> {    // Si la lectura fue exitosa…
                    if (!snap.exists()) {          // Si no hay datos previos…
                        tvUbicacion.setText("Ubicación: (sin datos)"); // Mensaje por defecto
                        tvHora.setText("Última actualización: —");     // Guion largo
                        lastLat = null; lastLon = null;                // Limpia coordenadas
                        return;                                        // Salimos
                    }
                    // Convierto el snapshot a nuestro POJO UserLocation
                    UserLocation ul = snap.getValue(UserLocation.class);
                    if (ul != null) {              // Si la conversión fue correcta
                        lastLat = ul.lat;          // Guarda en memoria
                        lastLon = ul.lon;
                        // Muestra en UI
                        tvUbicacion.setText(String.format("Ubicación: %.6f, %.6f", ul.lat, ul.lon));
                        tvHora.setText("Última actualización: " + formatearHora(ul.timestamp));
                    }
                })
                .addOnFailureListener(e ->         // Si falla la lectura…
                        Toast.makeText(this, "Error al leer ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Convierte un timestamp (ms) a una fecha/hora legible según configuración de sistema
    private String formatearHora(long ts) {
        DateFormat df = DateFormat.getDateTimeInstance(); // Usa formato local (p. ej. 24h/12h)
        return df.format(new Date(ts));                   // Devuelve texto legible
    }

    // Callback del resultado al pedir permisos (se llama después del diálogo del sistema)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Pasa a la superclase
        if (requestCode == REQ_LOC) { // Chequea que sea nuestra solicitud
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el usuario aceptó, intentamos obtener y guardar ubicación nuevamente
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) solicitarUbicacion(user.getUid());
            } else {
                // Si lo denegó, avisamos con un Toast
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
