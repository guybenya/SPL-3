package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Map;

public class StompFrame {

    private String command;
    private Map<String, String> headers;
    private String body;

    // Constructor for creating a StompFrame from scratch (e.g., when the server wants to send a message)
    public StompFrame(String command, Map<String, String> headers, String body) {
        this.command = command;
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body != null ? body : "";
    }

    // Static factory method to parse a raw string from the Decoder into a StompFrame object
    public static StompFrame parse(String rawMessage) {
        // Split the raw message into lines. We limit the split so we don't accidentally split the body
        String[] lines = rawMessage.split("\n");
        
        if (lines.length == 0) return null;

        // 1. The first line is always the STOMP command (e.g., CONNECT, SEND)
        String command = lines[0].trim();
        Map<String, String> headers = new HashMap<>();
        StringBuilder bodyBuilder = new StringBuilder();

        int i = 1;
        // 2. Read headers until we hit an empty line
        while (i < lines.length && !lines[i].trim().isEmpty()) {
            String line = lines[i];
            int colonIndex = line.indexOf(':');
            if (colonIndex != -1) {
                // Extract key and value, ignoring spaces around them if any
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
            i++;
        }

        // Skip the empty line that separates headers from the body
        i++;

        // 3. Everything else is the body of the frame
        while (i < lines.length) {
            bodyBuilder.append(lines[i]);
            if (i < lines.length - 1) {
                bodyBuilder.append("\n");
            }
            i++;
        }

        // Return a new parsed StompFrame object
        return new StompFrame(command, headers, bodyBuilder.toString());
    }

    // Converts the StompFrame back into a valid STOMP string format (without the \u0000 which the Encoder adds)
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // 1. Add the command followed by a newline
        sb.append(command).append("\n");
        
        // 2. Add all headers in the format "key:value"
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey()).append(":").append(header.getValue()).append("\n");
        }
        
        // 3. Add a blank line to separate headers from the body
        sb.append("\n");
        
        // 4. Add the body
        if (body != null && !body.isEmpty()) {
            sb.append(body);
        }
        
        return sb.toString();
    }

    // Getters
    public String getCommand() { return command; }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String key) { return headers.get(key); }
    public String getBody() { return body; }
}