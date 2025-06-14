package com.calmpuchia.userapp.model;

import java.util.List;

public class CheckoutOrder {
    private String orderId;
    private String userId;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private List<CartItem> products;
    private int shippingFee;
    private String voucherCode;
    private int voucherDiscount;
    private int totalPrice;
    private String paymentMethod;
    private long timestamp;
    private String status;

    public CheckoutOrder() {
        // Constructor rỗng cho Firebase
    }

    // Getters và Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public List<CartItem> getProducts() { return products; }
    public void setProducts(List<CartItem> products) { this.products = products; }

    public int getShippingFee() { return shippingFee; }
    public void setShippingFee(int shippingFee) { this.shippingFee = shippingFee; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public int getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(int voucherDiscount) { this.voucherDiscount = voucherDiscount; }

    public int getTotalPrice() { return totalPrice; }
    public void setTotalPrice(int totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}