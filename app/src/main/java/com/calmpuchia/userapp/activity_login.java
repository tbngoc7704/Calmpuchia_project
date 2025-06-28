package com.calmpuchia.userapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.calmpuchia.userapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class activity_login extends AppCompatActivity {
    private EditText edtemail, edtpassword;
    private Button btnlogin, btnregister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        FirebaseAuth.getInstance().setLanguageCode("vi"); // hoặc "en"
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        edtemail = findViewById(R.id.edtemail);
        edtpassword = findViewById(R.id.edtpassword);
        btnlogin = findViewById(R.id.btnlogin);
        btnregister = findViewById(R.id.btnregister);

        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        btnregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void register() {
        Intent intent = new Intent(activity_login.this, activity_register.class);
        startActivity(intent);
    }

    private void login() {
        String email = edtemail.getText().toString().trim();
        String pass = edtpassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Vui lòng nhập password!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = mAuth.getCurrentUser().getUid();

                // Sử dụng Firestore để lấy thông tin user
                firestore.collection("users").document(userId)
                        .get()
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                DocumentSnapshot document = task1.getResult();
                                if (document.exists()) {
                                    User user = document.toObject(User.class);
                                    if (user != null) {
                                        // Đặt user_id từ document ID
                                        user.setUserId(document.getId());
                                        Prevalent.currentOnlineUser = user;

                                        // Cập nhật thời gian đăng nhập cuối
                                        updateLastLogin(userId);

                                        Toast.makeText(getApplicationContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(activity_login.this, HomepageActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Lỗi tải thông tin người dùng: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đăng nhập không thành công";
                Toast.makeText(getApplicationContext(), "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateLastLogin(String userId) {
        // Cập nhật thời gian đăng nhập cuối trong Firestore
        firestore.collection("users").document(userId)
                .update("last_login", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    // Không cần thông báo thành công cho việc này
                })
                .addOnFailureListener(e -> {
                    // Log lỗi nhưng không hiển thị cho user
                    android.util.Log.e("LoginActivity", "Failed to update last login: " + e.getMessage());
                });
    }
}