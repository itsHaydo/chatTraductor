package com.example.translate;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";
    private FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void createUser(String username, String email, String preferredLanguageCode, String password) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("password", password);
        user.put("preferred_language_code", preferredLanguageCode);
        user.put("created_at", FieldValue.serverTimestamp());

        db.collection("users").add(user)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Usuario añadido con ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error al añadir usuario", e));
    }

    public void createConversation(List<String> userIds) {
        Map<String, Object> conversation = new HashMap<>();
        conversation.put("created_at", FieldValue.serverTimestamp());

        db.collection("conversaciones").add(conversation)
                .addOnSuccessListener(documentReference -> {
                    String conversationId = documentReference.getId();
                    Log.d(TAG, "Conversación creada con ID: " + conversationId);

                    for (String userId : userIds) {
                        addParticipantToConversation(conversationId, userId);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error al crear conversación", e));
    }

    public void addParticipantToConversation(String conversationId, String userId) {
        Map<String, Object> participant = new HashMap<>();
        participant.put("user_id", userId);
        participant.put("joined_at", FieldValue.serverTimestamp());

        db.collection("conversaciones").document(conversationId)
                .collection("participantes").add(participant)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Participante añadido con ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error al añadir participante", e));
    }

    public void sendMessage(String conversationId, String userId, String messageText, String languageCode) {
        Map<String, Object> message = new HashMap<>();
        message.put("user_id", userId);
        message.put("message_text", messageText);
        message.put("language_code", languageCode);
        message.put("created_at", FieldValue.serverTimestamp());

        db.collection("conversaciones").document(conversationId)
                .collection("mensajes").add(message)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Mensaje enviado con ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error al enviar mensaje", e));
    }

    public void addTranslation(String conversationId, String messageId, String translatedText, String targetLanguageCode) {
        Map<String, Object> translation = new HashMap<>();
        translation.put("translated_text", translatedText);
        translation.put("target_language_code", targetLanguageCode);
        translation.put("created_at", FieldValue.serverTimestamp());

        db.collection("conversaciones").document(conversationId)
                .collection("mensajes").document(messageId)
                .collection("traducciones").add(translation)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Traducción añadida con ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error al añadir traducción", e));
    }

    public void verifyUser(String emailOrUsername, String password, Context context, FirestoreCallback callback) {
        db.collection("users")
                .whereEqualTo("email", emailOrUsername)
                .whereEqualTo("password", password)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String userId = task.getResult().getDocuments().get(0).getId();

                        // Guardar el ID del usuario en SharedPreferences
                        SharedPreferences sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("userId", userId);
                        editor.apply();

                        callback.onCallback(true);
                    } else {
                        callback.onCallback(false);
                    }
                })
                .addOnFailureListener(e -> callback.onCallback(false));
    }

    public interface FirestoreCallback {
        void onCallback(boolean isSuccess);
    }


}