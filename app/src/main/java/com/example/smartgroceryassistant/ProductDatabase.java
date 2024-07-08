package com.example.smartgroceryassistant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class ProductDatabase {

    private OkHttpClient client = new OkHttpClient();

    public void fetchProductData(String query, final DataCallback callback) {
        String url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" + query + "&search_simple=1&action=process&json=1";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    List<Product> products = parseProducts(jsonData);
                    callback.onSuccess(products);
                } else {
                    callback.onFailure(new IOException("Unexpected code " + response));
                }
            }
        });
    }

    private List<Product> parseProducts(String jsonData) {
        List<Product> products = new ArrayList<>();

        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("products");

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject productObject = jsonArray.get(i).getAsJsonObject();
            String name = productObject.get("product_name").getAsString();
            double price = productObject.has("price") ? productObject.get("price").getAsDouble() : 0.0;
            String store = "Open Food Facts";  // This API does not provide store information

            products.add(new Product(name, price, store, true));
        }

        return products;
    }

    public interface DataCallback {
        void onSuccess(List<Product> products);
        void onFailure(Exception e);
    }
}

