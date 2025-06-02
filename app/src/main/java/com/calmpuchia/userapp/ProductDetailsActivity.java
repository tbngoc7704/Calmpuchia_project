package com.calmpuchia.userapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.calmpuchia.userapp.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ProductDetailsActivity extends AppCompatActivity {

    private Button addToCartButton;
    private ImageView productImage;
    private ImageButton btnIncrease, btnDecrease;
    private TextView productPrice, productDescription, productName, numberText;
    private String productID = "";
    private int quantity = 1;
    private double unitPrice = 0.0;
    private String name = "";
    private String image = "";
    private ArrayList<String> descriptionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ view
        addToCartButton = findViewById(R.id.pd_add_to_cart_button);
        productImage = findViewById(R.id.product_image_details);
        productName = findViewById(R.id.product_name_details);
        productDescription = findViewById(R.id.product_description_details);
        productPrice = findViewById(R.id.product_price_details);
        numberText = findViewById(R.id.number_text);
        btnIncrease = findViewById(R.id.btn_increase);
        btnDecrease = findViewById(R.id.btn_decrease);

        // Lấy dữ liệu từ Intent
        productID = getIntent().getStringExtra("pid");
        name = getIntent().getStringExtra("name");
        descriptionList = getIntent().getStringArrayListExtra("description");
        image = getIntent().getStringExtra("image");
        unitPrice = getIntent().getDoubleExtra("price", 0.0);

        if (productID == null || productID.isEmpty()) {
            Toast.makeText(this, "Không thể hiển thị sản phẩm: ID bị thiếu.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Thiết lập số lượng ban đầu
        numberText.setText(String.valueOf(quantity));

        // Xử lý nút tăng số lượng
        btnIncrease.setOnClickListener(v -> {
            quantity++;
            numberText.setText(String.valueOf(quantity));
            updateTotalPrice();
        });

        // Xử lý nút giảm số lượng
        btnDecrease.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                numberText.setText(String.valueOf(quantity));
                updateTotalPrice();
            } else {
                Toast.makeText(ProductDetailsActivity.this, "Số lượng tối thiểu là 1", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút thêm vào giỏ hàng
        addToCartButton.setOnClickListener(v -> addingToCartList());

        // Lấy thông tin sản phẩm từ Firestore
        loadProductDetails();
    }

    private void loadProductDetails() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products").document(productID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            // Cập nhật lại toàn bộ biến để đồng bộ dữ liệu từ Firestore
                            name = product.getName();
                            unitPrice = product.getPrice();

                            productName.setText(name);
                            productPrice.setText("Giá: " + unitPrice + " đ");
                            updateTotalPrice();

                            // Mô tả sản phẩm
                            if (descriptionList != null && !descriptionList.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (String desc : descriptionList) {
                                    sb.append(desc).append("\n");
                                }
                                productDescription.setText(sb.toString());
                            } else {
                                productDescription.setText("Không có mô tả.");
                            }

                            // Ảnh sản phẩm
                            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                                image = product.getImageUrl();
                            }
                            Glide.with(this)
                                    .load(image)
                                    .placeholder(R.mipmap.ic_producta)
                                    .into(productImage);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy sản phẩm.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi tải dữ liệu sản phẩm.", Toast.LENGTH_SHORT).show());
    }


    private void updateTotalPrice() {
        double totalPrice = unitPrice * quantity;
    }

    private void addingToCartList() {
        String saveCurrentDate, saveCurrentTime;
        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calForDate.getTime());

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List");

        double totalPrice = unitPrice * quantity; // tính total price

        HashMap<String, Object> cartMap = new HashMap<>();
        cartMap.put("pid", productID);
        cartMap.put("name", name);
        cartMap.put("price", unitPrice);
        cartMap.put("date", saveCurrentDate);
        cartMap.put("time", saveCurrentTime);
        cartMap.put("quantity", quantity);
        cartMap.put("total_price", totalPrice); // lưu total price vào Firebase
        cartMap.put("discount_price", "");

        cartListRef.child("User View").child(userId)
                .child("Products").child(productID)
                .updateChildren(cartMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartListRef.child("Admin View").child(userId)
                                .child("Products").child(productID)
                                .updateChildren(cartMap)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(ProductDetailsActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(ProductDetailsActivity.this, HomepageActivity.class));
                                        finish();
                                    }
                                });
                    }
                });
    }
}
