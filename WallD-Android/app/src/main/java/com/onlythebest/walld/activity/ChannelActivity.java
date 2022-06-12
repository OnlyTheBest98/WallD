package com.onlythebest.walld.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.onlythebest.walld.R;
import com.onlythebest.walld.listener.ChannelListener;
import com.onlythebest.walld.tools.APIConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChannelActivity extends AppCompatActivity {

    public static final String API_URL = "http://firster.ddns.net:8000/channels.json";

    private final HashMap<String, String> requestHeaders = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);
        setTitle(R.string.channel_setting_title);

        requestHeaders.put("User-Agent", MainActivity.USER_AGENT);
        SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
        Set<String> categories = pref.getStringSet("categories", new HashSet<>());
        requestHeaders.put("categories", String.join(",", categories));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            //Background work here
            JSONObject json = APIConnection.getJSON(API_URL, requestHeaders);
            handler.post(() -> {
                //UI Thread work here
                if (json != null) {
                    listOptions(json);
                }
            });
        });
    }

    private void listOptions(@NonNull JSONObject json) {
        findViewById(R.id.channel_progress_bar).setVisibility(View.INVISIBLE);
        LinearLayout field = findViewById(R.id.channel_field);
        SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
        Set<String> tickedChannels = pref.getStringSet("channels", new HashSet<>());

        TreeSet<String> keySet = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String s, String t) {
                try {
                    String v1 = json.getString(s);
                    String v2 = json.getString(t);
                    return v1.compareTo(v2);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            keySet.add(key);
        }

        for (String key : keySet) {
            try {
                String value = json.getString(key);
                CheckBox b = new CheckBox(this);
                b.setText(value);
                if (tickedChannels.contains(key)) {
                    b.setChecked(true);
                }

                b.setOnCheckedChangeListener(new ChannelListener(key, getApplicationContext()));
                field.addView(b);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}