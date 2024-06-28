package com.example.smartgroceryassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText itemInput;
    private Button addButton;
    private ListView shoppingListView;
    private ListView recommendationsListView;
    private ArrayList<String> shoppingList;
    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> recommendationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        itemInput = findViewById(R.id.item_input);
        addButton = findViewById(R.id.add_button);
        shoppingListView = findViewById(R.id.shopping_list);
        recommendationsListView = findViewById(R.id.recommendations_list);

        shoppingList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, shoppingList);
        shoppingListView.setAdapter(adapter);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String item = itemInput.getText().toString();
                if (!item.isEmpty()) {
                    shoppingList.add(item);
                    adapter.notifyDataSetChanged();
                    itemInput.setText("");

                    List<Product> recommendations = ProductDatabase.getProductRecommendations(item);
                    List<String> recommendationStrings = new ArrayList<>();
                    for (Product product : recommendations) {
                        recommendationStrings.add(product.getName() + " - $" + product.getPrice() + " at " + product.getStore());
                    }
                    recommendationsAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, recommendationStrings);
                    recommendationsListView.setAdapter(recommendationsAdapter);
                }
            }
        });
    }
}
