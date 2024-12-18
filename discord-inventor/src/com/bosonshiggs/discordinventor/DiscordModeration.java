package com.bosonshiggs.discordinventor;

import android.os.Handler;
import android.os.Looper;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.*;
import com.google.appinventor.components.runtime.*;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

@DesignerComponent(
        version = 1,
        description = "Discord Moderation Extension - Provides tools for server moderation, including checking user permissions, roles, and more.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "https://cdn.prod.website-files.com/6257adef93867e50d84d30e2/636e0a6a49cf127bf92de1e2_icon_clyde_blurple_RGB.png"
)
public class DiscordModeration extends AndroidNonvisibleComponent {

    private final String BASE_URL = "https://discord.com/api/v10";
    private String botToken = "";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public DiscordModeration(ComponentContainer container) {
        super(container.$form());
    }

    @SimpleProperty(description = "Sets the Discord Bot Token for authentication.")
    public void SetBotToken(String token) {
        this.botToken = token;
    }

    @SimpleFunction(description = "Checks if a user has a specific permission in a guild.")
    public void CheckUserPermission(String guildId, String userId, String permission, String tag) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/guilds/" + guildId + "/members/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bot " + botToken);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    String response = readInputStream(conn.getInputStream());
                    JSONObject data = new JSONObject(response);
                    JSONObject roles = data.getJSONObject("roles");

                    boolean hasPermission = false;
                    for (Object roleObj : roles.keySet()) {
                        String role = (String) roleObj; // Cast necessário
                        if (role.contains(permission)) {
                            hasPermission = true;
                            break;
                        }
                    }

                    boolean finalHasPermission = hasPermission;
                    uiHandler.post(() -> OnResponse(tag, "User has permission: " + finalHasPermission));
                } else {
                    uiHandler.post(() -> OnError(tag, "Failed to fetch user permissions. Response Code: " + responseCode));
                }
            } catch (Exception e) {
                uiHandler.post(() -> OnError(tag, "Error: " + e.getMessage()));
            }
        }).start();
    }

    @SimpleFunction(description = "Checks if a user is an administrator or the owner of the bot.")
    public void CheckAdminOrOwner(String guildId, String userId, String tag) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/guilds/" + guildId + "/members/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bot " + botToken);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    String response = readInputStream(conn.getInputStream());
                    JSONObject data = new JSONObject(response);
                    boolean isAdmin = data.getBoolean("is_admin");
                    boolean isOwner = data.getBoolean("is_owner");

                    uiHandler.post(() -> OnResponse(tag, "Is Admin: " + isAdmin + ", Is Owner: " + isOwner));
                } else {
                    uiHandler.post(() -> OnError(tag, "Failed to check admin/owner status. Response Code: " + responseCode));
                }
            } catch (Exception e) {
                uiHandler.post(() -> OnError(tag, "Error: " + e.getMessage()));
            }
        }).start();
    }

    @SimpleFunction(description = "Kicks a user from the specified guild.")
    public void KickUser(String guildId, String userId, String reason, String tag) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/guilds/" + guildId + "/members/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bot " + botToken);
                conn.setRequestProperty("Content-Type", "application/json");

                if (reason != null && !reason.isEmpty()) {
                    JSONObject json = new JSONObject();
                    json.put("reason", reason);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(json.toString().getBytes());
                        os.flush();
                    }
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 204) {
                    uiHandler.post(() -> OnResponse(tag, "User kicked successfully."));
                } else {
                    uiHandler.post(() -> OnError(tag, "Failed to kick user. Response Code: " + responseCode));
                }
            } catch (Exception e) {
                uiHandler.post(() -> OnError(tag, "Error: " + e.getMessage()));
            }
        }).start();
    }

    @SimpleFunction(description = "Bans a user from the specified guild.")
    public void BanUser(String guildId, String userId, String reason, int deleteMessageDays, String tag) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/guilds/" + guildId + "/bans/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bot " + botToken);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject json = new JSONObject();
                if (reason != null && !reason.isEmpty()) {
                    json.put("reason", reason);
                }
                json.put("delete_message_days", deleteMessageDays);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 204) {
                    uiHandler.post(() -> OnResponse(tag, "User banned successfully."));
                } else {
                    uiHandler.post(() -> OnError(tag, "Failed to ban user. Response Code: " + responseCode));
                }
            } catch (Exception e) {
                uiHandler.post(() -> OnError(tag, "Error: " + e.getMessage()));
            }
        }).start();
    }

    @SimpleFunction(description = "Unbans a user from the specified guild.")
    public void UnbanUser(String guildId, String userId, String tag) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/guilds/" + guildId + "/bans/" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bot " + botToken);

                int responseCode = conn.getResponseCode();
                if (responseCode == 204) {
                    uiHandler.post(() -> OnResponse(tag, "User unbanned successfully."));
                } else {
                    uiHandler.post(() -> OnError(tag, "Failed to unban user. Response Code: " + responseCode));
                }
            } catch (Exception e) {
                uiHandler.post(() -> OnError(tag, "Error: " + e.getMessage()));
            }
        }).start();
    }

    private String readInputStream(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @SimpleEvent(description = "Triggered when a successful response is received.")
    public void OnResponse(String tag, String response) {
        EventDispatcher.dispatchEvent(this, "OnResponse", tag, response);
    }

    @SimpleEvent(description = "Triggered when an error occurs.")
    public void OnError(String tag, String error) {
        EventDispatcher.dispatchEvent(this, "OnError", tag, error);
    }
}

