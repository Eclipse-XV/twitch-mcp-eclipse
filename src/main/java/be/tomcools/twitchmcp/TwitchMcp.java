package be.tomcools.twitchmcp;

import be.tomcools.twitchmcp.client.TwitchClient;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;
import java.util.Arrays;

/**
 * Contains the MCP Definitions.
 */
public class TwitchMcp {
    @Inject
    TwitchClient client;

    @Tool(description = "Send message to the Twitch Chat")
    ToolResponse sendMessageToChat(@ToolArg(description = "The message") String message) {
        client.sendMessage(message);
        return ToolResponse.success(new TextContent("Successfully sent message: " + message));
    }

    @Tool(description = "Create a Twitch Poll")
    ToolResponse createTwitchPoll(
        @ToolArg(description = "Poll title") String title,
        @ToolArg(description = "Comma-separated choices") String choices,
        @ToolArg(description = "Duration in seconds") int duration
    ) {
        try {
            String result = client.createPoll(title, Arrays.asList(choices.split(",")), duration);
            return ToolResponse.success(new TextContent(result));
        } catch (Exception e) {
            return ToolResponse.success(new TextContent("Error creating poll: " + e.getMessage()));
        }
    }

    @Tool(description = "Create a Twitch Prediction")
    ToolResponse createTwitchPrediction(
        @ToolArg(description = "Prediction title") String title,
        @ToolArg(description = "Comma-separated outcomes") String outcomes,
        @ToolArg(description = "Duration in seconds") int duration
    ) {
        try {
            String result = client.createPrediction(title, Arrays.asList(outcomes.split(",")), duration);
            return ToolResponse.success(new TextContent(result));
        } catch (Exception e) {
            return ToolResponse.success(new TextContent("Error creating prediction: " + e.getMessage()));
        }
    }

    @Tool(description = "Create a Twitch clip of the current stream")
    ToolResponse createTwitchClip() {
        try {
            String result = client.createClip();
            return ToolResponse.success(new TextContent(result));
        } catch (Exception e) {
            return ToolResponse.success(new TextContent("Error creating clip: " + e.getMessage()));
        }
    }

    @Tool(description = "Analyze recent Twitch chat messages and provide a summary of topics and activity")
    ToolResponse analyzeChat() {
        String analysis = client.analyzeChat();
        return ToolResponse.success(new TextContent(analysis));
    }

    @Tool(description = "Get the last 20 chat messages for moderation context")
    ToolResponse getRecentChatLog() {
        var log = client.getRecentChatLog(20);
        if (log.isEmpty()) {
            return ToolResponse.success(new TextContent("No recent chat messages available."));
        }
        return ToolResponse.success(new TextContent(String.join("\n", log)));
    }

    @Tool(description = "Timeout a user in the Twitch chat. If no username is provided, it will return the recent chat log for LLM review.")
    ToolResponse timeoutUser(
        @ToolArg(description = "Username or descriptor to timeout (e.g. 'toxic', 'spammer', or a username)") String usernameOrDescriptor,
        @ToolArg(description = "Reason for timeout (optional)") String reason
    ) {
        try {
            String targetUser = client.resolveModerationTarget(usernameOrDescriptor);
            if (targetUser == null) {
                var log = client.getRecentChatLog(20);
                return ToolResponse.success(new TextContent(
                    "No explicit username provided. Here are the last 20 chat messages:\n" + String.join("\n", log)
                ));
            }
            int duration = client.guessTimeoutDuration(reason != null ? reason : "inappropriate behavior");
            String defaultReason = "inappropriate behavior";
            String result = client.timeoutUser(targetUser, reason != null ? reason : defaultReason, duration);
            return ToolResponse.success(new TextContent(result));
        } catch (Exception e) {
            return ToolResponse.success(new TextContent("Error timing out user: " + e.getMessage()));
        }
    }

    @Tool(description = "Ban a user from the Twitch chat. If no username is provided, it will return the recent chat log for LLM review.")
    ToolResponse banUser(
        @ToolArg(description = "Username or descriptor to ban (e.g. 'toxic', 'spammer', or a username)") String usernameOrDescriptor,
        @ToolArg(description = "Reason for ban (optional)") String reason
    ) {
        try {
            String targetUser = client.resolveModerationTarget(usernameOrDescriptor);
            if (targetUser == null) {
                var log = client.getRecentChatLog(20);
                return ToolResponse.success(new TextContent(
                    "No explicit username provided. Here are the last 20 chat messages:\n" + String.join("\n", log)
                ));
            }
            String defaultReason = "severe violation of chat rules";
            String result = client.banUser(targetUser, reason != null ? reason : defaultReason);
            return ToolResponse.success(new TextContent(result));
        } catch (Exception e) {
            return ToolResponse.success(new TextContent("Error banning user: " + e.getMessage()));
        }
    }

    @Tool(name = "updateStreamTitle", description = "Update the stream title")
    ToolResponse updateStreamTitle(@ToolArg(description = "The new title for the stream") String title) {
        try {
            String result = client.updateStreamTitle(title);
            return ToolResponse.success(new TextContent(result));
        } catch (Exception e) {
            return ToolResponse.success(new TextContent("Failed to update stream title: " + e.getMessage()));
        }
    }

    @Tool(name = "updateStreamCategory", description = "Update the game category of the stream")
    ToolResponse updateStreamCategory(@ToolArg(description = "The new game category, e.g. 'Fortnite'") String category) {
        try {
            String result = client.updateStreamCategory(category);
            return ToolResponse.success(new TextContent(result));
        } catch (Exception e) {
            return ToolResponse.success(new TextContent("Failed to update stream category: " + e.getMessage()));
        }
    }
}
