package be.tomcools.twitchmcp.client;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

@ApplicationScoped
public class TwitchClient {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelRoute camelRoute;

    @ConfigProperty(name = "twitch.auth")
    String authToken;

    @ConfigProperty(name = "twitch.client_id")
    String clientId;

    @ConfigProperty(name = "twitch.broadcaster_id")
    String broadcasterId;

    // List of descriptors and associated keywords
    private static final Map<String, List<String>> DESCRIPTOR_KEYWORDS = Map.of(
        "toxic", List.of("idiot", "stupid", "hate", "kill", "dumb", "trash", "noob", "loser", "shut up", "annoying", "toxic", "rude", "mean", "sucks", "bad", "worst", "report", "ban"),
        "spam", List.of("buy followers", "free", "promo", "visit", "http", "www", "spam", "emote", "caps", "repeated"),
        "rude", List.of("shut up", "idiot", "stupid", "dumb", "annoying", "rude", "mean", "trash", "loser", "bad", "worst")
    );

    public void sendMessage(String message) {
        producerTemplate.sendBody("direct:sendToIrc", message);
    }

    // Post something to the Twitch chat after connect.
    void onStart(@Observes StartupEvent ev) {
        this.sendMessage("Twitch MCP Server connected");
    }

    public String createPoll(String title, List<String> choices, int duration) throws Exception {
        URL url = new URL("https://api.twitch.tv/helix/polls");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + authToken.replace("oauth:", ""));
        conn.setRequestProperty("Client-Id", clientId);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        StringBuilder choicesJson = new StringBuilder();
        for (String choice : choices) {
            if (choicesJson.length() > 0) choicesJson.append(",");
            choicesJson.append("{\"title\":\"").append(choice).append("\"}");
        }

        String json = String.format(
            "{\"broadcaster_id\":\"%s\",\"title\":\"%s\",\"choices\":[%s],\"duration\":%d}",
            broadcasterId, title, choicesJson.toString(), duration
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            return "Poll created successfully!";
        } else {
            return "Failed to create poll: HTTP " + responseCode;
        }
    }

    public String createPrediction(String title, List<String> outcomes, int duration) throws Exception {
        URL url = new URL("https://api.twitch.tv/helix/predictions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + authToken.replace("oauth:", ""));
        conn.setRequestProperty("Client-Id", clientId);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        StringBuilder outcomesJson = new StringBuilder();
        for (String outcome : outcomes) {
            if (outcomesJson.length() > 0) outcomesJson.append(",");
            outcomesJson.append("{\"title\":\"").append(outcome).append("\"}");
        }

        String json = String.format(
            "{\"broadcaster_id\":\"%s\",\"title\":\"%s\",\"outcomes\":[%s],\"prediction_window\":%d}",
            broadcasterId, title, outcomesJson.toString(), duration
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            return "Prediction created successfully!";
        } else {
            return "Failed to create prediction: HTTP " + responseCode;
        }
    }

    public String analyzeChat() {
        List<String> messages = camelRoute.getRecentMessages();
        if (messages.isEmpty()) {
            return "No recent chat messages to analyze.";
        }

        // Count message frequency
        Map<String, Integer> wordFrequency = new HashMap<>();
        int totalMessages = messages.size();
        int totalWords = 0;

        for (String message : messages) {
            // Remove username prefix if present
            int colonIndex = message.indexOf(':');
            if (colonIndex > 0) {
                message = message.substring(colonIndex + 1).trim();
            }
            String[] words = message.toLowerCase().split("\\s+");
            totalWords += words.length;
            for (String word : words) {
                // Skip common words and short words
                if (word.length() > 3 && !isCommonWord(word)) {
                    wordFrequency.merge(word, 1, Integer::sum);
                }
            }
        }

        // Get top 5 most frequent words
        List<Map.Entry<String, Integer>> topWords = wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Build the analysis
        StringBuilder analysis = new StringBuilder();
        analysis.append("Chat Analysis:\n");
        analysis.append("- Total messages: ").append(totalMessages).append("\n");
        analysis.append("- Average words per message: ").append(String.format("%.1f", (double) totalWords / totalMessages)).append("\n");
        analysis.append("- Top topics: ");
        
        if (!topWords.isEmpty()) {
            analysis.append(topWords.stream()
                    .map(e -> e.getKey() + " (" + e.getValue() + " mentions)")
                    .collect(Collectors.joining(", ")));
        } else {
            analysis.append("No significant topics detected");
        }

        return analysis.toString();
    }

    public String findUserInChat(String partialName) {
        List<String> messages = camelRoute.getRecentMessages();
        Map<String, Integer> userActivity = new HashMap<>();
        
        // Extract usernames from messages and count their activity
        for (String message : messages) {
            if (message.startsWith(":")) {
                String[] parts = message.split("!");
                if (parts.length > 0) {
                    String username = parts[0].substring(1).toLowerCase();
                    userActivity.merge(username, 1, Integer::sum);
                }
            }
        }
        
        // Find the most active user that matches the partial name
        String bestMatch = null;
        int maxActivity = 0;
        partialName = partialName.toLowerCase();
        
        for (Map.Entry<String, Integer> entry : userActivity.entrySet()) {
            String username = entry.getKey();
            if (username.contains(partialName) && entry.getValue() > maxActivity) {
                bestMatch = username;
                maxActivity = entry.getValue();
            }
        }
        
        return bestMatch;
    }

    public int guessTimeoutDuration(String reason) {
        reason = reason.toLowerCase();
        if (reason.contains("spam") || reason.contains("caps") || reason.contains("emote")) {
            return 300; // 5 minutes for spam/caps/emote spam
        } else if (reason.contains("toxic") || reason.contains("rude") || reason.contains("mean")) {
            return 1800; // 30 minutes for toxic behavior
        } else if (reason.contains("severe") || reason.contains("serious")) {
            return 3600; // 1 hour for severe violations
        }
        return 600; // Default 10 minutes
    }

    private String getUserIdFromUsername(String username) throws Exception {
        URL url = new URL("https://api.twitch.tv/helix/users?login=" + username);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + authToken.replace("oauth:", ""));
        conn.setRequestProperty("Client-Id", clientId);
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            String responseStr = response.toString();
            int idIndex = responseStr.indexOf("\"id\":\"");
            if (idIndex != -1) {
                int startIndex = idIndex + 6;
                int endIndex = responseStr.indexOf("\"", startIndex);
                return responseStr.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    public String timeoutUser(String username, String reason, int duration) throws Exception {
        if (username == null || username.isEmpty()) {
            return "No username provided for timeout.";
        }
        String userId = getUserIdFromUsername(username);
        if (userId == null) {
            return "Could not resolve user ID for username: " + username;
        }
        URL url = new URL("https://api.twitch.tv/helix/moderation/bans");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + authToken.replace("oauth:", ""));
        conn.setRequestProperty("Client-Id", clientId);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String json = String.format(
            "{\"broadcaster_id\":\"%s\",\"moderator_id\":\"%s\",\"data\":{\"user_id\":\"%s\",\"reason\":\"%s\",\"duration\":%d}}",
            broadcasterId, broadcasterId, userId, reason, duration
        );
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            return String.format("Successfully timed out %s for %d seconds. Reason: %s", username, duration, reason);
        } else {
            String errorMsg = "Failed to timeout user: HTTP " + responseCode;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getErrorStream()))) {
                String line;
                StringBuilder errorResponse = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorMsg += "\n" + errorResponse.toString();
            } catch (Exception ignored) {}
            return errorMsg;
        }
    }

    public String banUser(String username, String reason) throws Exception {
        if (username == null || username.isEmpty()) {
            return "No username provided for ban.";
        }
        String userId = getUserIdFromUsername(username);
        if (userId == null) {
            return "Could not resolve user ID for username: " + username;
        }
        URL url = new URL("https://api.twitch.tv/helix/moderation/bans");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + authToken.replace("oauth:", ""));
        conn.setRequestProperty("Client-Id", clientId);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        String json = String.format(
            "{\"broadcaster_id\":\"%s\",\"moderator_id\":\"%s\",\"data\":{\"user_id\":\"%s\",\"reason\":\"%s\"}}",
            broadcasterId, broadcasterId, userId, reason
        );
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            return String.format("Successfully banned %s. Reason: %s", username, reason);
        } else {
            String errorMsg = "Failed to ban user: HTTP " + responseCode;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getErrorStream()))) {
                String line;
                StringBuilder errorResponse = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorMsg += "\n" + errorResponse.toString();
            } catch (Exception ignored) {}
            return errorMsg;
        }
    }

    private boolean isCommonWord(String word) {
        // List of common words to filter out
        Set<String> commonWords = Set.of(
            "the", "and", "that", "have", "for", "not", "with", "you", "this", "but",
            "his", "from", "they", "say", "her", "she", "will", "one", "all", "would",
            "there", "their", "what", "so", "up", "out", "if", "about", "who", "get",
            "which", "go", "me", "when", "make", "can", "like", "time", "no", "just",
            "him", "know", "take", "people", "into", "year", "your", "good", "some",
            "could", "them", "see", "other", "than", "then", "now", "look", "only",
            "come", "its", "over", "think", "also", "back", "after", "use", "two",
            "how", "our", "work", "first", "well", "way", "even", "new", "want",
            "because", "any", "these", "give", "day", "most", "us"
        );
        return commonWords.contains(word.toLowerCase());
    }

    public String createClip() throws Exception {
        URL url = new URL("https://api.twitch.tv/helix/clips?broadcaster_id=" + broadcasterId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + authToken.replace("oauth:", ""));
        conn.setRequestProperty("Client-Id", clientId);
        conn.setRequestProperty("Content-Type", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            // Read the response to get the clip URL
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // Parse the response to get the clip URL
            String responseStr = response.toString();
            int editUrlIndex = responseStr.indexOf("\"edit_url\":\"");
            if (editUrlIndex != -1) {
                int startIndex = editUrlIndex + 12;
                int endIndex = responseStr.indexOf("\"", startIndex);
                String clipUrl = responseStr.substring(startIndex, endIndex);
                return "Clip created successfully! You can view it at: " + clipUrl;
            }
            return "Clip created successfully!";
        } else {
            return "Failed to create clip: HTTP " + responseCode;
        }
    }

    // Analyze chat and return the user with the most messages matching the descriptor
    public String findUserByDescriptor(String descriptor) {
        List<String> messages = camelRoute.getRecentMessages();
        Map<String, Integer> userScores = new HashMap<>();
        List<String> keywords = DESCRIPTOR_KEYWORDS.getOrDefault(descriptor, List.of(descriptor));
        for (String message : messages) {
            if (message.startsWith(":")) {
                String[] parts = message.split("!");
                if (parts.length > 0) {
                    String username = parts[0].substring(1).toLowerCase();
                    String content = message.toLowerCase();
                    for (String keyword : keywords) {
                        if (content.contains(keyword)) {
                            userScores.merge(username, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        // Return the user with the highest score
        return userScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    // Return the last N chat messages (default 20) as a list of "username: message" strings
    public List<String> getRecentChatLog(int n) {
        List<String> messages = camelRoute.getRecentMessages();
        int start = Math.max(0, messages.size() - n);
        List<String> result = new ArrayList<>();
        for (int i = start; i < messages.size(); i++) {
            String message = messages.get(i);
            int colonIndex = message.indexOf(':');
            if (colonIndex > 0) {
                String username = message.substring(0, colonIndex).trim();
                String content = message.substring(colonIndex + 1).trim();
                result.add(username + ": " + content);
            } else {
                result.add(message); // fallback, just in case
            }
        }
        return result;
    }

    // Updated: Only resolve explicit usernames, otherwise return null
    public String resolveModerationTarget(String input) {
        if (input == null || input.isEmpty()) return null;
        String lowered = input.toLowerCase();
        // If the input contains 'user named' or looks like a username, use fuzzy match
        if (lowered.contains("user named") || lowered.matches("[a-zA-Z0-9_]{3,25}")) {
            String username = input.replaceAll(".*user named ", "").trim();
            String match = findUserInChat(username);
            return match != null ? match : username;
        }
        // Otherwise, return null so the tool can provide the chat log to the LLM
        return null;
    }

    public String updateStreamTitle(String newTitle) throws Exception {
        if (newTitle == null || newTitle.isEmpty()) {
            return "No title provided.";
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch("https://api.twitch.tv/helix/channels");
            String bearerToken = authToken.replace("oauth:", "");
            httpPatch.setHeader("Authorization", "Bearer " + bearerToken);
            httpPatch.setHeader("Client-Id", clientId);
            httpPatch.setHeader("Content-Type", "application/json");

            // Escape quotes in the title to prevent JSON formatting issues
            String escapedTitle = newTitle.replace("\"", "\\\"");
            String json = String.format("{\"broadcaster_id\":\"%s\",\"title\":\"%s\"}", broadcasterId, escapedTitle);
            
            httpPatch.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            try (var response = httpClient.execute(httpPatch)) {
                int statusCode = response.getCode();
                String responseBody = "";
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.getEntity().getContent()))) {
                    String line;
                    StringBuilder responseContent = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line);
                    }
                    responseBody = responseContent.toString();
                } catch (Exception e) {
                    responseBody = "Error reading response: " + e.getMessage();
                }

                if (statusCode == 204) {
                    return "Successfully updated stream title to: " + newTitle;
                } else {
                    return String.format("Failed to update stream title: HTTP %d\nResponse: %s", statusCode, responseBody);
                }
            }
        }
    }

    public String updateStreamCategory(String categoryName) throws Exception {
        if (categoryName == null || categoryName.isEmpty()) {
            return "No category provided.";
        }

        // Step 1: Resolve the category/game ID using the search endpoint
        String encodedQuery = java.net.URLEncoder.encode(categoryName, java.nio.charset.StandardCharsets.UTF_8);
        URL searchUrl = new URL("https://api.twitch.tv/helix/search/categories?query=" + encodedQuery);
        HttpURLConnection searchConn = (HttpURLConnection) searchUrl.openConnection();
        searchConn.setRequestMethod("GET");
        searchConn.setRequestProperty("Authorization", "Bearer " + authToken.replace("oauth:", ""));
        searchConn.setRequestProperty("Client-Id", clientId);

        int searchResponseCode = searchConn.getResponseCode();
        if (searchResponseCode != 200) {
            return "Failed to search for category '" + categoryName + "': HTTP " + searchResponseCode;
        }
        StringBuilder searchResponse = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(searchConn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                searchResponse.append(line);
            }
        }

        String responseStr = searchResponse.toString();
        int idIndex = responseStr.indexOf("\"id\":\"");
        if (idIndex == -1) {
            return "Could not find a Twitch category named '" + categoryName + "'.";
        }
        int startIndex = idIndex + 6; // length of "id":"
        int endIndex = responseStr.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return "Unexpected response while parsing category ID.";
        }
        String categoryId = responseStr.substring(startIndex, endIndex);

        // Step 2: Patch the channel with the new game_id
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch("https://api.twitch.tv/helix/channels");
            String bearerToken = authToken.replace("oauth:", "");
            httpPatch.setHeader("Authorization", "Bearer " + bearerToken);
            httpPatch.setHeader("Client-Id", clientId);
            httpPatch.setHeader("Content-Type", "application/json");

            String json = String.format("{\"broadcaster_id\":\"%s\",\"game_id\":\"%s\"}", broadcasterId, categoryId);
            httpPatch.setEntity(new org.apache.hc.core5.http.io.entity.StringEntity(json, org.apache.hc.core5.http.ContentType.APPLICATION_JSON));

            try (var response = httpClient.execute(httpPatch)) {
                int statusCode = response.getCode();
                String responseBody = "";
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.getEntity().getContent()))) {
                    String line;
                    StringBuilder responseContent = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line);
                    }
                    responseBody = responseContent.toString();
                } catch (Exception e) {
                    responseBody = "Error reading response: " + e.getMessage();
                }

                if (statusCode == 204) {
                    return "Successfully updated stream category to: " + categoryName;
                } else {
                    return String.format("Failed to update stream category: HTTP %d\nResponse: %s", statusCode, responseBody);
                }
            }
        }
    }
}
