package com.calmpuchia.userapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.calmpuchia.userapp.adapters.ProductAdapter;
import com.calmpuchia.userapp.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class HomepageActivity extends AppCompatActivity {
    private static final String TAG = "HomepageActivity";

    private RecyclerView hotDealsRecyclerView;
    private ProductAdapter hotDealsAdapter;
    private TextView hotDealsTitle;
    private ProductRecommendationService recommendationService;
    private String currentUserId;
    private ProgressBar loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        initViews();
        setupRecommendationService();
        loadHotDealsNearUser();
    }

    private void initViews() {
        hotDealsRecyclerView = findViewById(R.id.hotDealsRecyclerView);
        hotDealsTitle = findViewById(R.id.hotDealsTitle);
        loadingProgress = findViewById(R.id.loadingProgress);

        // Kiểm tra null trước khi sử dụng
        if (hotDealsRecyclerView != null) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
            hotDealsRecyclerView.setLayoutManager(gridLayoutManager);

            hotDealsAdapter = new ProductAdapter(this, new ArrayList<>());
            hotDealsRecyclerView.setAdapter(hotDealsAdapter);

            // Thiết lập listener cho adapter
            hotDealsAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
                @Override
                public void onProductClick(Product product) {
                    onProductClick(product);
                }
            });
        }

        if (hotDealsTitle != null) {
            hotDealsTitle.setOnClickListener(v -> openAllNearbyProductsActivity());
        }
    }

    private void setupRecommendationService() {
        recommendationService = new ProductRecommendationService(this);
        currentUserId = getCurrentUserId();
    }

    private void loadHotDealsNearUser() {
        if (currentUserId == null) {
            Log.e(TAG, "User ID is null");
            hideHotDealsSection();
            return;
        }

        showLoading(true);

        recommendationService.getLocationBasedRecommendations(currentUserId,
                new ProductRecommendationService.OnRecommendationListener() {
                    @Override
                    public void onRecommendationReceived(List<Product> products) {
                        runOnUiThread(() -> {
                            showLoading(false);

                            if (products == null || products.isEmpty()) {
                                hideHotDealsSection();
                                Log.d(TAG, "Không có sản phẩm nào gần người dùng");
                            } else {
                                showHotDealsSection();
                                if (hotDealsAdapter != null) {
                                    hotDealsAdapter.updateProducts(products);
                                }
                                Log.d(TAG, "Đã tải " + products.size() + " sản phẩm hot deals");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Log.e(TAG, "Lỗi khi load hot deals: " + error);
                            hideHotDealsSection();
                            Toast.makeText(HomepageActivity.this,
                                    "Không thể tải sản phẩm gần bạn. Vui lòng thử lại.",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showHotDealsSection() {
        View hotDealsSection = findViewById(R.id.hotDealsSection);
        if (hotDealsSection != null) {
            hotDealsSection.setVisibility(View.VISIBLE);
        }
    }

    private void hideHotDealsSection() {
        View hotDealsSection = findViewById(R.id.hotDealsSection);
        if (hotDealsSection != null) {
            hotDealsSection.setVisibility(View.GONE);
        }
    }

    private void onProductClick(Product product) {
        if (product != null && product.getProductId() != null) {
            Intent intent = new Intent(this, ProductDetailsActivity.class);
            intent.putExtra("product_id", product.getProductId());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Không thể mở chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAllNearbyProductsActivity() {
        if (currentUserId != null) {
            Intent intent = new Intent(this, NearbyProductsActivity.class);
            intent.putExtra("user_id", currentUserId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để xem sản phẩm gần bạn", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentUserId() {
        // Ưu tiên Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }

        // Fallback về SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getString("user_id", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data khi quay lại activity
        if (currentUserId != null) {
            loadHotDealsNearUser();
        }
    }
}