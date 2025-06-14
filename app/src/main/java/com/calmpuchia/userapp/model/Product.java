package com.calmpuchia.userapp.model;

import java.util.List;

public class Product {
    private String id;
    private String productId;
    private String name;
    private String image_url;
    private Double price;  // Đổi từ primitive double sang wrapper Double
    private List<String> description;
    private Double discount_price;

    public Product() {} // Bắt buộc cho Firestore

    public Product(String id, String name, String imageUrl, Double price, List<String> description, Double discount_price) {
        this.id = id;
        this.name = name;
        this.image_url = imageUrl;
        this.price = price;
        this.description = description;
        this.discount_price = discount_price;

    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description; }

    public Double getDiscount_price() {
        return discount_price;
    }

    public void setDiscount_price(Double discount_price) {
        this.discount_price = discount_price;
    }
}
