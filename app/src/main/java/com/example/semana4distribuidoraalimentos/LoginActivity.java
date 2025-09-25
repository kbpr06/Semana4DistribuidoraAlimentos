package com.example.semana4distribuidoraalimentos; // Paquete de la app (debe coincidir con el de tu proyecto)

// ===== IMPORTS =====
import android.content.Intent;          // Para abrir MenuActivity
import android.os.Bundle;               // Ciclo de vida (onCreate)
import android.text.TextUtils;          // Utilidades para validar textos vacíos
import android.util.Patterns;           // Validador de formato de email
import android.view.View;               // Controlar visibilidad del ProgressBar
import android.widget.Button;           // Botones de la UI
import android.widget.EditText;         // Inputs de texto
import android.widget.ProgressBar;      // Indicador de carga
import android.widget.TextView;         // Texto clickeable (recuperar contraseña)
import android.widget.Toast;            // Mensajes cortos en pantalla

import androidx.appcompat.app.AppCompatActivity; // Clase base de Activities

import com.google.android.gms.tasks.Task;     // Resultado asíncrono (reset password)
import com.google.firebase.FirebaseApp;       // Inicialización de Firebase (guard)
import com.google.firebase.auth.FirebaseAuth; // Autenticación de Firebase
import com.google.firebase.auth.FirebaseUser; // Representa al usuario autenticado

public class LoginActivity extends AppCompatActivity {

    // ===== Referencias a la UI =====
    private EditText etEmail;     // Campo para correo
    private EditText etPass;      // Campo para contraseña
    private Button btnLogin;      // Botón "Iniciar sesión"
    private Button btnRegister;   // Botón "Crear cuenta"
    private TextView tvForgot;    // Texto "¿Olvidaste tu contraseña?"
    private ProgressBar progress; // Barra de progreso simple

    // ===== Firebase =====
    private FirebaseAuth auth;    // Punto de entrada para Auth

    @Override
    protected void onCreate(Bundle savedInstanceState) {  // Método llamado al crear la Activity
        super.onCreate(savedInstanceState);               // Llama a la implementación base
        setContentView(R.layout.activity_login);          // Infla el layout de esta pantalla

        // --- Guard de inicialización (útil si alguna vez falla la auto-init) ---
        if (FirebaseApp.getApps(this).isEmpty()) {        // ¿No hay app de Firebase registrada?
            FirebaseApp.initializeApp(this);              // -> Inicializa Firebase manualmente
        }

        // 1) Instancia de autenticación
        auth = FirebaseAuth.getInstance();                // Obtiene la instancia global de Auth

        // 2) Vinculamos controles del XML por id
        etEmail     = findViewById(R.id.etEmail);         // Input correo
        etPass      = findViewById(R.id.etPass);          // Input contraseña
        btnLogin    = findViewById(R.id.btnLogin);        // Botón login
        btnRegister = findViewById(R.id.btnRegister);     // Botón registro
        tvForgot    = findViewById(R.id.tvForgot);        // Enlace recuperar contraseña
        progress    = findViewById(R.id.progress);        // Barra de progreso

        // 3) Si ya existe una sesión activa, saltamos directo al Menú
        FirebaseUser current = auth.getCurrentUser();     // Usuario actualmente logueado (si existe)
        if (current != null) {                            // Si no es null → ya hay sesión
            irAMenu();                                    // Abrimos el Menú
            return;                                       // Evitamos que se muestre el login
        }

        // 4) Click en "Iniciar sesión"
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim(); // Leemos el correo (sin espacios)
            String pass  = etPass.getText().toString().trim();  // Leemos la contraseña
            if (!validarCampos(email, pass)) return;            // Si no pasa validación → corto
            mostrarCargando(true);                              // Muestro progreso y deshabilito UI
            signIn(email, pass);                                // Intento de login en Firebase
        });

        // 5) Click en "Crear cuenta"
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim(); // Leemos correo
            String pass  = etPass.getText().toString().trim();  // Leemos contraseña
            if (!validarCampos(email, pass)) return;            // Validación básica
            mostrarCargando(true);                              // Muestro progreso
            createAccount(email, pass);                         // Intento de registro en Firebase
        });

        // 6) Click en "¿Olvidaste tu contraseña?"
        tvForgot.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();                   // Necesitamos el correo
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Ingresa un correo válido");                     // Marca error en campo
                etEmail.requestFocus();                                           // Lleva el foco al input
                return;                                                           // No continúo
            }
            mostrarCargando(true);                                               // Muestro progreso
            resetPassword(email);                                                // Envío mail de recuperación
        });
    }

    // ===== Validación de campos =====
    private boolean validarCampos(String email, String pass) {
        // Valida formato de correo
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Correo inválido");   // Mensaje en el input
            etEmail.requestFocus();                // Foco al input
            return false;                          // Corta el flujo
        }
        // Valida longitud mínima de contraseña (requisito de Firebase: 6+)
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            etPass.setError("Contraseña mínima de 6 caracteres");
            etPass.requestFocus();
            return false;
        }
        return true; // OK
    }

    // ===== Control visual de carga (deshabilita/rehabilita la UI) =====
    private void mostrarCargando(boolean cargando) {
        progress.setVisibility(cargando ? View.VISIBLE : View.GONE); // Muestra/oculta ProgressBar
        btnLogin.setEnabled(!cargando);                               // Evita doble click
        btnRegister.setEnabled(!cargando);                            // Evita doble click
        tvForgot.setEnabled(!cargando);                               // Bloquea interacciones
    }

    // ===== Registro de cuenta =====
    private void createAccount(String email, String pass) {
        auth.createUserWithEmailAndPassword(email, pass)        // Llama a Firebase (async)
                .addOnCompleteListener(this, task -> {              // Callback cuando termina
                    mostrarCargando(false);                         // Oculta progreso siempre
                    if (task.isSuccessful()) {                      // Éxito
                        Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show();
                        irAMenu();                                  // Ir al Menú
                    } else {                                        // Error
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Error";
                        Toast.makeText(this, "No se pudo crear: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ===== Inicio de sesión =====
    private void signIn(String email, String pass) {
        auth.signInWithEmailAndPassword(email, pass)            // Llama a Firebase (async)
                .addOnCompleteListener(this, task -> {              // Callback cuando termina
                    mostrarCargando(false);                         // Oculta progreso
                    if (task.isSuccessful()) {                      // Éxito
                        Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show();
                        irAMenu();                                  // Ir al Menú
                    } else {                                        // Error
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Error";
                        Toast.makeText(this, "Login fallido: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ===== Recuperación de contraseña (envía email) =====
    private void resetPassword(String email) {
        Task<Void> t = auth.sendPasswordResetEmail(email);      // Pide a Firebase el envío del correo
        t.addOnCompleteListener(task -> {                       // Callback
            mostrarCargando(false);                             // Oculta progreso
            if (task.isSuccessful()) {
                Toast.makeText(this, "Email de recuperación enviado", Toast.LENGTH_LONG).show();
            } else {
                String msg = (task.getException() != null)
                        ? task.getException().getMessage()
                        : "Error";
                Toast.makeText(this, "No se pudo enviar: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ===== Navegación al Menú =====
    private void irAMenu() {
        startActivity(new Intent(this, MenuActivity.class)); // Abre MenuActivity
        finish();                                            // Cierra el Login (no vuelve con “atrás”)
    }
}
