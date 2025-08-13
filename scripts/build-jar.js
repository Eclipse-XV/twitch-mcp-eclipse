#!/usr/bin/env node

const { exec } = require('child_process');
const fs = require('fs');
const path = require('path');

const JAR_PATH = path.join(__dirname, '..', 'dist', 'twitch-mcp-server.jar');
const DIST_DIR = path.dirname(JAR_PATH);

function checkJavaHome() {
    return new Promise((resolve) => {
        exec('java -version', (error) => {
            if (error) {
                console.error('❌ Java not found. Please ensure JAVA_HOME is set and Java 11+ is installed.');
                process.exit(1);
            }
            resolve();
        });
    });
}

function checkMaven() {
    return new Promise((resolve) => {
        exec('mvn --version', (error) => {
            if (error) {
                console.error('❌ Maven not found. Please install Maven 3.6.2+ or use ./mvnw');
                process.exit(1);
            }
            resolve();
        });
    });
}

async function buildJar() {
    console.log('🔍 Checking for existing JAR...');
    
    // Create dist directory if it doesn't exist
    if (!fs.existsSync(DIST_DIR)) {
        fs.mkdirSync(DIST_DIR, { recursive: true });
    }

    // Check if JAR already exists and is recent
    if (fs.existsSync(JAR_PATH)) {
        const jarStat = fs.statSync(JAR_PATH);
        const pomStat = fs.statSync(path.join(__dirname, '..', 'pom.xml'));
        
        if (jarStat.mtime > pomStat.mtime) {
            console.log('✅ JAR is up to date, skipping build');
            return;
        }
    }

    console.log('🔨 Building JAR...');
    
    await checkJavaHome();
    await checkMaven();

    return new Promise((resolve, reject) => {
        // Use system mvn since wrapper may not be properly configured
        const mvnCmd = 'mvn';
        
        const command = `${mvnCmd} clean package -DskipTests`;
        
        console.log(`Running: ${command}`);
        
        exec(command, { cwd: path.join(__dirname, '..') }, (error, stdout, stderr) => {
            if (error) {
                console.error('❌ Maven build failed:');
                console.error(stderr);
                reject(error);
                return;
            }

            // Find the built JAR in target directory
            const targetDir = path.join(__dirname, '..', 'target');
            if (!fs.existsSync(targetDir)) {
                reject(new Error('Target directory not found after build'));
                return;
            }

            const jarFiles = fs.readdirSync(targetDir)
                .filter(file => file.endsWith('.jar') && !file.includes('sources'));
            
            if (jarFiles.length === 0) {
                reject(new Error('No JAR files found in target directory'));
                return;
            }

            // Copy the main JAR to dist directory
            const sourceJar = path.join(targetDir, jarFiles[0]);
            fs.copyFileSync(sourceJar, JAR_PATH);
            
            console.log('✅ JAR built successfully:', JAR_PATH);
            console.log('⚠️  Remember: Do NOT commit the JAR file to git!');
            
            resolve();
        });
    });
}

if (require.main === module) {
    buildJar().catch((error) => {
        console.error('Build failed:', error.message);
        process.exit(1);
    });
}

module.exports = { buildJar, JAR_PATH };