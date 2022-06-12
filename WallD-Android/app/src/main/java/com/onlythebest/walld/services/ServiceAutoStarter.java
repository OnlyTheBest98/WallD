package com.onlythebest.walld.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class ServiceAutoStarter extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        context.startService(new Intent(context, WallpaperChangerService.class));
    }
}