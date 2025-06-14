package com.calmpuchia.userapp.model;

public class Store {
    private String storeId;
    private String address;
    private double lat;
    private double lng;

    public Store() {
        // Bắt buộc cần constructor rỗng cho Firestore
    }

    public Store(String storeId, String address, double lat, double lng) {
        this.storeId = storeId;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
    }

    // Getters & Setters

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
