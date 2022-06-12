package com.onlythebest.walld.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.onlythebest.walld.R;
import com.onlythebest.walld.activity.MainActivity;
import com.onlythebest.walld.tools.APIConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperChangerService extends Service {

    public static final String API_URL = "http://firster.ddns.net:8000/image.json";
    public static final long SYNC_INTERVAL = 10_000;

    private Handler handler;
    private BroadcastReceiver screenOnReceiver;

    public WallpaperChangerService() {
    }

    private void changeWallpaper(Bitmap wallpaper) {
        WallpaperManager manager = WallpaperManager.getInstance(getApplicationContext());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
                manager.setBitmap(wallpaper, null, false, WallpaperManager.FLAG_SYSTEM);
                if (pref.getBoolean("lockscreen", false)) {
                    manager.setBitmap(wallpaper, null, false, WallpaperManager.FLAG_LOCK);
                }
            } else {
                manager.setBitmap(wallpaper);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Runnable serviceRunnable = new Runnable() {
        @Override
        public void run() {
            ExecutorService executor = Executors.newSingleThreadExecutor();

            HashMap<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("User-Agent", MainActivity.USER_AGENT);
            SharedPreferences pref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
            requestHeaders.put("categories", String.join(",", pref.getStringSet("categories", new HashSet<>())));
            requestHeaders.put("excluded-channels", String.join(",", pref.getStringSet("channels", new HashSet<>())));
            final String lastReceived = pref.getString("last_image", null);

            executor.execute(() -> {
                //Background work here
                JSONObject wallpaperJSON = APIConnection.getJSON(API_URL, requestHeaders);
                if (wallpaperJSON == null) {
                    return;
                }

                try {
                    if (lastReceived != null && lastReceived.compareTo(wallpaperJSON.getString("datetime")) == 0) {
                        return;
                    } else {
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("last_image", wallpaperJSON.getString("datetime"));
                        editor.apply();
                    }
                    Bitmap wallpaper = APIConnection.getBitmapFromImageUrl(wallpaperJSON.getString("url"));
                    handler.post(() -> {
                        //Main Thread work here
                        if (wallpaper != null) {
                            changeWallpaper(wallpaper);
                            try {
                                if (pref.getBoolean("save_to_storage", false)) {
                                    saveImage(wallpaper, pref.getString("last_image", "error"));
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

            handler.postDelayed(serviceRunnable, SYNC_INTERVAL);
        }
    };

    private void saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".png");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + getString(R.string.app_name));
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            System.out.println("Image uri: " + imageUri);
            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    // get image path
                    String imagesDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                            + File.separator + getString(R.string.app_name);
                    File dir = new File(imagesDir);
                    dir.mkdirs();
                    File image = new File(imagesDir, name + ".png");
                    System.out.println("file location: " + image);
                    fos = new FileOutputStream(image);
                } else {
                    System.out.println("permission missing...");
                    return;
                }
            } else {
                // get image path
                String imagesDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + File.separator + getString(R.string.app_name);
                File dir = new File(imagesDir);
                dir.mkdirs();
                File image = new File(imagesDir, name + ".png");
                System.out.println("file location: " + image);
                fos = new FileOutputStream(image);
            }
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);
        Objects.requireNonNull(fos).close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (handler != null) {
            System.out.println("Service already started");
            return START_STICKY;
        }
        if(!getApplicationContext().getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE).getBoolean("active", false)) {
            // service should not be active
            return START_STICKY;
        }
        System.out.println("Service started");
        handler = new Handler();
        handler.post(serviceRunnable);

        Intent notificationIntent = new Intent(this, MainActivity.class);

        // PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
        //         notificationIntent, 0);

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "service",
                    "WallD",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel);

            notification = new Notification.Builder(this, channel.getId())
                    // .setSmallIcon(R.mipmap.ic_launcher)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentTitle("WallD image bot")
                    .setChannelId(channel.getId())
                    .build();
        } else {
            notification = new NotificationCompat.Builder(this, "service")
                    // .setSmallIcon(R.mipmap.ic_launcher)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentTitle("WallD image bot")
                    .setChannelId("service")
                    .build();
        }

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        // StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
        //          .detectLeakedClosableObjects()
        //          .build());

        screenOnReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // Some action
                System.out.println("Screen is turned on");
            }
        };

        registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(serviceRunnable);
        stopForeground(true);
        stopSelf();
        unregisterReceiver(screenOnReceiver);
        handler = null;
        super.onDestroy();
    }
}