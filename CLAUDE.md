
# Claude's Guide to the Twitch MCP Server

Hello Claude! This document is your guide to understanding the Twitch MCP Server project. It's designed to help you, or any other AI, quickly grasp the project's architecture, key components, and how to interact with it.

## 1. Project Overview

This project is a **Twitch Model Context Protocol (MCP) Server**. Its primary purpose is to expose a set of tools to AI assistants like yourself, allowing you to interact with a Twitch stream. These tools enable you to perform actions such as:

*   **Chat Moderation:** Timeout or ban users.
*   **Stream Management:** Update the stream title and category.
*   **Viewer Engagement:** Create polls and predictions.
*   **Chat Interaction:** Send and receive chat messages.

The server is built in Java using the Quarkus framework and Apache Camel for Twitch IRC integration. It communicates with AI assistants like you over standard input/output (stdio) using the Model Context Protocol.

## 2. Directory Structure

Here's a breakdown of the most important files and directories:

```
twitch-mcp-dev/
├── src/
│   └── main/
│       ├── java/
│       │   └── be/tomcools/twitchmcp/
│       │       ├── TwitchMcp.java        # Defines the tools available to you (MCP layer).
│       │       └── client/
│       │           ├── CamelRoute.java   # Handles Twitch IRC communication (Integration layer).
│       │           └── TwitchClient.java # Implements the core logic for each tool (Service layer).
│       └── resources/
│           └── application.properties  # Configuration for the Quarkus application.
├── pom.xml                         # Maven project configuration, including dependencies.
├── README.md                       # General information for human users.
├── README-developers.md            # Detailed information for human developers.
└── CLAUDE.md                       # This file! Your guide to the project.
```

## 3. Core Technologies

This project is built on a modern Java stack:

*   **Java:** The core programming language.
*   **Quarkus:** A Kubernetes-native Java stack for building fast, lightweight applications.
*   **Apache Camel:** A powerful integration framework used here to connect to Twitch's IRC-based chat.
*   **Maven:** A build automation and dependency management tool.
*   **MCP (Model Context Protocol):** The protocol used to communicate between this server and AI assistants like you.

## 4. Architecture

The application follows a simple, layered architecture:

1.  **MCP Layer (`TwitchMcp.java`):** This is the entry point for all your requests. It uses the `@Tool` annotation to define the functions that you can call. Each method in this class corresponds to a specific action you can perform on Twitch.

2.  **Service Layer (`TwitchClient.java`):** This layer contains the business logic for each tool. When you call a tool in the `TwitchMcp` class, it delegates the actual work to a corresponding method in this `TwitchClient` class. This is where the interactions with the Twitch API and IRC are handled.

3.  **Integration Layer (`CamelRoute.java`):** This layer is responsible for the low-level communication with Twitch's IRC chat. It uses Apache Camel to establish a persistent connection to the Twitch IRC server, allowing the application to send and receive chat messages in real-time.

## 5. Key Files Explained

### `TwitchMcp.java`

This is the most important file for you to understand. It defines the "tools" you can use. Each method annotated with `@Tool` is a function you can call. For example, to send a chat message, you would call the `sendMessageToChat` method.

### `TwitchClient.java`

This file contains the implementation of the tools defined in `TwitchMcp.java`. For example, the `sendMessageToChat` method in `TwitchMcp.java` calls the `sendMessage` method in this file, which then uses the Apache Camel integration to send the message to the Twitch IRC server.

### `CamelRoute.java`

This file sets up the connection to the Twitch IRC server. It defines a "route" that listens for incoming messages from Twitch chat and forwards them to the `TwitchClient` for processing. It also provides a way for the `TwitchClient` to send messages back to the Twitch chat.

### `pom.xml`

This file defines all the project's dependencies, including Quarkus, Apache Camel, and the MCP server library. It also configures the build process.

## 6. How It Works: A Step-by-Step Example

Let's walk through what happens when you want to send a message to the Twitch chat:

1.  **You:** You decide to call the `sendMessageToChat` tool with a specific message.
2.  **MCP Server:** The MCP server receives your request and calls the `sendMessageToChat` method in the `TwitchMcp.java` file.
3.  **`TwitchMcp.java`:** This method simply calls the `sendMessage` method in the `TwitchClient.java` file, passing along the message.
4.  **`TwitchClient.java`:** The `sendMessage` method takes the message and uses the Apache Camel producer template to send it to the Twitch IRC endpoint defined in `CamelRoute.java`.
5.  **`CamelRoute.java`:** The Camel route sends the message to the Twitch IRC server.
6.  **Twitch:** The message appears in the Twitch chat.

## 7. How to Extend the Functionality

If a human developer wants to add a new tool for you to use, they would follow these steps:

1.  **Define the Tool:** Add a new method to the `TwitchMcp.java` file and annotate it with `@Tool`. This will make the new tool available to you.
2.  **Implement the Logic:** Add a corresponding method to the `TwitchClient.java` file to implement the new tool's functionality. This might involve calling the Twitch API, interacting with the IRC chat, or performing some other action.
3.  **Update Documentation:** The developer should update this file (`CLAUDE.md`) to let you and other AIs know about the new tool.

I hope this guide helps you understand the Twitch MCP Server project. If you have any questions, feel free to ask!
