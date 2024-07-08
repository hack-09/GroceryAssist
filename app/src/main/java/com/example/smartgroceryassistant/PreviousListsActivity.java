package com.example.smartgroceryassistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PreviousListsActivity extends AppCompatActivity {

    private static final String TAG = "PreviousListsActivity";
    private ListView previousListsView;
    private ArrayAdapter<String> adapter;
    private List<String> previousLists;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_lists);

        previousListsView = findViewById(R.id.previous_lists_view);
        previousLists = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.item_previous_list, R.id.previous_list_date, previousLists);
        previousListsView.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        loadPreviousListsFromFirebase();

        previousListsView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedListDate = previousLists.get(position);
            Log.d(TAG, "Clicked on date: " + selectedListDate);
            Intent intent = new Intent(PreviousListsActivity.this, ListDetailsActivity.class);
            intent.putExtra("SELECTED_DATE", selectedListDate);
            startActivity(intent);
        });

        previousListsView.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(PreviousListsActivity.this)
                    .setTitle("Delete List")
                    .setMessage("Are you sure you want to delete this list?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteListFromFirebase(position))
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        });
    }

    private void loadPreviousListsFromFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(userId).child("previousLists");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    previousLists.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String date = snapshot.getKey();
                        previousLists.add(date);
                    }
                    Log.d(TAG, "Loaded previous lists: " + previousLists);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(PreviousListsActivity.this, "Failed to load previous lists.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(PreviousListsActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteListFromFirebase(int position) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String date = previousLists.get(position);

            DatabaseReference userRef = databaseReference.child("users").child(userId).child("previousLists").child(date);
            userRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(PreviousListsActivity.this, "List deleted successfully!", Toast.LENGTH_SHORT).show();
                    previousLists.remove(position);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(PreviousListsActivity.this, "Failed to delete list.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(PreviousListsActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }
}
