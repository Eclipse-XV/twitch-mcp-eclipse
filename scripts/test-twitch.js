#!/usr/bin/env node

const { exec } = require('child_process');
const fs = require('fs');
const path = require('path');

const CONFIG_PATHS = [
    path.join(process.env.HOME || process.env.USERPROFILE, '.config', 'twitch-mcp', 'config.json'),
    path.join(process.env.APPDATA || path.join(process.env.HOME, 'Library', 'Application Support'), 'twitch-mcp', 'config.json'),
    path.join(process.cwd(), 'config.json'),
    path.join(process.cwd(), 'twitch-config.json')
];

function findConfigFile() {
    for (const configPath of CONFIG_PATHS) {
        if (fs.existsSync(configPath)) {
            return configPath;
        }
    }
    return null;
}

function checkEnvironmentVariables() {
    const requiredVars = ['TWITCH_BOT_TOKEN', 'TWITCH_CHANNEL'];
    const missingVars = requiredVars.filter(varName => !process.env[varName]);
    
    if (missingVars.length > 0) {
        console.log('❌ Missing required environment variables:');
        missingVars.forEach(varName => {
            console.log(`   - ${varName}`);
        });
        console.log('');
        console.log('Please set these environment variables:');
        console.log('   export TWITCH_BOT_TOKEN="your_bot_token"');
        console.log('   export TWITCH_CHANNEL="your_channel_name"');
        console.log('');
        return false;
    }
    
    return true;
}

function checkConfigFile() {
    const configPath = findConfigFile();
    
    if (!configPath) {
        console.log('⚠️  No config file found in standard locations:');
        CONFIG_PATHS.forEach(p => console.log(`   - ${p}`));
        console.log('');
        return false;
    }
    
    try {
        const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
        const requiredFields = ['channel', 'auth', 'clientId'];
        const missingFields = requiredFields.filter(field => !config[field]);
        
        if (missingFields.length > 0) {
            console.log('❌ Config file missing required fields:');
            missingFields.forEach(field => console.log(`   - ${field}`));
            return false;
        }
        
        console.log('✅ Config file found and valid:', configPath);
        return true;
    } catch (error) {
        console.log('❌ Config file is invalid JSON:', error.message);
        return false;
    }
}

async function testTwitchConnection() {
    console.log('🧪 Testing Twitch MCP Server Configuration...');
    console.log('');
    
    // Check configuration
    const hasEnvVars = checkEnvironmentVariables();
    const hasConfigFile = checkConfigFile();
    
    if (!hasEnvVars && !hasConfigFile) {
        console.log('❌ No valid configuration found. Please set up either:');
        console.log('   1. Environment variables (TWITCH_BOT_TOKEN, TWITCH_CHANNEL), or');
        console.log('   2. A config.json file in one of the standard locations');
        process.exit(1);
    }
    
    console.log('');
    console.log('📡 Configuration looks good!');
    console.log('');
    console.log('🚀 To run a full test with the MCP server:');
    console.log('   1. Start the server: npm run start');
    console.log('   2. Connect your AI assistant (Claude, etc.) to the MCP server');
    console.log('   3. Try sending a test message to chat');
    console.log('');
    console.log('💡 Example MCP commands to test:');
    console.log('   - "Send a hello message to my Twitch chat"');
    console.log('   - "Get the recent chat messages"');
    console.log('   - "Analyze the recent chat activity"');
    console.log('');
    console.log('✅ Manual test setup complete!');
}

if (require.main === module) {
    testTwitchConnection().catch((error) => {
        console.error('Test failed:', error.message);
        process.exit(1);
    });
}