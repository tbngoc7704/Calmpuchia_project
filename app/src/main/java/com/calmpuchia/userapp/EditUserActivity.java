package com.calmpuchia.userapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.calmpuchia.userapp.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

public class EditUserActivity extends AppCompatActivity {

    private static final int GALLERY_PICK = 1;
    private static final String TAG = "EditUserActivity";

    private CircleImageView profileImageView;
    private TextInputEditText fullNameEditText, userPhoneEditText, addressEditText;
    private MaterialButton profileChangeBtn, saveBtn;

    private Uri imageUri;
    private String imageUrl = "";
    private boolean imageChanged = false;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_user);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        initViews();
        loadUserInfo();
        setClickListeners();
    }

    private void initViews() {
        profileImageView = findViewById(R.id.settings_profile_image);
        fullNameEditText = findViewById(R.id.settings_full_name);
        userPhoneEditText = findViewById(R.id.settings_phone_number);
        addressEditText = findViewById(R.id.settings_address);
        profileChangeBtn = findViewById(R.id.profile_image_change_btn);
        saveBtn = findViewById(R.id.save_settings_btn);
    }

    private void setClickListeners() {
        profileChangeBtn.setOnClickListener(view -> {
            openGallery();
        });

        saveBtn.setOnClickListener(view -> {
            saveUserInfo();
        });
    }

    private void openGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_PICK);
    }

    private void loadUserInfo() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usersRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        fullNameEditText.setText(user.getName());
                        userPhoneEditText.setText(user.getPhone());
                        addressEditText.setText(user.getAddress());

                        if (user.getImage() != null && !user.getImage().isEmpty()) {
                            imageUrl = user.getImage();
                            Glide.with(EditUserActivity.this)
                                    .load(user.getImage())
                                    .placeholder(R.mipmap.profile)
                                    .error(R.mipmap.profile)
                                    .into(profileImageView);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EditUserActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo() {
        if (!validateInput()) return;

        if (imageChanged && imageUri != null) {
            // Upload image first, then update user info
            uploadImageToImageBB();
        } else {
            // Update user info without changing image
            updateUserInfoToDatabase();
        }
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(fullNameEditText.getText().toString().trim())) {
            fullNameEditText.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(addressEditText.getText().toString().trim())) {
            addressEditText.setError("Address is required");
            return false;
        }
        if (TextUtils.isEmpty(userPhoneEditText.getText().toString().trim())) {
            userPhoneEditText.setError("Phone is required");
            return false;
        }
        return true;
    }

    // Upload image to ImageBB (free service, no registration needed)
    private void uploadImageToImageBB() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Image...");
        progressDialog.setMessage("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        // Run image processing in background thread to avoid UI blocking
        new Thread(() -> {
            try {
                // Convert URI to Bitmap and then to Base64
                Bitmap bitmap = null;
                try {
                    bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                            getContentResolver(), imageUri);

                    // Resize bitmap to reduce size
                    bitmap = resizeBitmap(bitmap, 800);

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(EditUserActivity.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // Create OkHttp client
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS) // Increased timeout
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                // Create request body for ImageBB
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", base64Image)
                        .build();

                // ImageBB API - free service
                Request request = new Request.Builder()
                        .url("https://api.imgbb.com/1/upload?key=781b8a01e85b3c3c5b5a31f4f4c96bd3") // Free public key
                        .post(requestBody)
                        .build();

                // Execute request
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            // Try alternative service if ImageBB fails
                            uploadImageToFreeImageHost();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();

                        runOnUiThread(() -> {
                            progressDialog.dismiss();

                            if (response.isSuccessful()) {
                                try {
                                    JSONObject jsonResponse = new JSONObject(responseBody);
                                    if (jsonResponse.getBoolean("success")) {
                                        JSONObject data = jsonResponse.getJSONObject("data");
                                        imageUrl = data.getString("url");
                                        updateUserInfoToDatabase();
                                    } else {
                                        Toast.makeText(EditUserActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    // Try alternative if parsing fails
                                    uploadImageToFreeImageHost();
                                }
                            } else {
                                // Try alternative service
                                uploadImageToFreeImageHost();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditUserActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Alternative free image hosting service
    private void uploadImageToFreeImageHost() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Trying alternative...");
        progressDialog.setMessage("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        // Run in background thread
        new Thread(() -> {
            try {
                // Convert URI to Bitmap
                Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                bitmap = resizeBitmap(bitmap, 800);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                // Using PostImage.cc - completely free, no registration
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload", "image.jpg",
                                RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                        .build();

                Request request = new Request.Builder()
                        .url("https://postimg.cc/json")
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            // If all external services fail, save as Base64 in Firebase
                            saveImageAsBase64();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();

                        runOnUiThread(() -> {
                            progressDialog.dismiss();

                            if (response.isSuccessful()) {
                                try {
                                    JSONObject jsonResponse = new JSONObject(responseBody);
                                    if (jsonResponse.getString("status").equals("OK")) {
                                        imageUrl = jsonResponse.getString("url");
                                        updateUserInfoToDatabase();
                                    } else {
                                        // Fallback to Base64 storage
                                        saveImageAsBase64();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    // Fallback to Base64 storage
                                    saveImageAsBase64();
                                }
                            } else {
                                // Fallback to Base64 storage
                                saveImageAsBase64();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    saveImageAsBase64();
                });
            }
        }).start();
    }

    // Fallback: Save image as Base64 directly in Firebase Database
    private void saveImageAsBase64() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Saving image...");
        progressDialog.setMessage("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        // Run in background thread
        new Thread(() -> {
            try {
                Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);

                // Resize smaller for Base64 storage (to avoid Firebase size limits)
                bitmap = resizeBitmap(bitmap, 300); // Smaller size for Base64

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); // Lower quality for smaller size
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    // Store as data URL
                    imageUrl = "data:image/jpeg;base64," + base64Image;
                    updateUserInfoToDatabase();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditUserActivity.this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Alternative: Upload to PostImage (another free service)
    private void uploadImageToPostImage() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Image...");
        progressDialog.setMessage("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        try {
            // Convert URI to Bitmap
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                    getContentResolver(), imageUri);
            bitmap = resizeBitmap(bitmap, 800);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload", "image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://postimg.cc/json")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(EditUserActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    runOnUiThread(() -> {
                        progressDialog.dismiss();

                        if (response.isSuccessful()) {
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                if (jsonResponse.getString("status").equals("OK")) {
                                    imageUrl = jsonResponse.getString("url");
                                    updateUserInfoToDatabase();
                                } else {
                                    Toast.makeText(EditUserActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(EditUserActivity.this, "Failed to parse response", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(EditUserActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void updateUserInfoToDatabase() {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", fullNameEditText.getText().toString().trim());
        userMap.put("address", addressEditText.getText().toString().trim());
        userMap.put("phone", userPhoneEditText.getText().toString().trim());

        // Only update image if it was changed
        if (imageChanged && !imageUrl.isEmpty()) {
            userMap.put("image", imageUrl);
        }

        userMap.put("lastUpdated", System.currentTimeMillis());

        usersRef.child(currentUserId).updateChildren(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EditUserActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditUserActivity.this, "Database update failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null) {
            Uri sourceUri = data.getData();
            File destinationFile = new File(getFilesDir(), "temp_crop_" + System.currentTimeMillis() + ".jpg");
            Uri destinationUri = Uri.fromFile(destinationFile);

            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(512, 512)
                    .start(this);

        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            imageUri = UCrop.getOutput(data);
            if (imageUri != null) {
                profileImageView.setImageURI(imageUri);
                imageChanged = true; // Mark that image has been changed
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Crop error: " + (cropError != null ? cropError.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
        }
    }
}