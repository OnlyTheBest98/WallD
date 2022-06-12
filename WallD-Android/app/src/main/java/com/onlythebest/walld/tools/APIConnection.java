package com.onlythebest.walld.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class APIConnection {
    @Nullable
    public static JSONObject getJSON(@NonNull String url, @Nullable HashMap<String, String> header) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(5000);
            if (header != null) {
                for (String key : header.keySet()) {
                    c.setRequestProperty(key, header.get(key));
                }
            }
            c.connect();
            int status = c.getResponseCode();
            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    //noinspection SpellCheckingInspection
                    JSONTokener tokener = new JSONTokener(sb.toString());
                    try {
                        return new JSONObject(tokener);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    //disconnect error
                }
            }
        }
        return null;
    }

    public static Bitmap getBitmapFromImageUrl(String src) {
        System.out.println("downloading wallpaper from " + src);
        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap b = BitmapFactory.decodeStream(input);
            input.close();
            return b;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}