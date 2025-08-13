package be.tomcools.twitchmcp;

import be.tomcools.twitchmcp.client.TwitchClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * HTTP endpoint for Smithery integration.
 * Bridges HTTP requests to MCP tools using "Streamable HTTP" protocol.
 */
@Path("/mcp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SmitheryMcpResource {

    @Inject
    TwitchClient client;

    @Context
    UriInfo uriInfo;

    /**
     * Handle GET requests - typically for tool discovery and read operations
     * Implements "lazy loading" - tools are discoverable without authentication
     */
    @GET
    public Response handleGet() {
        try {
            // Always return available tools for discovery, regardless of authentication status
            // This implements Smithery's "lazy loading" best practice
            return Response.ok(getAvailableTools()).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to process request: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Handle POST requests - typically for tool execution
     * Validates authentication only when tools are invoked (lazy loading)
     */
    @POST
    public Response handlePost(Map<String, Object> request) {
        try {
            // Extract tool name and parameters from request
            String tool = (String) request.get("tool");
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            
            if (tool == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Tool name is required"))
                        .build();
            }
            
            // Parse configuration from query parameters ONLY when executing tools
            Map<String, String> config = parseConfiguration();
            
            // Validate Twitch authentication only when tools are actually invoked
            String validationError = validateTwitchConfiguration(config);
            if (validationError != null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", validationError))
                        .build();
            }
            
            // Update Twitch client configuration
            updateTwitchConfiguration(config);
            
            // Execute the requested tool
            Object result = executeTool(tool, params != null ? params : Map.of());
            
            return Response.ok(Map.of("result", result)).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute tool: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Handle DELETE requests - typically for cleanup operations
     */
    @DELETE
    public Response handleDelete() {
        try {
            // Perform any cleanup operations if needed
            return Response.ok(Map.of("message", "Cleanup completed")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to cleanup: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Parse dot-notation configuration from query parameters
     */
    private Map<String, String> parseConfiguration() {
        Map<String, String> config = new HashMap<>();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().get(0); // Take first value
            config.put(key, value);
        }
        
        return config;
    }

    /**
     * Validate Twitch configuration for tool execution
     * Returns null if valid, error message if invalid
     */
    private String validateTwitchConfiguration(Map<String, String> config) {
        String channel = config.get("twitch.channel");
        String auth = config.get("twitch.auth");
        String clientId = config.get("twitch.clientId");
        String broadcasterId = config.get("twitch.broadcasterId");
        
        if (channel == null || channel.trim().isEmpty()) {
            return "Missing required parameter: twitch.channel";
        }
        if (auth == null || auth.trim().isEmpty()) {
            return "Missing required parameter: twitch.auth (OAuth token)";
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            return "Missing required parameter: twitch.clientId";
        }
        if (broadcasterId == null || broadcasterId.trim().isEmpty()) {
            return "Missing required parameter: twitch.broadcasterId";
        }
        
        return null; // Valid configuration
    }

    /**
     * Update TwitchClient configuration from parsed parameters
     */
    private void updateTwitchConfiguration(Map<String, String> config) {
        // Update Twitch configuration if provided in query parameters
        String channel = config.get("twitch.channel");
        String auth = config.get("twitch.auth");
        String clientId = config.get("twitch.clientId");
        String broadcasterId = config.get("twitch.broadcasterId");
        
        if (channel != null) {
            System.setProperty("twitch.channel", channel);
        }
        if (auth != null) {
            System.setProperty("twitch.auth", auth);
        }
        if (clientId != null) {
            System.setProperty("twitch.client_id", clientId);
        }
        if (broadcasterId != null) {
            System.setProperty("twitch.broadcaster_id", broadcasterId);
        }
    }

    /**
     * Return list of available tools for discovery (lazy loading implementation)
     * Tools are discoverable without authentication - validation happens on execution
     */
    private Map<String, Object> getAvailableTools() {
        return Map.of(
            "server", "Twitch MCP Server",
            "version", "1.0.0",
            "description", "AI integration for Twitch chat moderation, stream management, and viewer engagement",
            "authentication", Map.of(
                "required", true,
                "type", "oauth",
                "description", "Twitch OAuth token and client credentials required for tool execution",
                "lazy_loading", true
            ),
            "tools", Arrays.asList(
                Map.of(
                    "name", "sendMessageToChat", 
                    "description", "Send message to the Twitch Chat",
                    "parameters", Map.of("message", "string (required)")
                ),
                Map.of(
                    "name", "createTwitchPoll", 
                    "description", "Create a Twitch Poll",
                    "parameters", Map.of(
                        "title", "string (required)",
                        "choices", "string (required - comma-separated)",
                        "duration", "integer (required - seconds)"
                    )
                ),
                Map.of(
                    "name", "createTwitchPrediction", 
                    "description", "Create a Twitch Prediction",
                    "parameters", Map.of(
                        "title", "string (required)",
                        "outcomes", "string (required - comma-separated)",
                        "duration", "integer (required - seconds)"
                    )
                ),
                Map.of(
                    "name", "createTwitchClip", 
                    "description", "Create a Twitch clip of the current stream",
                    "parameters", Map.of()
                ),
                Map.of(
                    "name", "analyzeChat", 
                    "description", "Analyze recent Twitch chat messages and provide a summary",
                    "parameters", Map.of()
                ),
                Map.of(
                    "name", "getRecentChatLog", 
                    "description", "Get the last 20 chat messages for moderation context",
                    "parameters", Map.of()
                ),
                Map.of(
                    "name", "timeoutUser", 
                    "description", "Timeout a user in the Twitch chat",
                    "parameters", Map.of(
                        "usernameOrDescriptor", "string (required)",
                        "reason", "string (optional)"
                    )
                ),
                Map.of(
                    "name", "banUser", 
                    "description", "Ban a user from the Twitch chat",
                    "parameters", Map.of(
                        "usernameOrDescriptor", "string (required)",
                        "reason", "string (optional)"
                    )
                ),
                Map.of(
                    "name", "updateStreamTitle", 
                    "description", "Update the stream title",
                    "parameters", Map.of("title", "string (required)")
                ),
                Map.of(
                    "name", "updateStreamCategory", 
                    "description", "Update the game category of the stream",
                    "parameters", Map.of("category", "string (required)")
                )
            )
        );
    }

    /**
     * Execute the specified tool with given parameters
     */
    private Object executeTool(String toolName, Map<String, Object> params) {
        switch (toolName) {
            case "sendMessageToChat":
                String message = (String) params.get("message");
                if (message == null) throw new IllegalArgumentException("message parameter is required");
                client.sendMessage(message);
                return "Successfully sent message: " + message;

            case "createTwitchPoll":
                String pollTitle = (String) params.get("title");
                String choices = (String) params.get("choices");
                Integer pollDuration = (Integer) params.get("duration");
                if (pollTitle == null || choices == null || pollDuration == null) {
                    throw new IllegalArgumentException("title, choices, and duration parameters are required");
                }
                try {
                    return client.createPoll(pollTitle, Arrays.asList(choices.split(",")), pollDuration);
                } catch (Exception e) {
                    return "Error creating poll: " + e.getMessage();
                }

            case "createTwitchPrediction":
                String predTitle = (String) params.get("title");
                String outcomes = (String) params.get("outcomes");
                Integer predDuration = (Integer) params.get("duration");
                if (predTitle == null || outcomes == null || predDuration == null) {
                    throw new IllegalArgumentException("title, outcomes, and duration parameters are required");
                }
                try {
                    return client.createPrediction(predTitle, Arrays.asList(outcomes.split(",")), predDuration);
                } catch (Exception e) {
                    return "Error creating prediction: " + e.getMessage();
                }

            case "createTwitchClip":
                try {
                    return client.createClip();
                } catch (Exception e) {
                    return "Error creating clip: " + e.getMessage();
                }

            case "analyzeChat":
                return client.analyzeChat();

            case "getRecentChatLog":
                var log = client.getRecentChatLog(20);
                return log.isEmpty() ? "No recent chat messages available." : String.join("\n", log);

            case "timeoutUser":
                String timeoutTarget = (String) params.get("usernameOrDescriptor");
                String timeoutReason = (String) params.get("reason");
                if (timeoutTarget == null) {
                    var chatLog = client.getRecentChatLog(20);
                    return "No explicit username provided. Here are the last 20 chat messages:\n" + String.join("\n", chatLog);
                }
                String resolvedUser = client.resolveModerationTarget(timeoutTarget);
                if (resolvedUser == null) {
                    var chatLog = client.getRecentChatLog(20);
                    return "Could not resolve user. Here are the last 20 chat messages:\n" + String.join("\n", chatLog);
                }
                int duration = client.guessTimeoutDuration(timeoutReason != null ? timeoutReason : "inappropriate behavior");
                try {
                    return client.timeoutUser(resolvedUser, timeoutReason != null ? timeoutReason : "inappropriate behavior", duration);
                } catch (Exception e) {
                    return "Error timing out user: " + e.getMessage();
                }

            case "banUser":
                String banTarget = (String) params.get("usernameOrDescriptor");
                String banReason = (String) params.get("reason");
                if (banTarget == null) {
                    var chatLog = client.getRecentChatLog(20);
                    return "No explicit username provided. Here are the last 20 chat messages:\n" + String.join("\n", chatLog);
                }
                String resolvedBanUser = client.resolveModerationTarget(banTarget);
                if (resolvedBanUser == null) {
                    var chatLog = client.getRecentChatLog(20);
                    return "Could not resolve user. Here are the last 20 chat messages:\n" + String.join("\n", chatLog);
                }
                try {
                    return client.banUser(resolvedBanUser, banReason != null ? banReason : "severe violation of chat rules");
                } catch (Exception e) {
                    return "Error banning user: " + e.getMessage();
                }

            case "updateStreamTitle":
                String newTitle = (String) params.get("title");
                if (newTitle == null) throw new IllegalArgumentException("title parameter is required");
                try {
                    return client.updateStreamTitle(newTitle);
                } catch (Exception e) {
                    return "Failed to update stream title: " + e.getMessage();
                }

            case "updateStreamCategory":
                String category = (String) params.get("category");
                if (category == null) throw new IllegalArgumentException("category parameter is required");
                try {
                    return client.updateStreamCategory(category);
                } catch (Exception e) {
                    return "Failed to update stream category: " + e.getMessage();
                }

            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
    }
}