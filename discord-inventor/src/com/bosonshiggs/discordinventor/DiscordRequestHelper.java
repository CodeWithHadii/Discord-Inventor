package com.bosonshiggs.discordinventor;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;

public class DiscordRequestHelper {

    private final String BASE_URL = "https://discord.com/api/v10";
    private final TokenManager tokenManager;
    private final Handler uiHandler;
    private final Context context;

    public DiscordRequestHelper(TokenManager tokenManager, Handler uiHandler, Context context) {
        this.tokenManager = tokenManager;
        this.uiHandler = uiHandler;
        this.context = context;
    }

    public void makeRequestWithBody(String endpoint, String method, String tag, String jsonBody, String guildId, 
                                    Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bot " + tokenManager.getBotToken(context));
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                if (jsonBody != null) {
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(jsonBody.getBytes());
                        os.flush();
                    }
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    uiHandler.post(() -> callback.onResponse(tag, "Success for guild: " + guildId + " with code: " + responseCode));
                } else {
                    uiHandler.post(() -> callback.onError(tag, "Error for guild: " + guildId + " with code: " + responseCode));
                }
            } catch (Exception e) {
                uiHandler.post(() -> callback.onError(tag, "Error for guild: " + guildId + " - " + e.getMessage()));
            }
        }).start();
    }

    public void makeRequest(String endpoint, String method, String tag, String content, String guildId, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Authorization", "Bot " + tokenManager.getBotToken(context));
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                if (content != null) {
                    JSONObject json = new JSONObject();
                    json.put("content", content);
                    json.put("guild_id", guildId);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(json.toString().getBytes());
                        os.flush();
                    }
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    uiHandler.post(() -> callback.onResponse(tag, "Success for guild: " + guildId + " with code: " + responseCode));
                } else {
                    uiHandler.post(() -> callback.onError(tag, "Error for guild: " + guildId + " with code: " + responseCode));
                }
            } catch (Exception e) {
                uiHandler.post(() -> callback.onError(tag, "Error for guild: " + guildId + " - " + e.getMessage()));
            }
        }).start();
    }

    public interface Callback {
        void onResponse(String tag, String response);
        void onError(String tag, String error);
    }
}

