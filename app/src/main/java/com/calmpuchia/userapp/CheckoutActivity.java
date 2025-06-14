package com.calmpuchia.userapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.calmpuchia.userapp.adapters.CheckoutAdapter;
import com.calmpuchia.userapp.model.CartItem;
import com.calmpuchia.userapp.model.CheckoutOrder;
import com.calmpuchia.userapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserAddress, tvSubtotal, tvShippingFee, tvVoucherDiscount, tvFinalTotal;
    private Button btnEditAddress, btnApplyVoucher, btnCheckout;
    private EditText etVoucherCode;
    private RadioGroup rgPaymentMethod;
    private LinearLayout llPaymentDetails;
    private RecyclerView recyclerCheckoutItems;

    private CheckoutAdapter checkoutAdapter;
    private List<CartItem> selectedItems = new ArrayList<>();
    private User currentUser;
    private String userAddress = "";

    private int subtotal = 0;
    private int shippingFee = 30000; // 30k toàn quốc
    private int voucherDiscount = 0;
    private int finalTotal = 0;

    private DatabaseReference databaseReference;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        initViews();
        setupRecyclerView();
        loadUserInfo();
        calculatePrices();
        setupEventListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserAddress = findViewById(R.id.tvUserAddress);
        btnEditAddress = findViewById(R.id.btnEditAddress);

        recyclerCheckoutItems = findViewById(R.id.recyclerCheckoutItems);

        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvVoucherDiscount = findViewById(R.id.tvVoucherDiscount);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);

        etVoucherCode = findViewById(R.id.etVoucherCode);
        btnApplyVoucher = findViewById(R.id.btnApplyVoucher);

        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        llPaymentDetails = findViewById(R.id.llPaymentDetails);

        btnCheckout = findViewById(R.id.btnCheckout);

        // Lấy dữ liệu từ intent
        selectedItems = getIntent().getParcelableArrayListExtra("selectedItems");
        if (selectedItems == null) {
            selectedItems = new ArrayList<>();
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void setupRecyclerView() {
        checkoutAdapter = new CheckoutAdapter(selectedItems);
        recyclerCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerCheckoutItems.setAdapter(checkoutAdapter);
    }

    private void loadUserInfo() {
        databaseReference.child("Users").child(userId).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        currentUser = dataSnapshot.getValue(User.class);
                        if (currentUser != null) {
                            tvUserName.setText(currentUser.getName());

                            if (!TextUtils.isEmpty(currentUser.getAddress())) {
                                userAddress = currentUser.getAddress();
                                tvUserAddress.setText(userAddress);
                            } else {
                                tvUserAddress.setText("Chưa có địa chỉ");
                                showAddressDialog();
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải thông tin user", Toast.LENGTH_SHORT).show()
                );
    }

    private void calculatePrices() {
        subtotal = 0;
        for (CartItem item : selectedItems) {
            subtotal += item.getPrice() * item.getQuantity();
        }

        finalTotal = subtotal + shippingFee - voucherDiscount;

        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        tvSubtotal.setText(String.format("%,d đ", subtotal));
        tvShippingFee.setText(String.format("%,d đ", shippingFee));
        tvVoucherDiscount.setText(String.format("-%,d đ", voucherDiscount));
        tvFinalTotal.setText(String.format("%,d đ", finalTotal));

        // Cập nhật text button checkout
        btnCheckout.setText(String.format("Đặt hàng - %,d đ", finalTotal));
    }

    private void setupEventListeners() {
        btnEditAddress.setOnClickListener(v -> showAddressDialog());

        btnApplyVoucher.setOnClickListener(v -> applyVoucher());

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            showPaymentDetails(checkedId);
        });

        btnCheckout.setOnClickListener(v -> processCheckout());
    }

    private void showAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập địa chỉ giao hàng");

        EditText etAddress = new EditText(this);
        etAddress.setText(userAddress);
        etAddress.setHint("Nhập địa chỉ của bạn...");
        builder.setView(etAddress);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String address = etAddress.getText().toString().trim();
            if (!TextUtils.isEmpty(address)) {
                userAddress = address;
                tvUserAddress.setText(userAddress);

                // Cập nhật địa chỉ trong database
                databaseReference.child("Users").child(userId).child("address").setValue(address);
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void applyVoucher() {
        String voucherCode = etVoucherCode.getText().toString().trim();
        if (TextUtils.isEmpty(voucherCode)) {
            Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra voucher trong database
        databaseReference.child("Vouchers").child(voucherCode).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        Integer discount = dataSnapshot.child("discount").getValue(Integer.class);
                        if (discount != null) {
                            voucherDiscount = discount;
                            calculatePrices();
                            Toast.makeText(this, "Áp dụng voucher thành công!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Mã voucher không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi kiểm tra voucher", Toast.LENGTH_SHORT).show()
                );
    }

    private void showPaymentDetails(int checkedId) {
        llPaymentDetails.removeAllViews();

        if (checkedId == R.id.rbMomo) {
            TextView tvMomoInfo = new TextView(this);
            tvMomoInfo.setText("Chủ tài khoản: Trần Bảo Ngọc\nSố điện thoại: 0933109239");
            tvMomoInfo.setPadding(16, 8, 16, 8);
            llPaymentDetails.addView(tvMomoInfo);
            llPaymentDetails.setVisibility(View.VISIBLE);
        } else if (checkedId == R.id.rbBankTransfer) {
            TextView tvBankInfo = new TextView(this);
            tvBankInfo.setText("Chủ tài khoản: TRAN BAO NGOC tài khoản: 007072004\nNgân hàng: VIB_Ngân hàng Quốc tế");
            tvBankInfo.setPadding(16, 8, 16, 8);
            llPaymentDetails.addView(tvBankInfo);
            llPaymentDetails.setVisibility(View.VISIBLE);
        } else {
            llPaymentDetails.setVisibility(View.GONE);
        }
    }

    private void processCheckout() {
        // Validate thông tin
        if (TextUtils.isEmpty(userAddress)) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPaymentId = rgPaymentMethod.getCheckedRadioButtonId();
        if (selectedPaymentId == -1) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đơn hàng
        CheckoutOrder order = new CheckoutOrder();
        order.setOrderId(databaseReference.child("Checkout").push().getKey());
        order.setUserId(userId);
        order.setCustomerName(currentUser.getName());
        order.setCustomerAddress(userAddress);
        order.setCustomerPhone(currentUser.getPhone());
        order.setProducts(selectedItems);
        order.setShippingFee(shippingFee);
        order.setVoucherCode(etVoucherCode.getText().toString().trim());
        order.setVoucherDiscount(voucherDiscount);
        order.setTotalPrice(finalTotal);

        RadioButton selectedPayment = findViewById(selectedPaymentId);
        order.setPaymentMethod(selectedPayment.getText().toString());

        order.setTimestamp(System.currentTimeMillis());
        order.setStatus("Pending");

        // Lưu vào database
        databaseReference.child("Checkout").child(order.getOrderId()).setValue(order)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi đặt hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
    private void removeCheckedItemsFromCart() {
        DatabaseReference cartRef = databaseReference.child("Cart List")
                .child("User View").child(userId).child("Products");
        // Xóa từng sản phẩm đã checkout khỏi cart
        for (CartItem item : selectedItems) {
            if (item.getProductID() != null && !item.getProductID().isEmpty()) {
                cartRef.child(item.getProductID()).removeValue();
            }
        }
    }
}