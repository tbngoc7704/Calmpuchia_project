package com.calmpuchia.userapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.calmpuchia.userapp.model.Product;
import com.calmpuchia.userapp.model.Store;
import com.calmpuchia.userapp.model.User;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;

import java.util.*;

public class ProductRecommendationService {
    private static final String TAG = "ProductRecommendation";
    private static final double MAX_DISTANCE_KM = 10.0;
    private static final int MAX_RECOMMENDATIONS = 6;

    private FirebaseFirestore db;
    private DatabaseReference userRef;
    private GeocodingService geocodingService;

    public ProductRecommendationService(Context context) {
        db = FirebaseFirestore.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");
        geocodingService = new GeocodingService(context);
    }

    public void getLocationBasedRecommendations(String userId, OnRecommendationListener listener) {
        if (userId == null || userId.trim().isEmpty()) {
            Log.e(TAG, "User ID không hợp lệ");
            listener.onError("User ID không hợp lệ");
            return;
        }

        userRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    User user = snapshot.getValue(User.class);
                    if (user == null) {
                        Log.d(TAG, "User không tồn tại với ID: " + userId);
                        listener.onRecommendationReceived(new ArrayList<>());
                        return;
                    }

                    // Kiểm tra xem user có tọa độ chưa
                    if (user.isHasLocation() && !user.needsLocationUpdate()) {
                        Log.d(TAG, "Sử dụng tọa độ có sẵn của user");
                        getStoresAndRecommendProducts(user, listener);
                    } else {
                        Log.d(TAG, "Cần geocode địa chỉ của user");
                        geocodeUserAddressAndRecommend(user, listener);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi khi xử lý dữ liệu user: " + e.getMessage());
                    listener.onError("Lỗi khi xử lý dữ liệu user: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi khi lấy thông tin user: " + error.getMessage());
                listener.onError("Lỗi database: " + error.getMessage());
            }
        });
    }

    private void geocodeUserAddressAndRecommend(User user, OnRecommendationListener listener) {
        if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            Log.d(TAG, "User không có địa chỉ");
            listener.onRecommendationReceived(new ArrayList<>());
            return;
        }

        geocodingService.getLocationFromAddress(user.getAddress(), new GeocodingService.OnGeocodingListener() {
            @Override
            public void onLocationFound(double lat, double lng) {
                Log.d(TAG, "Đã tìm được tọa độ: " + lat + ", " + lng);
                user.setLat(lat);
                user.setLng(lng);
                user.setHasLocation(true);
                user.setLocationUpdatedTime(System.currentTimeMillis());

                updateUserLocationInDatabase(user);
                getStoresAndRecommendProducts(user, listener);
            }

            @Override
            public void onLocationNotFound(String message) {
                Log.d(TAG, "Không tìm được tọa độ từ Geocoder, thử API backup");
                // Thử với API backup
                geocodingService.getLocationFromAddressWithAPI(user.getAddress(), new GeocodingService.OnGeocodingListener() {
                    @Override
                    public void onLocationFound(double lat, double lng) {
                        Log.d(TAG, "Đã tìm được tọa độ từ API backup: " + lat + ", " + lng);
                        user.setLat(lat);
                        user.setLng(lng);
                        user.setHasLocation(true);
                        user.setLocationUpdatedTime(System.currentTimeMillis());
                        updateUserLocationInDatabase(user);
                        getStoresAndRecommendProducts(user, listener);
                    }

                    @Override
                    public void onLocationNotFound(String message) {
                        Log.d(TAG, "Không tìm được tọa độ từ API backup: " + message);
                        listener.onRecommendationReceived(new ArrayList<>());
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Lỗi API backup: " + error);
                        listener.onError("Lỗi khi xác định vị trí: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Lỗi Geocoding: " + error);
                listener.onError("Lỗi khi xác định vị trí: " + error);
            }
        });
    }

    private void updateUserLocationInDatabase(User user) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lat", user.getLat());
        updates.put("lng", user.getLng());
        updates.put("hasLocation", true);
        updates.put("locationUpdatedTime", user.getLocationUpdatedTime());

        userRef.child(user.getUserId()).updateChildren(updates)
                .addOnSuccessListener(unused -> Log.d(TAG, "Đã cập nhật vị trí người dùng"))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật vị trí: " + e.getMessage()));
    }

    private void getStoresAndRecommendProducts(User user, OnRecommendationListener listener) {
        Log.d(TAG, "Tìm kiếm cửa hàng gần user tại: " + user.getLat() + ", " + user.getLng());

        db.collection("stores")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        List<Store> nearbyStores = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Store store = doc.toObject(Store.class);
                            if (store != null && LocationUtils.isStoreNearUser(user, store, MAX_DISTANCE_KM)) {
                                nearbyStores.add(store);
                                Log.d(TAG, "Tìm thấy cửa hàng gần: ");
                            }
                        }

                        Log.d(TAG, "Tổng cộng " + nearbyStores.size() + " cửa hàng gần user");

                        if (nearbyStores.isEmpty()) {
                            Log.d(TAG, "Không có cửa hàng nào gần user");
                            listener.onRecommendationReceived(new ArrayList<>());
                            return;
                        }

                        getProductsFromNearbyStores(nearbyStores, listener);
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý danh sách cửa hàng: " + e.getMessage());
                        listener.onError("Lỗi khi xử lý danh sách cửa hàng: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách cửa hàng: " + e.getMessage());
                    listener.onError("Lỗi khi lấy danh sách cửa hàng: " + e.getMessage());
                });
    }

    private void getProductsFromNearbyStores(List<Store> nearbyStores, OnRecommendationListener listener) {
        List<String> storeIds = new ArrayList<>();
        for (Store store : nearbyStores) {
            if (store.getStoreId() != null) {
                storeIds.add(store.getStoreId());
            }
        }

        if (storeIds.isEmpty()) {
            Log.d(TAG, "Không có store ID hợp lệ");
            listener.onRecommendationReceived(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Tìm kiếm sản phẩm hot deal từ " + storeIds.size() + " cửa hàng");

        // Firestore chỉ hỗ trợ whereIn với tối đa 10 phần tử
        if (storeIds.size() > 10) {
            storeIds = storeIds.subList(0, 10);
        }

        db.collection("products")
                .whereIn("storeId", storeIds)
                .whereEqualTo("isHotDeal", true)
                .limit(MAX_RECOMMENDATIONS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        List<Product> recommendedProducts = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Product product = doc.toObject(Product.class);
                            if (product != null) {
                                recommendedProducts.add(product);
                            }
                        }
                        Log.d(TAG, "Tìm thấy " + recommendedProducts.size() + " sản phẩm hot deal");
                        listener.onRecommendationReceived(recommendedProducts);
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý danh sách sản phẩm: " + e.getMessage());
                        listener.onError("Lỗi khi xử lý danh sách sản phẩm: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
                    listener.onError("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
                });
    }

    public interface OnRecommendationListener {
        void onRecommendationReceived(List<Product> products);
        void onError(String error);
    }
}