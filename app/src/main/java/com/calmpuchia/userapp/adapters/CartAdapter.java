package com.calmpuchia.userapp.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.calmpuchia.userapp.R;
import com.calmpuchia.userapp.model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> itemList;
    private final OnItemCheckListener onItemCheckListener;

    public interface OnItemCheckListener {
        void onItemCheckChanged();
    }

    public CartAdapter(List<CartItem> itemList, OnItemCheckListener listener) {
        this.itemList = itemList;
        this.onItemCheckListener = listener;
    }

    public class CartViewHolder extends RecyclerView.ViewHolder {
        TextView name, price, quantityText;
        ImageView image;
        CheckBox checkBox;
        ImageButton btnMinus, btnPlus, btnDelete;

        public CartViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemName);
            price = itemView.findViewById(R.id.itemPrice);
            image = itemView.findViewById(R.id.itemImage);
            checkBox = itemView.findViewById(R.id.itemCheckBox);
            quantityText = itemView.findViewById(R.id.itemQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    @Override
    public CartViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(v);
    }

    @Override
    public void onBindViewHolder(CartViewHolder holder, int position) {
        CartItem item = itemList.get(position);
        holder.name.setText(item.getName());
        holder.price.setText(item.getPrice() + " đ");
        holder.quantityText.setText(String.valueOf(item.getQuantity()));
        holder.checkBox.setChecked(item.isSelected());

        String imageUrl = item.getImage_url();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.image.getContext())
                    .load(imageUrl)
                    .placeholder(R.mipmap.ic_productb) // thêm placeholder nếu cần
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.mipmap.ic_productb); // ảnh mặc định nếu không có ảnh
        }

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setSelected(isChecked);
            onItemCheckListener.onItemCheckChanged();
        });

        holder.btnPlus.setOnClickListener(v -> {
            int qty = item.getQuantity() + 1;
            item.setQuantity(qty);
            holder.quantityText.setText(String.valueOf(qty));
            updateItemInFirebase(item);
            notifyItemChanged(holder.getAdapterPosition());
            onItemCheckListener.onItemCheckChanged(); // cập nhật tổng tiền
        });

        holder.btnMinus.setOnClickListener(v -> {
            int qty = item.getQuantity();
            if (qty > 1) {
                qty--;
                item.setQuantity(qty);
                holder.quantityText.setText(String.valueOf(qty));
                updateItemInFirebase(item);
                notifyItemChanged(holder.getAdapterPosition());
                onItemCheckListener.onItemCheckChanged(); // cập nhật tổng tiền
            }
        });


        holder.btnDelete.setOnClickListener(v -> {
            deleteItemFromFirebase(item);
            int pos = holder.getAdapterPosition();
            itemList.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, itemList.size());
            onItemCheckListener.onItemCheckChanged();
        });
    }

    private void updateItemInFirebase(CartItem item) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference()
                .child("Cart List").child("User View").child(userId)
                .child("Products").child(item.getProductID());
        itemRef.setValue(item);
    }

    private void deleteItemFromFirebase(CartItem item) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String productId = item.getProductID();


        if (productId == null || productId.isEmpty()) return;

        DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference()
                .child("Cart List").child("User View").child(userId)
                .child("Products").child(productId);
        itemRef.removeValue();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public List<CartItem> getItemList() {
        return itemList;
    }
}
