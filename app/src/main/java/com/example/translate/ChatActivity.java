package com.example.translate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private ListView messagesListView;
    private EditText messageEditText;
    private Button sendButton;
    private ArrayAdapter<String> adapter;
    private List<String> messagesList;
    private FirebaseFirestore db;
    private String conversationId;
    private String currentUserId;
    private String recipientUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messagesListView = findViewById(R.id.messagesListView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        messagesList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messagesList);
        messagesListView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Obtener conversationId y userId del Intent
        conversationId = getIntent().getStringExtra("conversationId");
        recipientUserId = getIntent().getStringExtra("userId");
        currentUserId = getCurrentUserId();

        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private String getCurrentUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("userId", null);  // Devuelve null si no se encuentra el ID
    }

    private void loadMessages() {
        if (conversationId == null) {
            Log.w(TAG, "Conversation ID is null.");
            return;
        }

        // Obtener el idioma preferido del usuario actual
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentUserLanguage = documentSnapshot.getString("preferred_language_code");

                        // Cargar mensajes de la conversación
                        db.collection("conversaciones").document(conversationId)
                                .collection("mensajes")
                                .orderBy("timestamp", Query.Direction.ASCENDING)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            String originalMessage = document.getString("originalMessage");
                                            String senderId = document.getString("senderId");

                                            // Generar el campo de traducción específico para el idioma preferido
                                            String translationField = "translatedMessage_" + currentUserLanguage;
                                            String translatedMessage = document.getString(translationField);

                                            // Decidir qué mensaje mostrar dependiendo del idioma del usuario
                                            String messageToDisplay;
                                            if (senderId.equals(currentUserId)) {
                                                // Si el usuario actual es el remitente, muestra el mensaje original
                                                messageToDisplay = "Yo: " + originalMessage;
                                            } else {
                                                // Si el usuario actual es el receptor, muestra el mensaje traducido en su idioma preferido
                                                messageToDisplay = "El: " + (translatedMessage != null ? translatedMessage : originalMessage);
                                            }

                                            messagesList.add(messageToDisplay);
                                        }
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        Log.w(TAG, "Error getting messages.", task.getException());
                                    }
                                });
                    } else {
                        Log.w(TAG, "No preferred language found for current user.");
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error getting current user's language preference", e));
    }



    private String translateText(String text, String targetLanguage) {
        OkHttpClient client = new OkHttpClient();
        String apiKey = "AIzaSyAuMDPZfK7bIDyTjUXCPL__vC2RLOJ69b0"; // Reemplaza con tu clave de API
        String url = "https://translation.googleapis.com/language/translate/v2?q=" + text + "&target=" + targetLanguage + "&key=" + apiKey;

        Request request = new Request.Builder().url(url).build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject jsonObject = new JSONObject(response.body().string());
                return jsonObject
                        .getJSONObject("data")
                        .getJSONArray("translations")
                        .getJSONObject(0)
                        .getString("translatedText");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text; // Retorna el texto original en caso de fallo
    }


    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (messageText.isEmpty() || conversationId == null) {
            return;
        }

        // Lista de idiomas a los que quieres traducir
        String[] languages = {"es", "en", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko"};

        // Obtener el idioma preferido del receptor
        db.collection("users").document(recipientUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        new Thread(() -> {
                            // Mapa para almacenar el mensaje y las traducciones
                            Map<String, Object> message = new HashMap<>();
                            message.put("senderId", currentUserId);
                            message.put("recipientId", recipientUserId);
                            message.put("originalMessage", messageText);
                            message.put("timestamp", System.currentTimeMillis());

                            // Traducir el mensaje a cada idioma y agregar al mapa
                            for (String language : languages) {
                                String translatedMessage = translateText(messageText, language);
                                message.put("translatedMessage_" + language, translatedMessage);
                            }

                            // Guardar el mensaje con todas las traducciones en Firestore
                            db.collection("conversaciones").document(conversationId)
                                    .collection("mensajes")
                                    .add(message)
                                    .addOnSuccessListener(documentReference -> runOnUiThread(() -> {
                                        messagesList.add("Yo: " + messageText); // Mostrar mensaje original en el UI
                                        adapter.notifyDataSetChanged();
                                        messageEditText.setText("");
                                    }))
                                    .addOnFailureListener(e -> Log.w(TAG, "Error sending message", e));
                        }).start();
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error getting recipient's language preference", e));
    }

}
