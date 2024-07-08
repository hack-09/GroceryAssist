package com.example.smartgroceryassistant;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText itemInput;
    private static final int REQUEST_CODE_VOICE_INPUT = 1000;
    private ImageButton voiceInputButton;
    private Button addButton, showPreviousListButton, saveListButton, fetchRecommendationsButton;
    private RecyclerView recommendationsRecyclerView;
    private RecommendationsAdapter recommendationsAdapter;
    private Gson gson = new Gson();
    private ListView shoppingListView;
    private ListView recommendationsListView;
    private ArrayList<String> shoppingList;
    private ArrayAdapter<String> adapter;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private static final String TAG = "MainActivity";
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        itemInput = findViewById(R.id.item_input);
        voiceInputButton = findViewById(R.id.voiceInputButton);
        addButton = findViewById(R.id.add_button);
        shoppingListView = findViewById(R.id.shopping_list);
        showPreviousListButton = findViewById(R.id.show_previous_list_button);
        saveListButton = findViewById(R.id.save_list_button);
        fetchRecommendationsButton = findViewById(R.id.btn_fetch_recommendations);
        recommendationsRecyclerView = findViewById(R.id.recommendations_recycler_view);

        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recommendationsAdapter = new RecommendationsAdapter(new ArrayList<>());
        recommendationsRecyclerView.setAdapter(recommendationsAdapter);

        shoppingList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, shoppingList);
        shoppingListView.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up the sign-in button
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    // User is already signed in, show sign-out option
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Sign Out")
                            .setMessage("You are already signed in as " + user.getDisplayName() + ". Do you want to sign out?")
                            .setPositiveButton("Sign Out", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    signOut();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    // User is not signed in, proceed with sign-in
                    signIn();
                }
            }
        });

        voiceInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceInput();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String item = itemInput.getText().toString();
                if (!item.isEmpty()) {
                    shoppingList.add(item);
                    adapter.notifyDataSetChanged();
                    saveShoppingListToFirebase();
                    itemInput.setText("");

                    fetchRecommendations(item);
                }
            }
        });

        fetchRecommendationsButton.setOnClickListener(v -> {
            String query = itemInput.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                fetchRecommendations(query);
            } else {
                Toast.makeText(MainActivity.this, "Please enter an item to get recommendations", Toast.LENGTH_SHORT).show();
            }
        });

        saveListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveShoppingListToPreviousLists();
            }
        });

        showPreviousListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PreviousListsActivity.class);
                startActivity(intent);
            }
        });

        shoppingListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to delete this item?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                shoppingList.remove(position);
                                adapter.notifyDataSetChanged();
                                saveShoppingListToFirebase();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
        });

        loadCurrentListFromFirebase();
    }

    private void saveShoppingListToFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(userId).child("currentList");
            userRef.setValue(shoppingList).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "List saved successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to save list.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveShoppingListToPreviousLists() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Format the current date as a readable string
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDateAndTime = sdf.format(new Date());

            DatabaseReference userRef = databaseReference.child("users").child(userId).child("previousLists").child(currentDateAndTime);
            userRef.setValue(shoppingList).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "List saved to previous lists successfully!", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    saveShoppingListToFirebase();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to save list to previous lists.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadCurrentListFromFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(userId).child("currentList");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    shoppingList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String item = snapshot.getValue(String.class);
                        shoppingList.add(item);
                    }
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Current list loaded from Firebase: " + shoppingList);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "loadCurrentListFromFirebase:onCancelled", databaseError.toException());
                    Toast.makeText(MainActivity.this, "Failed to load list.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            updateUI(null);
            Toast.makeText(MainActivity.this, "Signed out successfully!", Toast.LENGTH_SHORT).show();
        });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the item name...");
        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }

        if (requestCode == REQUEST_CODE_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                itemInput.setText(result.get(0));
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            loadCurrentListFromFirebase();
                        } else {
                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "Signed in as: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchRecommendations(String query) {
        String url = "https://grocery-pricing-api.p.rapidapi.com/searchGrocery?keyword=" + query + "&perPage=10&page=1";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", "702e0722a1mshbfa914a9a32242fp117467jsnb9c657ff2434")
                .addHeader("x-rapidapi-host", "grocery-pricing-api.p.rapidapi.com")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API request failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to get recommendations", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    parseAndDisplayRecommendations(jsonResponse);
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to get recommendations", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void parseAndDisplayRecommendations(String jsonResponse) {
        RecommendationResponse recommendationResponse = gson.fromJson(jsonResponse, RecommendationResponse.class);
        List<Recommendation> recommendations = recommendationResponse.getHits();
        runOnUiThread(() -> recommendationsAdapter.updateRecommendations(recommendations));
    }

    static class RecommendationResponse {
//        @SerializedName("hits")
        private List<Recommendation> hits;

        public List<Recommendation> getHits() {
            return hits;
        }
    }

    static class Recommendation {
        @SerializedName("id")
        private String id;
        @SerializedName("name")
        private String name;
        @SerializedName("priceInfo")
        private PriceInfo priceInfo;
        @SerializedName("image")
        private String imageUrl;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public PriceInfo getPriceInfo() {
            return priceInfo;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        static class PriceInfo {
            @SerializedName("itemPrice")
            private String itemPrice;
            @SerializedName("linePriceDisplay")
            private String linePriceDisplay;

            public String getItemPrice() {
                return itemPrice;
            }

            public String getLinePriceDisplay() {
                return linePriceDisplay;
            }
        }
    }

}
