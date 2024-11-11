package com.example.translate;// RegisterActivity.java

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private FirestoreHelper firestoreHelper;
    private EditText etUsername, etEmail, etPassword;
    private Spinner spinnerLanguage;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firestoreHelper = new FirestoreHelper();
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        btnRegister = findViewById(R.id.btnRegister);

        // Llenar el spinner con idiomas
        setupLanguageSpinner();

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void setupLanguageSpinner() {
        // Lista de idiomas y códigos
        String[] languages = {
                "Español (es)", "Inglés (en)", "Francés (fr)", "Alemán (de)",
                "Italiano (it)", "Portugués (pt)", "Ruso (ru)", "Chino (zh)",
                "Japonés (ja)", "Coreano (ko)"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Obtener el idioma seleccionado y extraer el código
        String selectedLanguage = spinnerLanguage.getSelectedItem().toString();
        String languageCode = selectedLanguage.substring(selectedLanguage.indexOf('(') + 1, selectedLanguage.indexOf(')'));

        firestoreHelper.createUser(username, email, languageCode, password);
        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
        finish();
    }
}
