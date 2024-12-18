# Discord-Inventor

This extension enables you to build bots that can interact with Discord servers, send and monitor messages, and handle real-time events.

## Features
- Real-time **monitoring** of Discord channels using WebSockets.
- Send, edit, and delete messages in Discord channels.
- Cooldown functionality to prevent rate-limiting.
- Custom event handling for new messages and errors.

## Installation
1. Download the extension `.aix` file.
2. Import it into your **MIT App Inventor** project:
   - Go to **Extensions** > **Import Extension** > Upload the file.

## Permissions
Ensure that your app has the following permissions:
```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

---

## Usage

### 1. **Authentication**
Set your bot token using the `SetBotToken` method.

**Example:**
```blocks
call DiscordEvents.SetBotToken("YOUR_DISCORD_BOT_TOKEN")
```

---

### 2. **Monitor Events**
Use `StartMonitoring` to connect and listen to real-time events like new messages.

**Block Example:**
```blocks
call DiscordEvents.StartMonitoring("EventTag")
```

**Event Listener:**
- **`OnDiscordEvent`**: Triggered when a Discord event occurs.
   - **Parameters**: `eventType`, `eventInfoList`
   - Example Output:
     ```
     eventType: MESSAGE_CREATE
     content: Hello World!
     messageId: 123456789
     channelId: 987654321
     serverId: 1122334455
     userId: 1111111111
     username: ExampleUser
     ```

---

### 3. **Send, Edit, and Delete Messages**

- **SendMessage**:
  - Sends a message to a channel.
  ```blocks
  call DiscordText.SendMessage("ServerID", "ChannelID", "Hello, Discord!", "Tag", 10)
  ```
  Parameters:
   - `ServerID`, `ChannelID`, `Content`, `Tag`, `CooldownSeconds`

- **EditMessage**:
  - Edits an existing message.
  ```blocks
  call DiscordText.EditMessage("ServerID", "ChannelID", "MessageID", "New Content", "Tag", 10)
  ```

- **DeleteMessage**:
  - Deletes a message from the channel.
  ```blocks
  call DiscordText.DeleteMessage("ServerID", "ChannelID", "MessageID", "Tag", 10)
  ```

**Event Listeners:**
- **`OnResponse`**: Triggered on success.
- **`OnError`**: Triggered on failure.
- **`OnCooldown`**: Triggered when a command hits cooldown.

---

## Developer Guide: Creating New Commands

Follow these steps to add new commands to the extension:

### 1. **Setup**
Ensure your development environment includes:
- Java 8+
- Gradle
- Dependencies:
  - `org.java-websocket:1.5.3`
  - `org.slf4j:slf4j-simple:1.7.36`

### 2. **Define New Methods**
Create new methods in the appropriate Java files:

- **`DiscordText.java`** for message-based actions.
- **`DiscordEvents.java`** for real-time event handling.

**Template for a New Command:**
```java
@SimpleFunction(description = "Your command description.")
public void NewCommand(String param1, String param2, String tag) {
    if (isOnCooldown("NewCommand", cooldownSeconds, tag)) {
        return;
    }
    makeRequest("/endpoint", "POST", tag, param1, param2);
}
```

### 3. **Test Your Changes**
1. Compile the project using Gradle:
   ```bash
   ./gradlew build
   ```
2. Import the new `.aix` into **MIT App Inventor**.
3. Test the functionality in a sample project.

---

## Contributing
To contribute:
1. Fork this repository.
2. Implement new features or bug fixes.
3. Submit a Pull Request with a detailed description.

---

## Support
For issues or feature requests, create an issue in the repository.

---

**Happy Bot Building! 🎉**  
**BosonsHiggs Team**
