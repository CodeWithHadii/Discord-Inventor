package com.bosonshiggs.discordinventor;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    protected void setBotToken(Context context, String token) {
        // Store the bot token in TinyDb
        SharedPreferences sharedPreferences = context.getSharedPreferences("TokenManager", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("botToken", token);
        editor.commit();
    }

    protected String getBotToken(Context context) {
        // Retrieve the bot token from TinyDb
        SharedPreferences sharedPreferences = context.getSharedPreferences("TokenManager", Context.MODE_PRIVATE);
        return sharedPreferences.getString("botToken", "");
    }
}
