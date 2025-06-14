package com.calmpuchia.userapp;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StoreIdAssignerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assignRandomStoreIdToProducts(); // Gọi hàm xử lý luôn khi Activity chạy
    }

    private void assignRandomStoreIdToProducts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<String> storeIds = Arrays.asList("Store01", "Store02", "Store03", "Store04", "Store05");
        Random random = new Random();

        db.collection("products").get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot doc : querySnapshot) {
                String randomStoreId = storeIds.get(random.nextInt(storeIds.size()));

                db.collection("products")
                        .document(doc.getId())
                        .update("store_id", randomStoreId)
                        .addOnSuccessListener(aVoid -> Log.d("AssignStoreId", "Updated product " + doc.getId()))
                        .addOnFailureListener(e -> Log.e("AssignStoreId", "Failed to update " + doc.getId(), e));
            }
        }).addOnFailureListener(e -> Log.e("AssignStoreId", "Failed to load products", e));
    }
}
