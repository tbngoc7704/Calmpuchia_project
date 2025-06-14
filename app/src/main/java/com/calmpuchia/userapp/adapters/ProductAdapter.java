package com.calmpuchia.userapp.adapters;

import android.content.Context;
import android.content.Intent;
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

    // Interface cho product click listener
    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.onProductClickListener = listener;
    }

    // Method để update danh sách sản phẩm
    public void updateProducts(List<Product> newProducts) {
        if (newProducts != null) {
            this.productList.clear();
            this.productList.addAll(newProducts);
            notifyDataSetChanged();
        }
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtPrice;

        public ProductViewHolder(View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
        }
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        if (position < 0 || position >= productList.size()) {
            return;
        }

        Product product = productList.get(position);
        if (product == null) {
            return;
        }

        // Set tên sản phẩm
        if (product.getName() != null) {
            holder.txtName.setText(product.getName());
        } else {
            holder.txtName.setText("Tên sản phẩm không có");
        }

        // Set giá sản phẩm với format tiền tệ
        if (product.getPrice() != null) {
            try {
                NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
                String formattedPrice = formatter.format(product.getPrice()) + " đ";
                holder.txtPrice.setText(formattedPrice);
            } catch (Exception e) {
                holder.txtPrice.setText(product.getPrice() + " đ");
            }
        } else {
            holder.txtPrice.setText("Giá: Liên hệ");
        }

        // Load hình ảnh sản phẩm
        if (product.getImage_url() != null && !product.getImage_url().isEmpty()) {
            Glide.with(context)
                    .load(product.getImage_url())
                    .placeholder(R.mipmap.ic_producta)
                    .error(R.mipmap.ic_producta)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.mipmap.ic_producta);
        }

        // Set click listener cho item
        holder.itemView.setOnClickListener(v -> {
            if (onProductClickListener != null) {
                onProductClickListener.onProductClick(product);
            } else {
                // Fallback - mở chi tiết sản phẩm trực tiếp
                openProductDetails(product);
            }
        });
    }

    private void openProductDetails(Product product) {
        Intent intent = new Intent(context, ProductDetailsActivity.class);

        // Truyền dữ liệu sản phẩm
        if (product.getId() != null) {
            intent.putExtra("pid", product.getId());
        }
        if (product.getProductId() != null) {
            intent.putExtra("product_id", product.getProductId());
        }
        if (product.getName() != null) {
            intent.putExtra("name", product.getName());
        }
        if (product.getPrice() != null) {
            intent.putExtra("price", product.getPrice());
        }
        if (product.getImage_url() != null) {
            intent.putExtra("image", product.getImage_url());
        }
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            intent.putStringArrayListExtra("description", new ArrayList<>(product.getDescription()));
        }

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    // Helper methods
    public void clearProducts() {
        if (productList != null) {
            productList.clear();
            notifyDataSetChanged();
        }
    }

    public void addProduct(Product product) {
        if (product != null && productList != null) {
            productList.add(product);
            notifyItemInserted(productList.size() - 1);
        }
    }

    public void removeProduct(int position) {
        if (productList != null && position >= 0 && position < productList.size()) {
            productList.remove(position);
            notifyItemRemoved(position);
        }
    }
}