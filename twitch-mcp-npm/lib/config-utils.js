const fs = require('fs');
const path = require('path');
const os = require('os');

/**
 * Load configuration from various sources
 */
function loadConfig(argv) {
  // Load from environment variables
  const envConfig = {
    channel: process.env.TWITCH_CHANNEL,
    auth: process.env.TWITCH_AUTH,
    clientId: process.env.TWITCH_CLIENT_ID,
    broadcasterId: process.env.TWITCH_BROADCASTER_ID,
    showConnectionMessage: process.env.TWITCH_SHOW_CONNECTION_MESSAGE === 'true'
  };

  // Load from optional config file
  const configFilePath = resolveConfigFilePath(argv && argv.config);
  const fileConfig = readConfigFile(configFilePath);

  // Load from command line arguments
  const argConfig = {
    channel: argv.channel,
    auth: argv.auth,
    clientId: argv['client-id'],
    broadcasterId: argv['broadcaster-id'],
    showConnectionMessage: argv['show-connection-message'],
    jarPath: argv['jar-path']
  };

  // Merge configurations with priority: argv > file > env
  return {
    channel: argConfig.channel || fileConfig.channel || envConfig.channel,
    auth: argConfig.auth || fileConfig.auth || envConfig.auth,
    clientId: argConfig.clientId || fileConfig.clientId || envConfig.clientId,
    broadcasterId: argConfig.broadcasterId || fileConfig.broadcasterId || envConfig.broadcasterId,
    showConnectionMessage: argConfig.showConnectionMessage !== undefined
      ? argConfig.showConnectionMessage
      : (fileConfig.showConnectionMessage !== undefined ? fileConfig.showConnectionMessage : envConfig.showConnectionMessage),
    jarPath: argConfig.jarPath || fileConfig.jarPath
  };
}

/**
 * Determine default config file path based on OS.
 */
function getDefaultConfigPath() {
  const platform = os.platform();
  const homeDir = os.homedir();
  if (platform === 'win32') {
    const appData = process.env.APPDATA || path.join(homeDir, 'AppData', 'Roaming');
    return path.join(appData, 'twitch-mcp', 'config.json');
  }
  if (platform === 'darwin') {
    return path.join(homeDir, 'Library', 'Application Support', 'twitch-mcp', 'config.json');
  }
  return path.join(homeDir, '.config', 'twitch-mcp', 'config.json');
}

function resolveConfigFilePath(explicitPath) {
  if (explicitPath && typeof explicitPath === 'string') {
    try {
      const absolute = path.isAbsolute(explicitPath)
        ? explicitPath
        : path.join(process.cwd(), explicitPath);
      return fs.existsSync(absolute) ? absolute : null;
    } catch {
      return null;
    }
  }
  const defaultPath = getDefaultConfigPath();
  return fs.existsSync(defaultPath) ? defaultPath : null;
}

function readConfigFile(configPath) {
  if (!configPath) return {};
  try {
    const rawContent = fs.readFileSync(configPath, 'utf8');
    const parsed = JSON.parse(rawContent);
    // Normalize keys that may be provided in different styles
    return {
      channel: parsed.channel || parsed.twitchChannel || parsed.TWITCH_CHANNEL,
      auth: parsed.auth || parsed.twitchAuth || parsed.TWITCH_AUTH,
      clientId: parsed.clientId || parsed.client_id || parsed.TWITCH_CLIENT_ID,
      broadcasterId: parsed.broadcasterId || parsed.broadcaster_id || parsed.TWITCH_BROADCASTER_ID,
      showConnectionMessage: typeof parsed.showConnectionMessage === 'boolean'
        ? parsed.showConnectionMessage
        : (parsed.TWITCH_SHOW_CONNECTION_MESSAGE === true || parsed.TWITCH_SHOW_CONNECTION_MESSAGE === 'true'),
      jarPath: parsed.jarPath
    };
  } catch (error) {
    console.warn(`Warning: Failed to read config file at ${configPath}: ${error.message}`);
    return {};
  }
}

/**
 * Validate configuration
 */
function validateConfig(config) {
  const errors = [];
  
  if (!config.channel) {
    errors.push('Twitch channel name is required');
  }
  
  if (!config.auth) {
    errors.push('Twitch OAuth token is required');
  }
  
  if (!config.clientId) {
    errors.push('Twitch Client ID is required');
  }
  
  if (!config.broadcasterId) {
    errors.push('Twitch Broadcaster ID is required');
  }
  
  // Accept auth token with or without oauth: prefix; normalize later in CLI
  
  return errors;
}

module.exports = {
  loadConfig,
  validateConfig,
  getDefaultConfigPath
};