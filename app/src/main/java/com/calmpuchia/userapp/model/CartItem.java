package com.calmpuchia.userapp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class CartItem implements Parcelable {
    private String productID;
    private String name;         // Sử dụng "name" để khớp với dữ liệu Firebase
    private String image_url;    // Sử dụng "image_url" để khớp với Firebase
    private int price;
    private int quantity;
    private boolean isSelected;

    public CartItem() {
        // Constructor rỗng cho Firebase
    }

    public CartItem(String productID, String name, String image_url, int price, int quantity) {
        this.productID = productID;
        this.name = name;
        this.image_url = image_url;
        this.price = price;
        this.quantity = quantity;
        this.isSelected = false;
    }

    // Parcelable constructor
    protected CartItem(Parcel in) {
        productID = in.readString();
        name = in.readString();
        image_url = in.readString();
        price = in.readInt();
        quantity = in.readInt();
        isSelected = in.readByte() != 0;
    }

    public static final Creator<CartItem> CREATOR = new Creator<CartItem>() {
        @Override
        public CartItem createFromParcel(Parcel in) {
            return new CartItem(in);
        }

        @Override
        public CartItem[] newArray(int size) {
            return new CartItem[size];
        }
    };

    // Getters và Setters
    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productID);
        dest.writeString(name);
        dest.writeString(image_url);
        dest.writeInt(price);
        dest.writeInt(quantity);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }
}
