package com.bosonshiggs.discordinventor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.PropertyCategory;

import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.YailDictionary;

import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.YailDictionary;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.json.JSONException;

@DesignerComponent(
    version = 1, 
    description = "Discord Channel Monitor Extension - Monitors Discord channels in real-time using WebSocket without exceeding API limits.", 
    iconName = "icon.png",
	helpUrl = "https://github.com/iagolirapasssos/Discord-Inventor"
)
public class DiscordEvents extends AndroidNonvisibleComponent {
    private WebSocketClient webSocketClient;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private String botToken;
    private final String GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";
    private Timer heartbeatTimer;

    private final BlockingQueue<JSONObject> eventQueue = new LinkedBlockingQueue<>();
    private Timer eventTimer;

    public DiscordEvents(ComponentContainer container) {
        super(container.$form());
    }

    /**
     * Sets the Bot Token for Discord API authentication.
     * 
     * @param token The Discord Bot Token.
     */
    @DesignerProperty(
		editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
		defaultValue = ""
	)
	@SimpleProperty
	public void BotToken(String token) {
		this.botToken = token;
		new TokenManager().setBotToken(form.getApplicationContext(), token);
	}

    @SimpleProperty(description = "Returns the bot token.",
    				category = PropertyCategory.BEHAVIOR)
    public String BotToken() {
        return botToken;
    }

    /**
     * Starts the WebSocket connection to monitor Discord events.
     * 
     * @param tag A custom tag to identify the response.
     */
    @SimpleFunction(description = "Starts monitoring Discord channels in real-time.")
	public void StartMonitoring(String tag) {
		new Thread(() -> {
		    try {
		        URI gatewayUri = new URI(GATEWAY_URL);
		        webSocketClient = new WebSocketClient(gatewayUri) {
		            @Override
		            public void onOpen(ServerHandshake handshake) {
		                uiHandler.post(() -> Response(tag, "Connected to Discord Gateway"));
		                sendIdentifyPayload();
		            }

		            @Override
		            public void onMessage(String message) {
		                try {
		                    JSONObject json = new JSONObject(message);
		                    int opCode = json.optInt("op");

		                    if (opCode == 10) { // HELLO opcode - Start heartbeat
		                        int heartbeatInterval = json.getJSONObject("d").optInt("heartbeat_interval");
		                        startHeartbeat(heartbeatInterval);
		                        startEventProcessing(); // Inicia processamento controlado
		                    }

		                    String eventType = json.optString("t");

		                    // Ignorar o evento PRESENCE_UPDATE
		                    if (eventType.equals("PRESENCE_UPDATE")) {
		                        return; // Não processa o evento
		                    }

		                    JSONObject data = json.optJSONObject("d");
		                    if (data != null) {
		                        addEventToQueue(eventType, data); // Adiciona evento à fila
		                    }

		                } catch (Exception e) {
		                    uiHandler.post(() -> Error("PARSE_ERROR", e.getMessage()));
		                }
		            }

		            @Override
		            public void onClose(int code, String reason, boolean remote) {
		                uiHandler.post(() -> Response(tag, "Connection closed: " + reason));
		                reconnectWebSocket(tag);
		            }

		            @Override
		            public void onError(Exception ex) {
		                uiHandler.post(() -> Error(tag, ex.getMessage()));
		                reconnectWebSocket(tag);
		            }
		        };

		        webSocketClient.connect();
		    } catch (Exception e) {
		        uiHandler.post(() -> Error(tag, e.getMessage()));
		    }
		}).start();
	}


    /**
     * Parses event information into a YailList.
     * 
     * @param eventType The type of the event.
     * @param data      The JSON object containing the event data.
     * @return A YailList with event details.
     */
    private YailList parseEventInfo(String eventType, JSONObject data) {
		List<String> eventInfo = new ArrayList<>();
		try {
		    eventInfo.add(eventType);
		    eventInfo.add(data.toString()); // Retorna todo o conteúdo da API como uma string JSON.
		} catch (Exception e) {
		    eventInfo.add("error: " + e.getMessage());
		}
		return YailList.makeList(eventInfo);
	}



	private JSONObject getGuildContents(JSONObject data) {
		JSONObject guildContents = new JSONObject();
		try {
		    guildContents.put("guildId", data.optString("id", "N/A"));
		    guildContents.put("guildName", data.optString("name", "N/A"));

		    // Adicionar canais da guilda
		    if (data.has("channels")) {
		        guildContents.put("channels", data.getJSONArray("channels"));
		    }

		    // Adicionar membros da guilda
		    if (data.has("members")) {
		        guildContents.put("members", data.getJSONArray("members"));
		    }

		    // Adicionar roles da guilda
		    if (data.has("roles")) {
		        guildContents.put("roles", data.getJSONArray("roles"));
		    }
		} catch (Exception e) {
		    try {
		        guildContents.put("error", e.getMessage());
		    } catch (Exception ignored) {}
		}
		return guildContents;
	}


    /**
     * Starts the heartbeat timer to maintain the connection.
     * 
     * @param interval The interval in milliseconds to send heartbeat.
     */
    private void startHeartbeat(int interval) {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
        heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (webSocketClient != null && webSocketClient.isOpen()) {
                    try {
                        JSONObject heartbeat = new JSONObject();
                        heartbeat.put("op", 1);
                        heartbeat.put("d", JSONObject.NULL);
                        webSocketClient.send(heartbeat.toString());
                    } catch (Exception e) {
                        uiHandler.post(() -> Error("HEARTBEAT", e.getMessage()));
                    }
                }
            }
        }, 0, interval);
    }

    /**
     * Attempts to reconnect the WebSocket after a connection loss.
     * 
     * @param tag The custom tag for identifying responses.
     */
    private void reconnectWebSocket(String tag) {
        uiHandler.postDelayed(() -> {
            uiHandler.post(() -> Response(tag, "Attempting to reconnect..."));
            StartMonitoring(tag);
        }, 5000); // Wait 5 seconds before attempting to reconnect
    }

    /**
     * Stops the WebSocket connection.
     * 
     * @param tag A custom tag to identify the response.
     */
    @SimpleFunction(description = "Stops monitoring Discord channels.")
    public void StopMonitoring(String tag) {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
        Response(tag, "Monitoring stopped.");
    }

    /**
     * Sends the IDENTIFY payload to authenticate with the Discord Gateway.
     */
    private void sendIdentifyPayload() {
        try {
            JSONObject identify = new JSONObject();
            identify.put("op", 2);
            JSONObject data = new JSONObject();
            data.put("token", botToken);
            data.put("intents", 3276799); // Includes MESSAGE_CONTENT intent
            JSONObject properties = new JSONObject();
            properties.put("os", "android");
            properties.put("browser", "appinventor");
            properties.put("device", "appinventor");
            data.put("properties", properties);
            identify.put("d", data);

            webSocketClient.send(identify.toString());
        } catch (Exception e) {
            uiHandler.post(() -> Error("IDENTIFY", e.getMessage()));
        }
    }
    
    @SimpleFunction(description = "Converts a JSON-formatted string into a YailDictionary.")
	public YailDictionary ParseJsonString(String jsonString) {
		try {
		    // Convert string to JSONObject
		    JSONObject jsonObject = new JSONObject(jsonString);

		    // Convert JSONObject to YailDictionary
		    YailDictionary yailDict = new YailDictionary();

		    // Iterate through keys using keys() method and ensuring String type
		    Iterator<?> keys = jsonObject.keys();
		    while (keys.hasNext()) {
		        String key = keys.next().toString();
		        Object value = jsonObject.get(key);

		        // If the value is a JSONObject, convert it recursively
		        if (value instanceof JSONObject) {
		            value = ParseJsonString(value.toString());
		        }
		        yailDict.put(key, value);
		    }
		    return yailDict;

		} catch (JSONException e) {
		    // Return a YailDictionary with error details
		    YailDictionary errorDict = new YailDictionary();
		    errorDict.put("error", e.getMessage());
		    return errorDict;
		}
	}

    /**
     * Event triggered for any Discord event.
     * 
     * @param tag           The type of event.
     * @param eventInfoList A list containing all event details.
     */
    @SimpleEvent(description = "Triggered when a Discord event occurs. Returns tag and event information.")
    public void DiscordEvent(String tag, YailList eventInfoList) {
        EventDispatcher.dispatchEvent(this, "DiscordEvent", tag, eventInfoList);
    }

    /**
     * Event triggered when a response is received.
     * 
     * @param tag      A custom tag to identify the response.
     * @param response The response message.
     */
    @SimpleEvent(description = "Triggered when a response is received.")
    public void Response(String tag, String response) {
        EventDispatcher.dispatchEvent(this, "Response", tag, response);
    }

    /**
     * Event triggered when an error occurs.
     * 
     * @param tag   A custom tag to identify the error.
     * @param error The error message.
     */
    @SimpleEvent(description = "Triggered when an error occurs.")
    public void Error(String tag, String error) {
        EventDispatcher.dispatchEvent(this, "Error", tag, error);
    }

    // Adiciona evento à fila com controle de rate limit
    private void addEventToQueue(String eventType, JSONObject data) {
        try {
            JSONObject event = new JSONObject();
            event.put("t", eventType);
            event.put("d", data);
            eventQueue.offer(event);
        } catch (Exception e) {
            uiHandler.post(() -> Error("QUEUE_ERROR", e.getMessage()));
        }
    }

    // Inicia o processamento da fila com intervalo de 1 segundo (pode ser ajustado)
    private void startEventProcessing() {
		executor.scheduleAtFixedRate(() -> {
		    try {
		        JSONObject event = eventQueue.poll();
		        if (event != null) {
		            processEvent(event);
		        }
		    } catch (Exception e) {
		        uiHandler.post(() -> Error("EVENT_PROCESSING", e.getMessage()));
		    }
		}, 0, 1, TimeUnit.SECONDS); // Ajuste o intervalo conforme necessário
	}

    private void processEvent(JSONObject event) {
        try {
            String eventType = event.optString("t");
            JSONObject data = event.optJSONObject("d");

            if (data != null && !eventType.equals("PRESENCE_UPDATE")) { // Filtra PRESENCE_UPDATE
                YailList eventInfoList = parseEventInfo(eventType, data);
                uiHandler.post(() -> {
					if (!eventInfoList.isEmpty()) {
						DiscordEvent(eventType, eventInfoList);
					}
				});

            }
        } catch (Exception e) {
            uiHandler.post(() -> Error("EVENT_PROCESSING", e.getMessage()));
        }
    }
}
