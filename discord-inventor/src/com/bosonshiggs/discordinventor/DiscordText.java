package com.bosonshiggs.discordinventor;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

@DesignerComponent(
	version = 1, 
	description = "Discord Bot Message Extension - Allows sending, editing, deleting, and monitoring messages on Discord.", 
	iconName = "icon.png",
	helpUrl = "https://github.com/iagolirapasssos/Discord-Inventor"
)
public class DiscordText extends AndroidNonvisibleComponent {
	private final TokenManager tokenManager;
	private final String BASE_URL = "https://discord.com/api/v10";
	private final Handler uiHandler = new Handler(Looper.getMainLooper());
	private final HashMap<String, Long> cooldowns = new HashMap<>();

	public DiscordText(ComponentContainer container) {
		super(container.$form());
		tokenManager = new TokenManager();
	}

	private boolean isOnCooldown(String command, int cooldownSeconds, String tag) {
		long currentTime = System.currentTimeMillis() / 1000;
		long lastTime = cooldowns.getOrDefault(command, 0L);
		long timeLeft = cooldownSeconds - (currentTime - lastTime);

		if (timeLeft > 0) {
			Cooldown(tag, command, timeLeft);
			return true;
		}
		cooldowns.put(command, currentTime);
		return false;
	}

	@SimpleFunction(description = "Sends a message to the specified Discord channel in the given server.")
	public void SendMessage(String guildId, String channelId, String content, String tag, int cooldownSeconds) {
		if (isOnCooldown("SendMessage", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + channelId + "/messages", "POST", tag, content, guildId);
	}

	@SimpleFunction(description = "Edits an existing message in the specified channel within the given server.")
	public void EditMessage(String guildId, String channelId, String messageId, String newContent, String tag,
			int cooldownSeconds) {
		if (isOnCooldown("EditMessage", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + channelId + "/messages/" + messageId, "PATCH", tag, newContent, guildId);
	}

	@SimpleFunction(description = "Deletes a message from the specified channel in the given server.")
	public void DeleteMessage(String guildId, String channelId, String messageId, String tag, int cooldownSeconds) {
		if (isOnCooldown("DeleteMessage", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + channelId + "/messages/" + messageId, "DELETE", tag, null, guildId);
	}

	@SimpleFunction(description = "Pins a message in the specified Discord channel.")
	public void PinMessage(String guildId, String channelId, String messageId, String tag, int cooldownSeconds) {
		if (isOnCooldown("PinMessage", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + channelId + "/pins/" + messageId, "PUT", tag, null, guildId);
	}

	@SimpleFunction(description = "Unpins a message in the specified Discord channel.")
	public void UnpinMessage(String guildId, String channelId, String messageId, String tag, int cooldownSeconds) {
		if (isOnCooldown("UnpinMessage", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + channelId + "/pins/" + messageId, "DELETE", tag, null, guildId);
	}

	@SimpleFunction(description = "Fetches messages from a specified Discord channel.")
	public void GetMessages(String guildId, String channelId, int limit, String tag, int cooldownSeconds) {
		if (isOnCooldown("GetMessages", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + channelId + "/messages?limit=" + limit, "GET", tag, null, guildId);
	}

	@SimpleFunction(description = "Creates a thread in a Discord channel with a specified message.")
	public void CreateThread(String guildId, String channelId, String messageId, String threadName,
			int autoArchiveDuration, String tag, int cooldownSeconds) {
		if (isOnCooldown("CreateThread", cooldownSeconds, tag)) {
			return;
		}
		new Thread(() -> {
			try {
				JSONObject json = new JSONObject();
				json.put("name", threadName);
				json.put("auto_archive_duration", autoArchiveDuration);

				makeRequestWithBody("/channels/" + channelId + "/messages/" + messageId + "/threads", "POST", tag,
						json.toString(), guildId);
			} catch (Exception e) {
				uiHandler.post(() -> Error(tag, "Error creating thread: " + e.getMessage()));
			}
		}).start();
	}

	@SimpleFunction(description = "Updates the name or auto-archive duration of a thread in a Discord channel.")
	public void UpdateThread(String guildId, String threadId, String newName, int autoArchiveDuration, String tag,
			int cooldownSeconds) {
		if (isOnCooldown("UpdateThread", cooldownSeconds, tag)) {
			return;
		}
		new Thread(() -> {
			try {
				JSONObject json = new JSONObject();
				if (!newName.isEmpty()) {
					json.put("name", newName);
				}
				if (autoArchiveDuration > 0) {
					json.put("auto_archive_duration", autoArchiveDuration);
				}
				makeRequestWithBody("/channels/" + threadId, "PATCH", tag, json.toString(), guildId);
			} catch (Exception e) {
				uiHandler.post(() -> Error(tag, "Error updating thread: " + e.getMessage()));
			}
		}).start();
	}

	@SimpleFunction(description = "Deletes a thread from a Discord channel.")
	public void DeleteThread(String guildId, String threadId, String tag, int cooldownSeconds) {
		if (isOnCooldown("DeleteThread", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + threadId, "DELETE", tag, null, guildId);
	}

	@SimpleFunction(description = "Adds a user to a thread in a Discord channel.")
	public void AddUserToThread(String guildId, String threadId, String userId, String tag, int cooldownSeconds) {
		if (isOnCooldown("AddUserToThread", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + threadId + "/thread-members/" + userId, "PUT", tag, null, guildId);
	}

	@SimpleFunction(description = "Removes a user from a thread in a Discord channel.")
	public void RemoveUserFromThread(String guildId, String threadId, String userId, String tag, int cooldownSeconds) {
		if (isOnCooldown("RemoveUserFromThread", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + threadId + "/thread-members/" + userId, "DELETE", tag, null, guildId);
	}

	@SimpleFunction(description = "Gets a list of all active threads in a specified Discord channel.")
	public void ListActiveThreads(String guildId, String channelId, String tag, int cooldownSeconds) {
		if (isOnCooldown("ListActiveThreads", cooldownSeconds, tag)) {
			return;
		}
		makeRequest("/channels/" + channelId + "/threads/active", "GET", tag, null, guildId);
	}

	/* Events */
	@SimpleEvent(description = "Triggered when a successful response is received.")
	public void Response(String tag, String response) {
		EventDispatcher.dispatchEvent(this, "Response", tag, response);
	}

	@SimpleEvent(description = "Triggered when an error occurs.")
	public void Error(String tag, String error) {
		EventDispatcher.dispatchEvent(this, "Error", tag, error);
	}

	@SimpleEvent(description = "Triggered when a command is on cooldown. Returns the remaining cooldown time.")
	public void Cooldown(String tag, String command, long secondsRemaining) {
		EventDispatcher.dispatchEvent(this, "Cooldown", tag, command, secondsRemaining);
	}

	/* Private methods */
	private void makeRequestWithBody(String endpoint, String method, String tag, String jsonBody, String guildId) {
		new Thread(() -> {
			try {
				URL url = new URL(BASE_URL + endpoint);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod(method);
				conn.setRequestProperty("Authorization", "Bot " + tokenManager.getBotToken(form.getApplicationContext()));
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setDoOutput(true);

				if (jsonBody != null) {
					OutputStream os = conn.getOutputStream();
					os.write(jsonBody.getBytes());
					os.flush();
					os.close();
				}

				int responseCode = conn.getResponseCode();
				if (responseCode >= 200 && responseCode < 300) {
					uiHandler.post(
							() -> Response(tag, "Success for guild: " + guildId + " with code: " + responseCode));
				} else {
					uiHandler.post(() -> Error(tag, "Error for guild: " + guildId + " with code: " + responseCode));
				}
			} catch (Exception e) {
				uiHandler.post(() -> Error(tag, "Error for guild: " + guildId + " - " + e.getMessage()));
			}
		}).start();
	}

	private void makeRequest(String endpoint, String method, String tag, String content, String guildId) {
		new Thread(() -> {
			try {
				URL url = new URL(BASE_URL + endpoint);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod(method);
				conn.setRequestProperty("Authorization", "Bot " + tokenManager.getBotToken(form.getApplicationContext()));
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setDoOutput(true);

				if (content != null) {
					JSONObject json = new JSONObject();
					json.put("content", content);
					json.put("guild_id", guildId);
					OutputStream os = conn.getOutputStream();
					os.write(json.toString().getBytes());
					os.flush();
					os.close();
				}

				int responseCode = conn.getResponseCode();
				if (responseCode >= 200 && responseCode < 300) {
					uiHandler.post(
							() -> Response(tag, "Success for guild: " + guildId + " with code: " + responseCode));
				} else {
					uiHandler.post(() -> Error(tag, "Error for guild: " + guildId + " with code: " + responseCode));
				}
			} catch (Exception e) {
				uiHandler.post(() -> Error(tag, "Error for guild: " + guildId + " - " + e.getMessage()));
			}
		}).start();
	}
}
