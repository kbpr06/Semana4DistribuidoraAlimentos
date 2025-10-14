package com.example.semana4distribuidoraalimentos;

// ===== IMPORTS ANDROID =====
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// ===== IMPORTS FIREBASE CORE/AUTH =====
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

// ===== IMPORTS GOOGLE SIGN-IN =====
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

public class LoginActivity extends AppCompatActivity {

    // ===== Referencias a la UI =====
    private EditText etEmail;
    private EditText etPass;
    private Button btnLogin;
    private Button btnRegister;
    private Button btnGoogle;      // NUEVO: botón Google
    private TextView tvForgot;
    private ProgressBar progress;

    // ===== Firebase =====
    private FirebaseAuth auth;

    // ===== Google Sign-In =====
    private GoogleSignInClient googleClient;        // Cliente de Google
    private static final int RC_SIGN_IN = 100;      // Código de resultado del intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Infla el layout moderno

        // --- Guard de inicialización de Firebase (por si la auto-init falla) ---
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        // 1) Instancia de autenticación
        auth = FirebaseAuth.getInstance();

        // 2) Vinculamos controles del XML por id
        etEmail     = findViewById(R.id.etEmail);
        etPass      = findViewById(R.id.etPass);
        btnLogin    = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogle   = findViewById(R.id.btnGoogle); // NUEVO
        tvForgot    = findViewById(R.id.tvForgot);
        progress    = findViewById(R.id.progress);

        // 3) Si ya existe una sesión activa, saltamos directo al Menú
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            irAMenu();
            return;
        }

        // === CONFIGURACIÓN GOOGLE SIGN-IN ===
        // 4) Opciones: pedimos ID Token (para Firebase) y correo del usuario
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                // default_web_client_id viene de strings.xml (Firebase Console)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // 5) Creamos el cliente de Google con esas opciones
        googleClient = GoogleSignIn.getClient(this, gso);

        // 6) Click en "Continuar con Google": lanzamos el flujo de Google
        btnGoogle.setOnClickListener(v -> {
            mostrarCargando(true);
            signInWithGoogle();
        });

        // === TUS LISTENERS EXISTENTES (EMAIL/PASS) ===
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPass.getText().toString().trim();
            if (!validarCampos(email, pass)) return;
            mostrarCargando(true);
            signIn(email, pass);
        });

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPass.getText().toString().trim();
            if (!validarCampos(email, pass)) return;
            mostrarCargando(true);
            createAccount(email, pass);
        });

        tvForgot.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Ingresa un correo válido");
                etEmail.requestFocus();
                return;
            }
            mostrarCargando(true);
            resetPassword(email);
        });
    }

    // ====== GOOGLE: lanza la pantalla de selección de cuenta ======
    private void signInWithGoogle() {
        Intent signInIntent = googleClient.getSignInIntent(); // Intent oficial
        startActivityForResult(signInIntent, RC_SIGN_IN);     // Esperamos resultado
    }

    // ====== Resultado del flujo de Google ======
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // Tarea que contiene la cuenta seleccionada (o error)
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Si no hay excepción, obtenemos la cuenta
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Obtenemos el ID Token para autenticarnos en Firebase
                String idToken = account.getIdToken();
                firebaseAuthWithGoogle(idToken);
            } catch (ApiException e) {
                mostrarCargando(false);
                Toast.makeText(this, "Error al iniciar con Google: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ====== Autenticar en Firebase usando credencial de Google ======
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    mostrarCargando(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Inicio con Google exitoso", Toast.LENGTH_SHORT).show();
                        irAMenu();
                    } else {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Error desconocido";
                        Toast.makeText(this, "Fallo al autenticar: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ===== Validación de campos email/pass =====
    private boolean validarCampos(String email, String pass) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Correo inválido");
            etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            etPass.setError("Contraseña mínima de 6 caracteres");
            etPass.requestFocus();
            return false;
        }
        return true;
    }

    // ===== Control visual de carga =====
    private void mostrarCargando(boolean cargando) {
        progress.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!cargando);
        btnRegister.setEnabled(!cargando);
        btnGoogle.setEnabled(!cargando);
        tvForgot.setEnabled(!cargando);
        etEmail.setEnabled(!cargando);
        etPass.setEnabled(!cargando);
    }

    // ===== Registro (Firebase email/pass) =====
    private void createAccount(String email, String pass) {
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    mostrarCargando(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show();
                        irAMenu();
                    } else {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Error";
                        Toast.makeText(this, "No se pudo crear: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ===== Inicio de sesión (Firebase email/pass) =====
    private void signIn(String email, String pass) {
        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    mostrarCargando(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show();
                        irAMenu();
                    } else {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Error";
                        Toast.makeText(this, "Login fallido: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ===== Recuperación de contraseña =====
    private void resetPassword(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    mostrarCargando(false);
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
        startActivity(new Intent(this, MenuActivity.class));
        finish();
    }
}
