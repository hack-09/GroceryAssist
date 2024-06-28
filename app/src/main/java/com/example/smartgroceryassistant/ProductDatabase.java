package com.example.smartgroceryassistant;

import java.util.ArrayList;
import java.util.List;

public class ProductDatabase {
    public static List<Product> getProductRecommendations(String itemName) {
        List<Product> products = new ArrayList<>();
        // Mock data
        products.add(new Product(itemName, 2.99, "Store A"));
        products.add(new Product(itemName, 2.49, "Store B"));
        products.add(new Product(itemName, 3.19, "Store C"));
        return products;
    }
}
