package com.onlythebest.walld.listener;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CompoundButton;

import com.onlythebest.walld.activity.MainActivity;

import java.util.HashSet;
import java.util.Set;

public class CategoryListener implements CompoundButton.OnCheckedChangeListener {

    private final String id;
    private final Context context;

    public CategoryListener(String id, Context context) {
        this.id = id;
        this.context = context;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        SharedPreferences pref = context.getSharedPreferences(MainActivity.SHARED_SETTING, Context.MODE_PRIVATE);
        final Set<String> oldSet = pref.getStringSet("categories", new HashSet<>());
        HashSet<String> newSet = new HashSet<>(oldSet);
        if (b) {
            newSet.add(id);
        } else {
            newSet.remove(id);
        }
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("categories");
        editor.putStringSet("categories", newSet);
        editor.apply();
    }
}
