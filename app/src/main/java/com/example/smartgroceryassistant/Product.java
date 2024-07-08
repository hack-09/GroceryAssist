package com.example.smartgroceryassistant;

public class Product {
    private String name;
    private double price;
    private String store;
    private boolean available;

    public Product(String name, double price, String store, boolean available) {
        this.name = name;
        this.price = price;
        this.store = store;
        this.available = available;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getStore() {
        return store;
    }

    public boolean isAvailable() {
        return available;
    }
}
