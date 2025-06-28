package com.calmpuchia.userapp.model;

import java.util.List;

public class Product {
    private String id;
    private String productId;
    private String name;
    private String image_url;
    private Double price;
    private List<String> description;
    private Double discount_price;
    private String storeId;
    private int stock;
    private String category;
    private boolean isActive;

    // New fields to match Firestore document structure
    private String store_id;     // matches store_id field in Firestore
    private String category_id;  // matches category_id field in Firestore
    private com.google.firebase.Timestamp updated_at;   // matches updated_at field in Firestore (Timestamp type)
    private String product_id;   // matches product_id field in Firestore (different from productId)
    private String brand;        // matches brand field in Firestore
    private List<String> tags;   // matches tags field in Firestore

    public Product() {} // Required for Firestore

    public Product(String id, String name, String imageUrl, Double price, List<String> description, Double discount_price) {
        this.id = id;
        this.name = name;
        this.image_url = imageUrl;
        this.price = price;
        this.description = description;
        this.discount_price = discount_price;
        this.isActive = true;
    }

    // Existing getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description; }

    public Double getDiscount_price() { return discount_price; }
    public void setDiscount_price(Double discount_price) { this.discount_price = discount_price; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // New getters and setters for Firestore fields
    public String getStore_id() { return store_id; }
    public void setStore_id(String store_id) { this.store_id = store_id; }

    public String getCategory_id() { return category_id; }
    public void setCategory_id(String category_id) { this.category_id = category_id; }

    public com.google.firebase.Timestamp getUpdated_at() { return updated_at; }
    public void setUpdated_at(com.google.firebase.Timestamp updated_at) { this.updated_at = updated_at; }

    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // Utility methods
    public boolean hasDiscount() {
        return discount_price != null && discount_price > 0 && discount_price < price;
    }

    public double getDiscountPercentage() {
        if (hasDiscount()) {
            return ((price - discount_price) / price) * 100;
        }
        return 0;
    }

    public Double getFinalPrice() {
        return hasDiscount() ? discount_price : price;
    }

    // Helper method to get the correct store ID regardless of field name
    public String getEffectiveStoreId() {
        return store_id != null ? store_id : storeId;
    }

    // Helper method to get the correct product ID regardless of field name
    public String getEffectiveProductId() {
        return product_id != null ? product_id : productId;
    }

    // Helper method to get updated_at as a formatted string
    public String getUpdatedAtString() {
        if (updated_at != null) {
            return new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault())
                    .format(updated_at.toDate());
        }
        return "";
    }

    // Helper method to get updated_at as Date
    public java.util.Date getUpdatedAtDate() {
        return updated_at != null ? updated_at.toDate() : null;
    }
}