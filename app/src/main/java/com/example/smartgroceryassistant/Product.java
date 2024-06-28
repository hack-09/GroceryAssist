package com.example.smartgroceryassistant;

public class Product {
    private String name;
    private double price;
    private String store;

    public Product(String name, double price, String store) {
        this.name = name;
        this.price = price;
        this.store = store;
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
}
