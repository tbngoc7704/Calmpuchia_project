package com.calmpuchia.userapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import com.calmpuchia.userapp.adapters.NearbyDealsAdapter;
import com.calmpuchia.userapp.model.Product;
import com.calmpuchia.userapp.model.User;
import com.calmpuchia.userapp.model.Store;
import com.calmpuchia.userapp.utils.GeocodingHelper;
import com.calmpuchia.userapp.utils.LocationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomepageActivity extends AppCompatActivity {
    private static final String TAG = "HomepageActivity";

    // UI components
    private RecyclerView addressRecyclerView;
    private NearbyDealsAdapter nearbyDealsAdapter;
    private TextView tvNearbyDealsTitle;

    // Data
    private List<Product> nearbyProducts = new ArrayList<>();

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // Initialize Firebase
        initFirebase();

        // Initialize views
        initViews();

        // Load nearby products
        loadNearbyProducts();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "Current user ID: " + currentUserId);
        } else {
            Log.w(TAG, "No authenticated user found");
        }
    }

    private void initViews() {
        // Initialize nearby deals section
        addressRecyclerView = findViewById(R.id.addressrecyclerView);
        tvNearbyDealsTitle = findViewById(R.id.tvNearbyDealsTitle);

        // Setup RecyclerView for nearby deals
        nearbyDealsAdapter = new NearbyDealsAdapter(this, nearbyProducts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        addressRecyclerView.setLayoutManager(layoutManager);
        addressRecyclerView.setAdapter(nearbyDealsAdapter);

        // Set click listener for title to navigate to NearbyProductsActivity
        if (tvNearbyDealsTitle != null) {
            tvNearbyDealsTitle.setOnClickListener(v -> {
                Intent intent = new Intent(HomepageActivity.this, NearbyProductsActivity.class);
                startActivity(intent);
            });
        }

        // Set click listener for entire nearby deals section
        View nearbyDealsSection = findViewById(R.id.nearbyDealsSection);
        if (nearbyDealsSection != null) {
            nearbyDealsSection.setOnClickListener(v -> {
                Intent intent = new Intent(HomepageActivity.this, NearbyProductsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadNearbyProducts() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot load nearby products: User not authenticated");
            return;
        }

        Log.d(TAG, "Loading nearby products for user: " + currentUserId);

        // Get user address from Firestore
        userListener = firestore.collection("users").document(currentUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            String address = user.getAddress();
                            if (address != null && !address.trim().isEmpty()) {
                                Log.d(TAG, "User address found: " + address);
                                convertAddressAndFindNearbyProducts(address.trim());
                            } else {
                                Log.w(TAG, "User address is empty");
                                // You might want to show a message to user to set their address
                            }
                        } else {
                            Log.w(TAG, "User object is null");
                        }
                    } else {
                        Log.w(TAG, "User data does not exist");
                    }
                });
    }

    private void convertAddressAndFindNearbyProducts(String address) {
        Log.d(TAG, "Converting address to coordinates: " + address);

        GeocodingHelper.getLatLngFromAddress(address, new GeocodingHelper.GeocodingCallback() {
            @Override
            public void onSuccess(double lat, double lng) {
                Log.d(TAG, "Address converted successfully: " + lat + ", " + lng);
                findNearestStoreAndLoadProducts(lat, lng);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error converting address to coordinates: " + error);
                // You might want to show an error message to the user
            }
        });
    }

    private void findNearestStoreAndLoadProducts(double userLat, double userLng) {
        Log.d(TAG, "Finding nearest store to coordinates: " + userLat + ", " + userLng);

        firestore.collection("stores").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Store nearestStore = null;
                    double minDistance = Double.MAX_VALUE;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Store store = document.toObject(Store.class);
                            if (store != null && store.getLat() != 0 && store.getLng() != 0) {
                                double distance = LocationUtils.calculateDistance(
                                        userLat, userLng, store.getLat(), store.getLng()
                                );

                                Log.d(TAG, "Store: " + store.getStoreId() + ", Distance: " + distance + " km");

                                if (distance < minDistance) {
                                    minDistance = distance;
                                    nearestStore = store;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing store document: " + document.getId(), e);
                        }
                    }

                    if (nearestStore != null) {
                        Log.d(TAG, "Nearest store found: " + nearestStore.getStoreId() +
                                " at distance: " + minDistance + " km");
                        loadProductsFromStore(nearestStore.getStoreId());
                    } else {
                        Log.w(TAG, "No nearest store found");
                        // You might want to show a message that no nearby stores are available
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading stores from Firestore", e);
                });
    }

    private void loadProductsFromStore(String storeId) {
        Log.d(TAG, "Loading products from store: " + storeId);

        firestore.collection("products")
                .whereEqualTo("store_id", storeId)
                .limit(5) // Only get first 5 products
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    nearbyProducts.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Product product = document.toObject(Product.class);
                            if (product != null) {
                                // Set document ID if product ID is null
                                if (product.getId() == null) {
                                    product.setId(document.getId());
                                }
                                nearbyProducts.add(product);
                                Log.d(TAG, "Product added: " + product.getName());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing product document: " + document.getId(), e);
                        }
                    }

                    Log.d(TAG, "Total products loaded: " + nearbyProducts.size());

                    // Update adapter on main thread
                    runOnUiThread(() -> {
                        if (nearbyDealsAdapter != null) {
                            nearbyDealsAdapter.updateProducts(nearbyProducts);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading products from store: " + storeId, e);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh nearby products when activity resumes
        if (currentUserId != null) {
            loadNearbyProducts();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener
        if (userListener != null) {
            userListener.remove();
        }
    }
}