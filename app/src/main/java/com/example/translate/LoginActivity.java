package com.example.translate;// LoginActivity.java

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmailOrUsername, etPassword;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmailOrUsername = findViewById(R.id.etEmailOrUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> performLogin());
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String emailOrUsername = etEmailOrUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(emailOrUsername) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Ingrese usuario/email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.verifyUser(emailOrUsername, password, this, isSuccess -> {  // Pasa `this` como contexto
            if (isSuccess) {
                Intent intent = new Intent(LoginActivity.this, ContactListActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
