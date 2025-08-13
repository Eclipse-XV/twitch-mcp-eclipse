#!/usr/bin/env node

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const { JAR_PATH } = require('../scripts/build-jar');

function checkJavaHome() {
    return new Promise((resolve) => {
        const { exec } = require('child_process');
        exec('java -version', (error) => {
            if (error) {
                console.error('❌ Java not found. Please ensure JAVA_HOME is set and Java 11+ is installed.');
                process.exit(1);
            }
            resolve();
        });
    });
}

function ensureJarNotCommitted() {
    const { exec } = require('child_process');
    return new Promise((resolve) => {
        exec('git status --porcelain', { cwd: path.dirname(JAR_PATH) }, (error, stdout) => {
            if (error) {
                // Not a git repo or git not available, skip check
                resolve();
                return;
            }
            
            const stagedFiles = stdout.split('\\n').filter(line => line.trim());
            const jarInStaged = stagedFiles.some(line => 
                line.includes('.jar') || line.includes('dist/')
            );
            
            if (jarInStaged) {
                console.error('⚠️  WARNING: JAR files appear to be staged for commit!');
                console.error('   Please unstage them with: git reset HEAD -- dist/ *.jar');
                console.error('   JAR files should never be committed to git.');
            }
            
            resolve();
        });
    });
}

async function launchJar() {
    console.log('🚀 Starting Twitch MCP Server...');
    
    await checkJavaHome();
    
    // Check if JAR exists
    if (!fs.existsSync(JAR_PATH)) {
        console.error('❌ JAR file not found:', JAR_PATH);
        console.error('   Please run: npm run build:jar');
        process.exit(1);
    }
    
    await ensureJarNotCommitted();
    
    // Prepare environment variables
    const env = { ...process.env };
    
    // Launch Java process
    console.log('📦 Launching:', JAR_PATH);
    
    const javaArgs = ['-jar', JAR_PATH];
    
    // Pass through all command line arguments
    const args = process.argv.slice(2);
    javaArgs.push(...args);
    
    const javaProcess = spawn('java', javaArgs, {
        stdio: 'inherit',
        env: env,
        cwd: process.cwd()
    });
    
    javaProcess.on('error', (error) => {
        console.error('❌ Failed to launch Java process:', error.message);
        process.exit(1);
    });
    
    javaProcess.on('exit', (code) => {
        console.log(`🏁 Twitch MCP Server exited with code ${code}`);
        process.exit(code);
    });
    
    // Handle shutdown gracefully
    process.on('SIGINT', () => {
        console.log('\\n🛑 Shutting down...');
        javaProcess.kill('SIGINT');
    });
    
    process.on('SIGTERM', () => {
        console.log('\\n🛑 Shutting down...');
        javaProcess.kill('SIGTERM');
    });
}

if (require.main === module) {
    launchJar().catch((error) => {
        console.error('Launch failed:', error.message);
        process.exit(1);
    });
}