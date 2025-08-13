# Twitch MCP Server - Eclipse Edition

<a href="https://smithery.ai">
  <img width="380" height="100" src="https://smithery.ai/badge.svg" alt="Deploy with Smithery" />
</a>

**Cloud-ready** Twitch MCP Server with [Smithery](https://smithery.ai) one-click deployment. AI-powered tools for Twitch streamers - chat moderation, stream management, and viewer engagement with zero local setup required.

## 🚀 One-Click Setup with Smithery

**The easiest way to get started:**

1. Visit [Smithery.ai](https://smithery.ai)
2. Search for "Twitch MCP Server" in the registry
3. Click "Deploy" to add it to your workspace
4. Configure your Twitch credentials:
   - **Channel**: Your Twitch username (without #)
   - **OAuth Token**: Generate at [twitchtokengenerator.com](https://twitchtokengenerator.com) 
   - **Client ID**: From your [Twitch Developer Console](https://dev.twitch.tv/console)
   - **Broadcaster ID**: Your numeric user ID ([find here](https://www.streamweasels.com/tools/convert-twitch-username-to-user-id/))
5. Start using AI tools for your Twitch stream!

**No downloads, installations, or technical setup required!**

## 🛠️ Available Tools

| Tool | Description | Use Case |
|------|-------------|----------|
| `sendMessageToChat` | Send message to Twitch chat | Announcements, responses |
| `createTwitchPoll` | Create interactive polls | Viewer engagement |
| `createTwitchPrediction` | Create channel predictions | Interactive betting |
| `createTwitchClip` | Generate clips of current stream | Highlight moments |
| `analyzeChat` | AI analysis of recent chat activity | Understand chat sentiment |
| `getRecentChatLog` | Get last 20 chat messages | Moderation context |
| `timeoutUser` | Timeout users (with smart targeting) | Chat moderation |
| `banUser` | Ban users from chat | Severe violations |
| `updateStreamTitle` | Change your stream title | Dynamic titles |
| `updateStreamCategory` | Update game category | Game switching |

## 💡 Alternative Setup Options

### For Local Development
- Node.js 14+ and Java 11+ available on your system PATH
- A Twitch account with appropriate API credentials

### Setup Configuration
Create a config file with your Twitch credentials (use bare access token, no "oauth:" prefix):

**Windows:** `C:/Users/<you>/AppData/Roaming/twitch-mcp/config.json`  
**macOS:** `~/Library/Application Support/twitch-mcp/config.json`  
**Linux:** `~/.config/twitch-mcp/config.json`

Example `config.json`:
```json
{
  "channel": "YOUR_TWITCH_USERNAME",
  "auth": "YOUR_TWITCH_ACCESS_TOKEN",  
  "clientId": "YOUR_TWITCH_CLIENT_ID",
  "broadcasterId": "YOUR_BROADCASTER_ID",
  "showConnectionMessage": true
}
```

### Recommended AI CLI Tools

**🥇 Primary Recommendation: Gemini CLI**
- Excellent MCP support with reliable connections
- Free tier with generous limits
- Great for day-to-day Twitch moderation and chat management

**🥈 Secondary Recommendation: Qwen Coder CLI** 
- Strong coding-focused AI with good MCP integration
- Particularly good for stream development and technical discussions
- Free and open-source

**⚡ For Power Users: Claude Code**
- Most advanced reasoning capabilities
- Best for complex moderation decisions and nuanced chat analysis
- **Note:** Limited credits - save for heavy lifting tasks

### Configuration Examples

#### Gemini CLI Configuration
Add to your Gemini settings:
```json
{
  "mcpServers": {
    "twitch-mcp": {
      "type": "stdio", 
      "command": "npx",
      "args": [
        "-y",
        "twitch-mcp-server@latest",
        "--config",
        "C:/Users/<you>/AppData/Roaming/twitch-mcp/config.json"
      ]
    }
  }
}
```

#### Qwen Coder Configuration  
Add to your Qwen settings:
```json
{
  "mcpServers": {
    "twitch-mcp": {
      "type": "stdio",
      "command": "npx", 
      "args": [
        "-y",
        "twitch-mcp-server@latest",
        "--config", 
        "C:/Users/<you>/AppData/Roaming/twitch-mcp/config.json"
      ]
    }
  }
}
```

## Features

- **Chat Management:** Send and read messages, recent chat log, chat analysis
- **Moderation:** Timeout/ban users (by username or descriptor keywords)
- **Stream Management:** Update title/category, create clips
- **Interactive Tools:** Create polls and predictions for viewer engagement

## Installation Options

### Option 1: NPX (Recommended)
No installation required - your AI tool will automatically fetch the latest version:
```bash
npx twitch-mcp-server@latest --config /path/to/config.json
```

### Option 2: Global Install
```bash
npm install -g twitch-mcp-server
twitch-mcp-server --config /path/to/config.json
```

### Option 3: Local Development
See [README-developers.md](README-developers.md) for building from source.

## Usage Notes

- Use a bare access token in `auth` field (no `oauth:` prefix needed)
- Only one AI client should connect at a time to avoid conflicts
- Ensure Node.js and Java are installed on the same system as your AI CLI
- The server will automatically handle IRC formatting and API authentication

## Troubleshooting

**Authentication Issues:**
- Confirm `auth` is a bare token (no `oauth:` prefix)  
- Verify `clientId` and scopes match your generated token
- Ensure `broadcasterId` matches your channel ID

**Connection Problems:**
- Check that both Node.js and Java are in your system PATH
- Try running `java -version` and `node -version` to verify installation
- For Windows users: Make sure you're not running from WSL if your AI CLI is on Windows

**NPX Prompts in Headless Environments:**
- The configurations above use `-y` flag for non-interactive execution
- If you encounter prompts, add `--yes` to the npx command

## Support

- For general usage questions, check the troubleshooting section above
- For development and contributions, see [README-developers.md](README-developers.md)  
- For Claude-specific integration help, see [CLAUDE.md](CLAUDE.md)
