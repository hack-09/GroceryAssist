package com.example.smartgroceryassistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class ListDetailsActivity extends AppCompatActivity {

    private static final String TAG = "ListDetailsActivity";
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> itemList;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_details);

        listView = findViewById(R.id.list_view);
        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        listView.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        selectedDate = getIntent().getStringExtra("SELECTED_DATE");

        loadSpecificListFromFirebase(selectedDate);

        ImageView shareButton = findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareList(itemList);
            }
        });
    }

    private void loadSpecificListFromFirebase(String date) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(userId).child("previousLists").child(date);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    itemList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String item = snapshot.getValue(String.class);
                        itemList.add(item);
                    }
                    Log.d(TAG, "Loaded list for " + date + ": " + itemList);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "loadSpecificListFromFirebase:onCancelled", databaseError.toException());
                    Toast.makeText(ListDetailsActivity.this, "Failed to load list.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(ListDetailsActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareList(ArrayList<String> shoppingList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("List of Grocery\n");
        for (String item : shoppingList) {
            stringBuilder.append(item).append("\n");
        }
        String shareText = stringBuilder.toString().trim(); // Trim to remove trailing newline

        // Create Intent to share text
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share list via"));
    }
}
