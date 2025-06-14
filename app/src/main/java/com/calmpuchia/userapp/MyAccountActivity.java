package com.calmpuchia.userapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyAccountActivity extends AppCompatActivity {

    private TextView txtName, txtEmail, txtCoins, cartBadge;
    private ImageView settingIcon, cartIcon, profileImage;
    private String userId;
    private DatabaseReference usersRef, cartRef, checkoutRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        // Khởi tạo views
        initViews();

        // Lấy userId
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Khởi tạo Firebase references
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        cartRef = FirebaseDatabase.getInstance().getReference("Cart List").child("User View").child(userId).child("Products");
        checkoutRef = FirebaseDatabase.getInstance().getReference("Checkout");

        // Load dữ liệu
        loadUserInfo();
        loadCartBadge();
        setupOrderListener();

        // Setup click listeners
        setupClickListeners();
    }

    private void initViews() {
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        settingIcon = findViewById(R.id.settingIcon); // Thêm id này vào layout
        cartIcon = findViewById(R.id.cartIcon); // Thêm id này vào layout
        cartBadge = findViewById(R.id.cartBadge); // Thêm TextView cho badge số lượng cart
        profileImage = findViewById(R.id.profileImage); // Thêm ImageView cho ảnh profile

        // Tìm TextView hiển thị coins (có thể cần thêm vào layout)
        txtCoins = findViewById(R.id.txtCoins);
    }

    private void setupClickListeners() {
        // Click vào setting icon -> chuyển đến EditUserActivity
        settingIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MyAccountActivity.this, EditUserActivity.class);
            startActivity(intent);
        });

        // Click vào cart icon -> chuyển đến CartActivity
        cartIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MyAccountActivity.this, CartActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserInfo() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Lấy thông tin cơ bản
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String imageUrl = dataSnapshot.child("image").getValue(String.class);

                    // Lấy số coins (mặc định là 0 nếu chưa có)
                    Long coins = dataSnapshot.child("coins").getValue(Long.class);
                    if (coins == null) coins = 0L;

                    // Gán dữ liệu vào views
                    txtName.setText(name != null ? name : "");
                    txtEmail.setText(email != null ? email : "");
                    txtCoins.setText(coins + " 🪙");

                    // Load ảnh profile
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        if (imageUrl.startsWith("data:image")) {
                            // Xử lý Base64 image
                            Glide.with(MyAccountActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.mipmap.ic_customer)
                                    .error(R.mipmap.ic_customer)
                                    .into(profileImage);
                        } else {
                            // Xử lý URL image
                            Glide.with(MyAccountActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.mipmap.ic_customer)
                                    .error(R.mipmap.ic_customer)
                                    .into(profileImage);
                        }
                    } else {
                        profileImage.setImageResource(R.mipmap.ic_customer);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MyAccountActivity.this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCartBadge() {
        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int totalItems = 0;

                // Đếm tổng số lượng sản phẩm trong cart
                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    Integer quantity = productSnapshot.child("quantity").getValue(Integer.class);
                    if (quantity != null) {
                        totalItems += quantity;
                    }
                }

                // Hiển thị badge
                if (totalItems > 0) {
                    cartBadge.setText(String.valueOf(totalItems));
                    cartBadge.setVisibility(TextView.VISIBLE);
                } else {
                    cartBadge.setVisibility(TextView.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error nếu cần
            }
        });
    }

    private void setupOrderListener() {
        // Lắng nghe các đơn hàng hoàn thành để cộng coins
        checkoutRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                            String status = orderSnapshot.child("status").getValue(String.class);
                            Boolean coinsAdded = orderSnapshot.child("coinsAdded").getValue(Boolean.class);

                            // Nếu đơn hàng hoàn thành và chưa cộng coins
                            if ("Completed".equals(status) && (coinsAdded == null || !coinsAdded)) {
                                addCoinsForCompletedOrder(orderSnapshot.getKey());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Handle error
                    }
                });
    }

    private void addCoinsForCompletedOrder(String orderId) {
        // Lấy số coins hiện tại
        usersRef.child("coins").get().addOnSuccessListener(dataSnapshot -> {
            Long currentCoins = dataSnapshot.getValue(Long.class);
            if (currentCoins == null) currentCoins = 0L;

            // Cộng thêm 500 coins
            long newCoins = currentCoins + 500;

            // Cập nhật coins trong database
            usersRef.child("coins").setValue(newCoins).addOnSuccessListener(aVoid -> {
                // Đánh dấu đơn hàng đã được cộng coins
                checkoutRef.child(orderId).child("coinsAdded").setValue(true);

                Toast.makeText(MyAccountActivity.this,
                        "Chúc mừng! Bạn được cộng 500 coins cho đơn hàng hoàn thành!",
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh dữ liệu khi trở lại từ EditUserActivity
        loadUserInfo();
    }
}