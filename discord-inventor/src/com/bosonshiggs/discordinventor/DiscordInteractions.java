package com.bosonshiggs.discordinventor;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailList;


@DesignerComponent(
    version = 1,
    description = "Discord Interactions Extension - Allows creating interactions with buttons, dropdowns, and modals on Discord.",
    iconName = "icon.png",
    helpUrl = "https://github.com/iagolirapassos/Discord-Inventor"
)
public class DiscordInteractions extends AndroidNonvisibleComponent {
    private final TokenManager tokenManager;
    private final DiscordRequestHelper requestHelper;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public DiscordInteractions(ComponentContainer container) {
        super(container.$form());
        tokenManager = new TokenManager();
        requestHelper = new DiscordRequestHelper(
            tokenManager,
            new Handler(Looper.getMainLooper()),
            form.getApplicationContext()
        );
    }

    @SimpleFunction(description = "Creates a message with a button in a specified Discord channel.")
	public void CreateButton(String guildId, String channelId, String content, String label, String customId, String style, boolean ephemeral, String tag) {
		try {
		    JSONObject json = new JSONObject();
		    json.put("content", content);

		    // Configurar a flag ephemeral se necessário
		    if (ephemeral) {
		        json.put("flags", 64); // Tornar a mensagem ephemeral
		    }

		    // Configurar os componentes (botões)
		    JSONArray components = new JSONArray();
		    JSONObject button = new JSONObject();
		    button.put("type", 2); // Tipo de componente: botão
		    button.put("label", label); // Texto no botão
		    button.put("style", Integer.parseInt(style)); // Estilo do botão
		    button.put("custom_id", customId); // ID personalizado para identificar o botão

		    JSONArray actionRow = new JSONArray();
		    actionRow.put(button);

		    JSONObject actionRowObject = new JSONObject();
		    actionRowObject.put("type", 1); // Tipo de ação: linha de ação
		    actionRowObject.put("components", actionRow);

		    components.put(actionRowObject);
		    json.put("components", components);

		    // Endpoint para criar a mensagem no canal
		    String endpoint = "/channels/" + channelId + "/messages";

		    requestHelper.makeRequestWithBody(
		        endpoint,
		        "POST",
		        tag,
		        json.toString(),
		        guildId,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "Message with button created successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, "Failed to create message with button: " + error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error creating message with button: " + e.getMessage()));
		}
	}


    @SimpleFunction(description = "Creates a message with a dropdown menu in a Discord channel.")
	public void CreateMessageWithDropdown(String guildId, String channelId, String content, YailList options, String customId, String tag) {
		try {
		    JSONObject json = new JSONObject();
		    json.put("content", content);

		    // Configurar o dropdown
		    JSONArray components = new JSONArray();
		    JSONObject dropdown = new JSONObject();
		    dropdown.put("type", 3); // Tipo: Dropdown
		    dropdown.put("custom_id", customId); // ID para identificar o dropdown

		    // Adicionar opções ao dropdown
		    JSONArray optionArray = new JSONArray();
		    for (Object option : options.toArray()) {
		        JSONObject optionObject = new JSONObject();
		        optionObject.put("label", option.toString()); // Texto visível
		        optionObject.put("value", option.toString()); // Valor enviado
		        optionArray.put(optionObject);
		    }
		    dropdown.put("options", optionArray);

		    // Adicionar dropdown à linha de ação
		    JSONArray actionRow = new JSONArray();
		    actionRow.put(dropdown);

		    JSONObject actionRowObject = new JSONObject();
		    actionRowObject.put("type", 1); // Tipo de ação: Linha de ação
		    actionRowObject.put("components", actionRow);

		    components.put(actionRowObject);
		    json.put("components", components);

		    // Endpoint para criar a mensagem
		    String endpoint = "/channels/" + channelId + "/messages";

		    requestHelper.makeRequestWithBody(
		        endpoint,
		        "POST",
		        tag,
		        json.toString(),
		        guildId,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "Message with dropdown created successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, "Failed to create message with dropdown: " + error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error creating message with dropdown: " + e.getMessage()));
		}
	}


    @SimpleFunction(description = "Creates a modal to be triggered by a button interaction.")
	public void TriggerModal(String interactionId, String interactionToken, String modalTitle, String modalCustomId, YailList inputs, String tag) {
		try {
		    JSONObject json = new JSONObject();
		    json.put("type", 9); // Tipo: Modal

		    JSONObject data = new JSONObject();
		    data.put("title", modalTitle); // Título do modal
		    data.put("custom_id", modalCustomId); // ID único do modal

		    // Configurar os campos de entrada
		    JSONArray components = new JSONArray();
		    for (Object input : inputs.toArray()) {
		        JSONObject textInput = new JSONObject();
		        textInput.put("type", 4); // Tipo: Input de texto
		        textInput.put("custom_id", input.toString()); // ID do campo
		        textInput.put("label", "Enter " + input.toString()); // Rótulo
		        textInput.put("style", 1); // Estilo: Linha única
		        textInput.put("required", true); // Campo obrigatório

		        JSONArray actionRow = new JSONArray();
		        actionRow.put(textInput);

		        JSONObject actionRowObject = new JSONObject();
		        actionRowObject.put("type", 1); // Tipo de ação: Linha de ação
		        actionRowObject.put("components", actionRow);

		        components.put(actionRowObject);
		    }

		    data.put("components", components);
		    json.put("data", data);

		    // Endpoint para responder à interação com o modal
		    String endpoint = "/interactions/" + interactionId + "/" + interactionToken + "/callback";

		    requestHelper.makeRequestWithBody(
		        endpoint,
		        "POST",
		        tag,
		        json.toString(),
		        null,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "Modal triggered successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, "Failed to trigger modal: " + error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error triggering modal: " + e.getMessage()));
		}
	}

	
	@SimpleFunction(description = "Responds to an interaction to avoid timeout errors.")
	public void RespondToInteraction(String interactionId, String interactionToken, String content, boolean defer, String tag) {
		try {
		    JSONObject json = new JSONObject();

		    if (defer) {
		        // Defer the interaction (acknowledge it but send a response later)
		        json.put("type", 5); // Deferred channel message with source
		    } else {
		        // Respond immediately with a message
		        json.put("type", 4); // Channel message with source
		        JSONObject data = new JSONObject();
		        data.put("content", content);
		        json.put("data", data);
		    }

		    String endpoint = "/interactions/" + interactionId + "/" + interactionToken + "/callback";

		    requestHelper.makeRequestWithBody(
		        endpoint,
		        "POST",
		        tag,
		        json.toString(),
		        null,
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "Interaction response sent successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, "Failed to respond to interaction: " + error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error responding to interaction: " + e.getMessage()));
		}
	}
	
	@SimpleFunction(description = "Deletes an ephemeral interaction response or follow-up message.")
	public void DeleteEphemeralMessage(String applicationId, String interactionToken, boolean isOriginal, String messageId, String tag) {
		try {
		    String endpoint;

		    if (isOriginal) {
		        // Endpoint para deletar a mensagem original
		        endpoint = "/webhooks/" + applicationId + "/" + interactionToken + "/messages/@original";
		    } else {
		        // Endpoint para deletar uma mensagem de follow-up específica
		        endpoint = "/webhooks/" + applicationId + "/" + interactionToken + "/messages/" + messageId;
		    }

		    requestHelper.makeRequest(
		        endpoint,
		        "DELETE",
		        tag,
		        null, // Sem corpo
		        null, // Sem guildId
		        new DiscordRequestHelper.Callback() {
		            @Override
		            public void onResponse(String tag, String response) {
		                Response(tag, "Ephemeral message deleted successfully.");
		            }

		            @Override
		            public void onError(String tag, String error) {
		                Error(tag, "Failed to delete ephemeral message: " + error);
		            }
		        }
		    );
		} catch (Exception e) {
		    uiHandler.post(() -> Error(tag, "Error deleting ephemeral message: " + e.getMessage()));
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

