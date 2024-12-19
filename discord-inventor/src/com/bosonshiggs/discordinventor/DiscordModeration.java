package com.bosonshiggs.discordinventor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.runtime.util.YailList;

@DesignerComponent(
    version = 1, 
    description = "Discord Moderation Extension - Provides tools for server moderation, including checking user permissions, roles, and more.", 
    iconName = "icon.png",
	helpUrl = "https://github.com/iagolirapasssos/Discord-Inventor"
)
public class DiscordModeration extends AndroidNonvisibleComponent {
    private final TokenManager tokenManager;
    private DiscordRequestHelper requestHelper;
    
    private final String BASE_URL = "https://discord.com/api/v10";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public DiscordModeration(ComponentContainer container) {
        super(container.$form());
        tokenManager = new TokenManager();
        requestHelper = new DiscordRequestHelper(
		    tokenManager,
		    new Handler(Looper.getMainLooper()),
		    form.getApplicationContext()
		);
    }

    @SimpleFunction(description = "Checks if a user has a specific permission in a guild.")
	public void CheckUserPermission(String guildId, String userId, String permission, String tag) {
		requestHelper.makeRequest(
		    "/guilds/" + guildId + "/members/" + userId,
		    "GET",
		    tag,
		    null,
		    guildId,
		    new DiscordRequestHelper.Callback() {
		        @Override
		        public void onResponse(String tag, String response) {
		            try {
		                JSONObject data = new JSONObject(response);
		                boolean hasPermission = data.getJSONObject("roles").toString().contains(permission);
		                Response(tag, "User has permission: " + hasPermission);
		            } catch (Exception e) {
		                Error(tag, "Error parsing permission data: " + e.getMessage());
		            }
		        }

		        @Override
		        public void onError(String tag, String error) {
		            Error(tag, error);
		        }
		    }
		);
	}

    @SimpleFunction(description = "Checks if a user is an administrator or the owner of the bot.")
	public void CheckAdminOrOwner(String guildId, String userId, String tag) {
		requestHelper.makeRequest(
		    "/guilds/" + guildId + "/members/" + userId,
		    "GET",
		    tag,
		    null,
		    guildId,
		    new DiscordRequestHelper.Callback() {
		        @Override
		        public void onResponse(String tag, String response) {
		            try {
		                JSONObject data = new JSONObject(response);
		                boolean isAdmin = data.getBoolean("is_admin");
		                boolean isOwner = data.getBoolean("is_owner");
		                Response(tag, "Is Admin: " + isAdmin + ", Is Owner: " + isOwner);
		            } catch (Exception e) {
		                Error(tag, "Error parsing admin/owner data: " + e.getMessage());
		            }
		        }

		        @Override
		        public void onError(String tag, String error) {
		            Error(tag, error);
		        }
		    }
		);
	}


    @SimpleFunction(description = "Kicks a user from the specified guild.")
	public void KickUser(String guildId, String userId, String reason, String tag) {
		try {
		    JSONObject json = new JSONObject();
		    if (reason != null && !reason.isEmpty()) {
		        json.put("reason", reason);
		    }
		    
		    requestHelper.makeRequestWithBody(
		        "/guilds/" + guildId + "/members/" + userId,
		        "DELETE",
		        tag,
		        json.toString(),
		        guildId,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "User kicked successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error kicking user: " + e.getMessage()));
		}
	}


    @SimpleFunction(description = "Bans a user from the specified guild.")
	public void BanUser(String guildId, String userId, String reason, int deleteMessageDays, String tag) {
		try {
		    JSONObject json = new JSONObject();
		    if (reason != null && !reason.isEmpty()) {
		        json.put("reason", reason);
		    }
		    json.put("delete_message_days", deleteMessageDays);

		    requestHelper.makeRequestWithBody(
		        "/guilds/" + guildId + "/bans/" + userId,
		        "PUT",
		        tag,
		        json.toString(),
		        guildId,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "User banned successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error banning user: " + e.getMessage()));
		}
	}

    @SimpleFunction(description = "Unbans a user from the specified guild.")
	public void UnbanUser(String guildId, String userId, String tag) {
		requestHelper.makeRequest(
		    "/guilds/" + guildId + "/bans/" + userId,
		    "DELETE",
		    tag,
		    null,
		    guildId,
		    new DiscordRequestHelper.Callback() {
		        @Override
		        public void onResponse(String tag, String response) {
		            Response(tag, "User unbanned successfully.");
		        }

		        @Override
		        public void onError(String tag, String error) {
		            Error(tag, error);
		        }
		    }
		);
	}

    
    @SimpleFunction(description = "Warns a user in a specified channel.")
	public void WarnUser(String guildId, String channelId, String userId, String warningMessage, String tag) {
		new Thread(() -> {
		    try {
		        JSONObject json = new JSONObject();
		        json.put("content", warningMessage);
		        json.put("guild_id", guildId);

		        requestHelper.makeRequestWithBody("/channels/" + channelId + "/messages", "POST", tag, json.toString(), guildId,
					new DiscordRequestHelper.Callback() {
						@Override
						public void onResponse(String tag, String response) {
							Response(tag, response); // Chama o método Response da extensão
						}

						@Override
						public void onError(String tag, String error) {
							Error(tag, error); // Chama o método Error da extensão
						}
					});
		    } catch (Exception e) {
		        uiHandler.post(() -> Error(tag, "Error warning user: " + e.getMessage()));
		    }
		}).start();
	}
	
 	@SimpleFunction(description = "Mutes a user in a specific guild.")
	public void MuteUser(String guildId, String userId, boolean mute, String tag) {
		try {
		    JSONObject json = new JSONObject();
		    json.put("mute", mute);

		    requestHelper.makeRequestWithBody(
		        "/guilds/" + guildId + "/members/" + userId,
		        "PATCH",
		        tag,
		        json.toString(),
		        guildId,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "User mute status updated successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, error);
		            }
		        }
		    );
		} catch (Exception e) {
		    Error(tag, "Error muting user: " + e.getMessage());
		}
	}

    @SimpleFunction(description = "Timeout a user in a specific guild.")
	public void TimeoutUser(String guildId, String userId, long timeoutSeconds, String tag) {
		try {
		    JSONObject json = new JSONObject();
		    if (timeoutSeconds > 0) {
		        long timestamp = System.currentTimeMillis() + (timeoutSeconds * 1000);
		        json.put("communication_disabled_until", new java.util.Date(timestamp).toInstant().toString());
		    } else {
		        json.put("communication_disabled_until", JSONObject.NULL);
		    }

		    requestHelper.makeRequestWithBody(
		        "/guilds/" + guildId + "/members/" + userId,
		        "PATCH",
		        tag,
		        json.toString(),
		        guildId,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "User timeout updated successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, error);
		            }
		        }
		    );
		} catch (Exception e) {
		    Error(tag, "Error setting timeout: " + e.getMessage());
		}
	}

    @SimpleFunction(description = "Gets the list of banned users in a guild.")
	public void GetBans(String guildId, String tag) {
		requestHelper.makeRequest(
		    "/guilds/" + guildId + "/bans",
		    "GET",
		    tag,
		    null,
		    guildId,
		    new DiscordRequestHelper.Callback() {
		        @Override
		        public void onResponse(String tag, String response) {
		            Response(tag, response);
		        }

		        @Override
		        public void onError(String tag, String error) {
		            Error(tag, error);
		        }
		    }
		);
	}
	
	@SimpleFunction(description = "Sets a user's nickname in a specific guild.")
    public void SetNickname(String guildId, String userId, String nickname, String tag) {
        try {
            JSONObject json = new JSONObject();
            json.put("nick", nickname);

            requestHelper.makeRequestWithBody(
                "/guilds/" + guildId + "/members/" + userId,
                "PATCH",
                tag,
                json.toString(),
                guildId,
                new DiscordRequestHelper.Callback() {
                    @Override
                    public void onResponse(String tag, String response) {
                        Response(tag, "Nickname updated successfully.");
                    }

                    @Override
                    public void onError(String tag, String error) {
                        Error(tag, error);
                    }
                }
            );
        } catch (Exception e) {
            uiHandler.post(() -> Error(tag, "Error setting nickname: " + e.getMessage()));
        }
    }

    @SimpleFunction(description = "Adds a role to a user in a specific guild.")
    public void AddRole(String guildId, String userId, String roleId, String tag) {
        requestHelper.makeRequest(
            "/guilds/" + guildId + "/members/" + userId + "/roles/" + roleId,
            "PUT",
            tag,
            null,
            guildId,
            new DiscordRequestHelper.Callback() {
                @Override
                public void onResponse(String tag, String response) {
                    Response(tag, "Role added successfully.");
                }

                @Override
                public void onError(String tag, String error) {
                    Error(tag, error);
                }
            }
        );
    }

    @SimpleFunction(description = "Removes a role from a user in a specific guild.")
    public void RemoveRole(String guildId, String userId, String roleId, String tag) {
        requestHelper.makeRequest(
            "/guilds/" + guildId + "/members/" + userId + "/roles/" + roleId,
            "DELETE",
            tag,
            null,
            guildId,
            new DiscordRequestHelper.Callback() {
                @Override
                public void onResponse(String tag, String response) {
                    Response(tag, "Role removed successfully.");
                }

                @Override
                public void onError(String tag, String error) {
                    Error(tag, error);
                }
            }
        );
    }

    @SimpleFunction(description = "Fetches the audit logs of a guild.")
    public void FetchAuditLogs(String guildId, String actionType, String tag) {
        String endpoint = "/guilds/" + guildId + "/audit-logs";
        if (actionType != null && !actionType.isEmpty()) {
            endpoint += "?action_type=" + actionType;
        }

        requestHelper.makeRequest(
            endpoint,
            "GET",
            tag,
            null,
            guildId,
            new DiscordRequestHelper.Callback() {
                @Override
                public void onResponse(String tag, String response) {
                    Response(tag, response);
                }

                @Override
                public void onError(String tag, String error) {
                    Error(tag, error);
                }
            }
        );
    }

    @SimpleFunction(description = "Bulk deletes messages in a channel.")
	public void BulkDeleteMessages(String channelId, YailList messageIds, String tag) {
		try {
		    // Converte YailList para JSONArray
		    org.json.JSONArray jsonArray = new org.json.JSONArray();
		    for (Object messageId : messageIds.toArray()) {
		        jsonArray.put(messageId.toString());
		    }

		    // Cria o objeto JSON com os IDs das mensagens
		    JSONObject json = new JSONObject();
		    json.put("messages", jsonArray);

		    requestHelper.makeRequestWithBody(
		        "/channels/" + channelId + "/messages/bulk-delete",
		        "POST",
		        tag,
		        json.toString(),
		        null,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "Messages deleted successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error deleting messages: " + e.getMessage()));
		}
	}



    @SimpleEvent(description = "Triggered when a successful response is received.")
    public void Response(String tag, String response) {
        EventDispatcher.dispatchEvent(this, "Response", tag, response);
    }

    @SimpleEvent(description = "Triggered when an error occurs.")
    public void Error(String tag, String error) {
        EventDispatcher.dispatchEvent(this, "Error", tag, error);
    }
}
