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

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

@DesignerComponent(
    version = 1, 
    description = "Discord Channel Monitor Extension - Monitors Discord channels in real-time using WebSocket without exceeding API limits.", 
    iconName = "icon.png",
	helpUrl = "https://github.com/iagolirapasssos/Discord-Inventor"
)
public class DiscordEvents extends AndroidNonvisibleComponent {
    private WebSocketClient webSocketClient;
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
    @DesignerProperty(editorType = "string", defaultValue = "")
    @SimpleProperty(description = "Sets the Discord Bot Token for authentication.")
    public void BotToken(String token) {
        this.botToken = token;
        new TokenManager().setBotToken(form.getApplicationContext(), token);
    }

    @SimpleProperty(description = "Returns the bot token.")
    public String GetBotToken() {
        return botToken;
    }

    /**
     * Starts the WebSocket connection to monitor Discord events.
     * 
     * @param tag A custom tag to identify the response.
     */
    @SimpleFunction(description = "Starts monitoring Discord channels in real-time.")
    public void StartMonitoring(String tag) {
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
            Error(tag, e.getMessage());
        }
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
            eventInfo.add("eventType: " + eventType);
            eventInfo.add("content: " + data.optString("content", "N/A"));
            eventInfo.add("messageId: " + data.optString("id", "N/A"));
            eventInfo.add("channelId: " + data.optString("channel_id", "N/A"));
            eventInfo.add("serverId: " + data.optString("guild_id", "N/A"));
            eventInfo.add(
                    "userId: " + (data.has("author") ? data.getJSONObject("author").optString("id", "N/A") : "N/A"));
            eventInfo.add("username: "
                    + (data.has("author") ? data.getJSONObject("author").optString("username", "N/A") : "N/A"));
        } catch (Exception e) {
            eventInfo.add("error: " + e.getMessage());
        }
        return YailList.makeList(eventInfo);
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
        if (eventTimer != null) {
            eventTimer.cancel();
        }

        eventTimer = new Timer();
        eventTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JSONObject event = eventQueue.poll();
                if (event != null) {
                    processEvent(event);
                }
            }
        }, 0, 1000); // Delay mínimo de 1 segundo entre eventos
    }

    private void processEvent(JSONObject event) {
        try {
            String eventType = event.optString("t");
            JSONObject data = event.optJSONObject("d");

            if (data != null && !eventType.equals("PRESENCE_UPDATE")) { // Filtra PRESENCE_UPDATE
                YailList eventInfoList = parseEventInfo(eventType, data);
                uiHandler.post(() -> DiscordEvent(eventType, eventInfoList));
            }
        } catch (Exception e) {
            uiHandler.post(() -> Error("EVENT_PROCESSING", e.getMessage()));
        }
    }
}
