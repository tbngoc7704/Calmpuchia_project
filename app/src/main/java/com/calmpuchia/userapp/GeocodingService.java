package com.calmpuchia.userapp;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeocodingService {
    private static final String TAG = "GeocodingService";
    private Context context;

    public GeocodingService(Context context) {
        this.context = context;
    }

    /**
     * Chuyển đổi address thành lat/lng sử dụng Android Geocoder
     * @param address Địa chỉ cần chuyển đổi
     * @param listener Callback để nhận kết quả
     */
    public void getLocationFromAddress(String address, OnGeocodingListener listener) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(address, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address location = addresses.get(0);
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onLocationFound(lat, lng);
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onLocationNotFound("Không tìm thấy tọa độ cho địa chỉ: " + address);
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding error: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError("Lỗi geocoding: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Backup method sử dụng HTTP API nếu Geocoder không hoạt động
     */
    public void getLocationFromAddressWithAPI(String address, OnGeocodingListener listener) {
        String url = "https://nominatim.openstreetmap.org/search?format=json&q=" +
                Uri.encode(address) + "&limit=1";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "YourAppName/1.0")  // Thay tên app của bạn
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError("API call failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(responseBody);
                        if (jsonArray.length() > 0) {
                            JSONObject location = jsonArray.getJSONObject(0);
                            double lat = Double.parseDouble(location.getString("lat"));
                            double lng = Double.parseDouble(location.getString("lon"));

                            new Handler(Looper.getMainLooper()).post(() -> {
                                listener.onLocationFound(lat, lng);
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                listener.onLocationNotFound("Không tìm thấy địa chỉ");
                            });
                        }
                    } catch (JSONException e) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            listener.onError("JSON parsing error: " + e.getMessage());
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onError("API response not successful: " + response.message());
                    });
                }
            }
        });
    }

    public interface OnGeocodingListener {
        void onLocationFound(double lat, double lng);
        void onLocationNotFound(String message);
        void onError(String error);
    }
}
