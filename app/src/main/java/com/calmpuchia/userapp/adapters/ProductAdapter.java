package com.calmpuchia.userapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.calmpuchia.userapp.R;
import com.calmpuchia.userapp.model.Product;
import com.calmpuchia.userapp.ProductDetailsActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnProductClickListener onProductClickListener;

    public ProductAdapter(Context context, List<Product> list) {
        this.context = context;
        this.productList = list != null ? list : new ArrayList<>();
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.onProductClickListener = listener;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtPrice, txtRating, txtRatingCount, txtSold;

        public ProductViewHolder(View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtRating = itemView.findViewById(R.id.txtRating);
            txtRatingCount = itemView.findViewById(R.id.txtRatingCount);
            txtSold = itemView.findViewById(R.id.txtSold);
        }
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        try {
            Product product = productList.get(position);
            if (product == null) return;

            // Tên sản phẩm
            holder.txtName.setText(product.getName() != null ? product.getName() : "Tên sản phẩm");

            // Giá sản phẩm
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            if (product.hasDiscount()) {
                String formattedDiscount = formatter.format(product.getDiscount_price()) + " đ";
                String formattedOriginal = formatter.format(product.getPrice()) + " đ";

                holder.txtPrice.setText(formattedDiscount + "  ");
                holder.txtPrice.append(formattedOriginal);
                holder.txtPrice.setPaintFlags(holder.txtPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (product.getPrice() != null) {
                String formattedPrice = formatter.format(product.getPrice()) + " đ";
                holder.txtPrice.setText(formattedPrice);
            } else {
                holder.txtPrice.setText("Giá: Liên hệ");
            }

            // Hình ảnh sản phẩm
            if (!TextUtils.isEmpty(product.getImage_url())) {
                Glide.with(context)
                        .load(product.getImage_url())
                        .placeholder(R.drawable.placeholder_product)
                        .into(holder.imgProduct);
            } else {
                holder.imgProduct.setImageResource(R.drawable.placeholder_product);
            }


            // Click để mở chi tiết
            holder.itemView.setOnClickListener(v -> {
                if (onProductClickListener != null) {
                    onProductClickListener.onProductClick(product);
                } else {
                    openProductDetails(product);
                }
            });

        } catch (Exception e) {
            Log.e("ProductAdapter", "Lỗi khi binding sản phẩm: " + e.getMessage(), e);
        }
    }

    private void openProductDetails(Product product) {
        Intent intent = new Intent(context, ProductDetailsActivity.class);
        if (product.getId() != null) intent.putExtra("pid", product.getId());
        if (product.getName() != null) intent.putExtra("name", product.getName());
        if (product.getPrice() != null) intent.putExtra("price", product.getPrice());
        if (product.getImage_url() != null) intent.putExtra("image", product.getImage_url());
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            intent.putStringArrayListExtra("description", new ArrayList<>(product.getDescription()));
        }
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateProducts(List<Product> newProducts) {
        this.productList.clear();
        if (newProducts != null) {
            this.productList.addAll(newProducts);
        }
        notifyDataSetChanged();
    }
}
