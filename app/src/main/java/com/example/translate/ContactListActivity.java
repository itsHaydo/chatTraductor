package com.example.translate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ContactListActivity extends AppCompatActivity {

    private static final String TAG = "ContactListActivity";
    private ListView contactListView;
    private ArrayAdapter<String> adapter;
    private List<String> contactList;
    private List<String> userIds;
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        contactListView = findViewById(R.id.contactListView);
        contactList = new ArrayList<>();
        userIds = new ArrayList<>();
        firestoreHelper = new FirestoreHelper();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactList);
        contactListView.setAdapter(adapter);

        loadContacts();

        contactListView.setOnItemClickListener((adapterView, view, position, id) -> {
            String selectedUserId = userIds.get(position);
            String conversationId = generateConversationId(selectedUserId);

            checkAndCreateConversation(conversationId, selectedUserId);
        });
    }

    private String generateConversationId(String selectedUserId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "User ID not found.");
            return null;
        }
        return currentUserId.compareTo(selectedUserId) < 0
                ? currentUserId + "_" + selectedUserId
                : selectedUserId + "_" + currentUserId;
    }

    private String getCurrentUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("userId", null);  // Devuelve null si no se encuentra el ID
    }

    private void loadContacts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String username = document.getString("username");
                    String userId = document.getId();
                    contactList.add(username);
                    userIds.add(userId);
                }
                adapter.notifyDataSetChanged();
            } else {
                Log.w(TAG, "Error getting contacts.", task.getException());
            }
        });
    }

    private void checkAndCreateConversation(String conversationId, String selectedUserId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "Current user ID not found.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("conversaciones").document(conversationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Crea una nueva conversación si no existe
                        List<String> participants = new ArrayList<>();
                        participants.add(selectedUserId);
                        participants.add(currentUserId);
                        firestoreHelper.createConversation(participants);
                    }

                    // Iniciar ChatActivity con la conversación creada o existente
                    Intent intent = new Intent(ContactListActivity.this, ChatActivity.class);
                    intent.putExtra("userId", selectedUserId);
                    intent.putExtra("conversationId", conversationId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error checking conversation", e));
    }
}
