package com.calmpuchia.userapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MyAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        // ★ Lấy UID của người dùng hiện tại từ Firebase Authentication
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // ★

        // ★ Truy cập vào nút "Users/userId" trong Firebase Realtime Database
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId); // ★

        // ★ Lấy dữ liệu người dùng từ Firebase
        ref.get().addOnCompleteListener(task -> { // ★
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    // ★ Lấy thông tin "name" và "email" từ dữ liệu
                    String name = task.getResult().child("name").getValue(String.class); // ★
                    String email = task.getResult().child("email").getValue(String.class); // ★

                    // ★ Ánh xạ TextView từ layout
                    TextView txtName = findViewById(R.id.txtName); // ★
                    TextView txtEmail = findViewById(R.id.txtEmail); // ★

                    // ★ Gán dữ liệu lấy được vào TextView
                    txtName.setText(name); // ★
                    txtEmail.setText(email); // ★
                }
            } else {
                // ★ Thông báo lỗi nếu không lấy được thông tin
                Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show(); // ★
            }
        });
    }

}