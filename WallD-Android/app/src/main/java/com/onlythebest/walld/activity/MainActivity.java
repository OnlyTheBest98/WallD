package com.onlythebest.walld.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.onlythebest.walld.R;
import com.onlythebest.walld.services.WallpaperChangerService;

public class MainActivity extends AppCompatActivity {

    public static final String SHARED_SETTING = "settings";
    public static final String USER_AGENT = "walld-android-app/v0.0.2";

    public void openCategorySetting(View view) {
        Intent intent = new Intent(this, CategoryActivity.class);
        startActivity(intent);
    }

    public void openChannelSetting(View view) {
        Intent intent = new Intent(this, ChannelActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);

        findViewById(R.id.category_setting_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCategorySetting(view);
            }
        });

        findViewById(R.id.channel_setting_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openChannelSetting(view);
            }
        });

        findViewById(R.id.discord_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDiscordInvite(view);
            }
        });

        CheckBox b = findViewById(R.id.activate_service_button);
        b.setChecked(pref.getBoolean("active", false));
        b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setServiceState(b);
            }
        });

        b = findViewById(R.id.change_lockscreen_button);
        b.setChecked(pref.getBoolean("lockscreen", false));
        b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setLockscreen(b);
            }
        });

        b = findViewById(R.id.save_to_storage_button);
        b.setChecked(pref.getBoolean("save_to_storage", false));
        b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setSaveToStorage(b);
            }
        });

        startService(new Intent(this, WallpaperChangerService.class));

        boolean p = isWriteStoragePermissionGranted();
        System.out.println("write storage permission: " + p);
    }

    private void setServiceState(boolean active) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("active", active);
        editor.apply();
        if (active) {
            startService(new Intent(this, WallpaperChangerService.class));
        } else {
            stopService(new Intent(this, WallpaperChangerService.class));
        }
    }

    private void setLockscreen(boolean changeLockscreen) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("lockscreen", changeLockscreen);
        editor.apply();
    }

    private void setSaveToStorage(boolean saveToStorage) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("save_to_storage", saveToStorage);
        editor.apply();
    }

    private void openDiscordInvite(View view) {
        Uri webpage = Uri.parse(getString(R.string.discord_invite));
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        startActivity(intent);
    }

    public  boolean isWriteStoragePermissionGranted() {
        String TAG = "Storage Permission:";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // other form of writing images
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted2");
                return true;
            } else {
                Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted2");
            return true;
        }
    }
}