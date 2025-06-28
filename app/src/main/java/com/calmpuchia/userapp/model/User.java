package com.calmpuchia.userapp.model;

public class User {
    private String userId;
    public String name;
    public String phone;
    public String email;
    public String password;
    public String image;
    public String address;
    private double lat;
    private double lng;
    private boolean hasLocation;
    private long locationUpdatedTime;
    private long lastUpdated;
    private int coins;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String name, String phone, String email, String password ) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.password = password;

    }

    // Getter
    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    // Setter
    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLat() { return lat;}

    public void setLat(double lat) {this.lat = lat;}

    public double getLng() {return lng;}

    public void setLng(double lng) {this.lng = lng;}

    public boolean isHasLocation() {return hasLocation;}

    public void setHasLocation(boolean hasLocation) {this.hasLocation = hasLocation;}

    public long getLocationUpdatedTime() {return locationUpdatedTime;}

    public void setLocationUpdatedTime(long locationUpdatedTime) {this.locationUpdatedTime = locationUpdatedTime;}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean needsLocationUpdate() {
        if (!hasLocation) return true;

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - locationUpdatedTime;
        long oneDayInMillis = 24 * 60 * 60 * 1000; // 24 giờ

        return timeDiff > oneDayInMillis;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }
}
