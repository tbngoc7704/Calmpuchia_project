package com.calmpuchia.userapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.calmpuchia.userapp.adapters.CartAdapter;
import com.calmpuchia.userapp.model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private TextView totalPriceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.recyclerCart);
        totalPriceText = findViewById(R.id.totalPriceText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cartItems, this::calculateTotal);
        recyclerView.setAdapter(cartAdapter);
        findViewById(R.id.btnCheckout).setOnClickListener(v -> proceedToCheckout());


        loadCartFromRealtimeDatabase();
    }

    private void loadCartFromRealtimeDatabase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference()
                .child("Cart List").child("User View").child(userId).child("Products");

        cartRef.get().addOnSuccessListener(dataSnapshot -> {
            cartItems.clear();
            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                CartItem item = ds.getValue(CartItem.class);
                if (item != null) {
                    item.setProductID(ds.getKey());
                    cartItems.add(item);
                }
            }
            cartAdapter.notifyDataSetChanged();
            calculateTotal();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi tải giỏ hàng", Toast.LENGTH_SHORT).show()
        );
    }

    private void calculateTotal() {
        int total = 0;
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                total += item.getPrice() * item.getQuantity();
            }
        }
        totalPriceText.setText("Tổng: " + total + " đ");
    }
    private void proceedToCheckout() {
        // Lấy các sản phẩm đã được chọn
        ArrayList<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuyển sang CheckoutActivity
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putParcelableArrayListExtra("selectedItems", selectedItems);
        startActivity(intent);
    }
}
