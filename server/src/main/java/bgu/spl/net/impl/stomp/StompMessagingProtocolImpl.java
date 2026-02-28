package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.impl.data.LoginStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {

    // Global counter for generating unique message IDs across the server
    private static final AtomicInteger messageIdCounter = new AtomicInteger(1);

    private int connectionId;
    private Connections<String> connections;
    private boolean shouldTerminate = false;
    
    // Tracks the logged-in user's username for this specific connection
    private String username = null;
    
    // Maps the client's subscription ID to the topic name (destination)
    // This is crucial for handling UNSUBSCRIBE which only provides the ID
    private Map<String, String> subIdToTopic = new HashMap<>();

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(String message) {
        // Parse the raw string message into our convenient StompFrame object
        StompFrame frame = StompFrame.parse(message);
        if (frame == null) {
            return;
        }

        String command = frame.getCommand();

        // Route the frame to the appropriate handler method based on its command
        switch (command) {
            case "CONNECT":
                handleConnect(frame);
                break;
            case "SUBSCRIBE":
                handleSubscribe(frame);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(frame);
                break;
            case "SEND":
                handleSend(frame);
                break;
            case "DISCONNECT":
                handleDisconnect(frame);
                break;
            default:
                sendError("Unknown command", "The server only supports standard STOMP commands", frame);
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private void handleConnect(StompFrame frame) {
        String acceptVersion = frame.getHeader("accept-version");
        String host = frame.getHeader("host");
        String login = frame.getHeader("login");
        String passcode = frame.getHeader("passcode");

        // Validate that all required headers are present
        if (acceptVersion == null || host == null || login == null || passcode == null) {
            sendError("Malformed frame", "CONNECT frame is missing required headers", frame);
            return;
        }

        // Interact with the Database to attempt a login or registration
        LoginStatus status = Database.getInstance().login(connectionId, login, passcode);

        switch (status) {
            case ADDED_NEW_USER:
            case LOGGED_IN_SUCCESSFULLY:
                this.username = login;
                Map<String, String> connectedHeaders = new HashMap<>();
                connectedHeaders.put("version", "1.2");
                StompFrame connectedFrame = new StompFrame("CONNECTED", connectedHeaders, null);
                connections.send(connectionId, connectedFrame.toString());
                break;
            case WRONG_PASSWORD:
                sendError("Wrong password", "The password provided is incorrect", frame);
                break;
            case ALREADY_LOGGED_IN:
            case CLIENT_ALREADY_CONNECTED:
                sendError("User already logged in", "This user is already connected to the server", frame);
                break;
        }
    }

    private void handleSubscribe(StompFrame frame) {
        String destination = frame.getHeader("destination");
        String id = frame.getHeader("id");

        if (destination == null || id == null) {
            sendError("Malformed frame", "SUBSCRIBE requires destination and id headers", frame);
            return;
        }

        // Keep track of the subscription ID for this specific client
        subIdToTopic.put(id, destination);

        // --- OLD CODE ---
        // // We cast to ConnectionsImpl because the base Connections interface lacks a subscribe method
        // // It is recommended to add subscribe and unsubscribe to the Connections interface directly
        // if (connections instanceof ConnectionsImpl) {
        //     ((ConnectionsImpl<String>) connections).subscribe(destination, connectionId);
        // }
        
        // --- NEW CODE ---
        // We cast to ConnectionsImpl to use the updated subscribe method that takes the unique subscription ID.
        if (connections instanceof ConnectionsImpl) {
            ((ConnectionsImpl<String>) connections).subscribe(destination, connectionId, id);
        }

        sendReceiptIfNeeded(frame);
    }

    private void handleUnsubscribe(StompFrame frame) {
        String id = frame.getHeader("id");

        if (id == null) {
            sendError("Malformed frame", "UNSUBSCRIBE requires an id header", frame);
            return;
        }

        // Retrieve the topic name associated with this subscription ID
        String topic = subIdToTopic.remove(id);

        if (topic != null && connections instanceof ConnectionsImpl) {
            ((ConnectionsImpl<String>) connections).unsubscribe(topic, connectionId);
        }

        sendReceiptIfNeeded(frame);
    }

    private void handleSend(StompFrame frame) {
        String destination = frame.getHeader("destination");

        if (destination == null) {
            sendError("Malformed frame", "SEND requires a destination header", frame);
            return;
        }
        
        // --- NEW CODE ---
        // File Tracking Integration: Check if this SEND frame is a game report (contains "user" header)
        String reportUser = frame.getHeader("user");
        if (reportUser != null) {
            // Retrieve filename if the client provided it as a header, otherwise use a default placeholder
            String filename = frame.getHeader("file");
            if (filename == null) {
                filename = "unknown_file";
            }
            Database.getInstance().trackFileUpload(reportUser, filename, destination);
        }
        // ----------------

        // Construct the MESSAGE frame that will be broadcasted to all subscribers
        Map<String, String> messageHeaders = new HashMap<>();
        messageHeaders.put("destination", destination);
        messageHeaders.put("message-id", String.valueOf(messageIdCounter.getAndIncrement()));
        
        // --- OLD CODE ---
        // // Note for SPL3: According to STOMP, each client should receive their unique subscription ID
        // // Since ConnectionsImpl broadcasts the exact same string to everyone, we place the topic name here
        // // A complete solution would modify ConnectionsImpl to format the message per-client
        // messageHeaders.put("subscription", destination); 
        
        // --- NEW CODE ---
        // We place the channel name (destination) as a placeholder in the subscription header.
        // ConnectionsImpl will dynamically replace "subscription:<destination>" with the client's 
        // unique subscription ID before sending it over the socket.
        messageHeaders.put("subscription", destination); 

        StompFrame messageFrame = new StompFrame("MESSAGE", messageHeaders, frame.getBody());
        
        // Broadcast the message to all clients subscribed to this topic
        connections.send(destination, messageFrame.toString());

        sendReceiptIfNeeded(frame);
    }

    private void handleDisconnect(StompFrame frame) {
        // Logout the user from the database
        Database.getInstance().logout(connectionId);
        
        // Disconnect from the connections manager (this removes the client from all topics as well)
        if (connections instanceof ConnectionsImpl) {
            ((ConnectionsImpl<String>) connections).disconnect(connectionId);
        }
        
        sendReceiptIfNeeded(frame);
        
        // Mark the protocol to terminate, which will safely close the socket
        shouldTerminate = true;
    }

    private void sendReceiptIfNeeded(StompFrame frame) {
        String receiptId = frame.getHeader("receipt");
        if (receiptId != null) {
            Map<String, String> receiptHeaders = new HashMap<>();
            receiptHeaders.put("receipt-id", receiptId);
            StompFrame receiptFrame = new StompFrame("RECEIPT", receiptHeaders, null);
            connections.send(connectionId, receiptFrame.toString());
        }
    }

    private void sendError(String messageHeader, String description, StompFrame causeFrame) {
        Map<String, String> errorHeaders = new HashMap<>();
        errorHeaders.put("message", messageHeader);
        
        // If the problematic frame had a receipt requested, the ERROR frame must include it
        String receiptId = causeFrame.getHeader("receipt");
        if (receiptId != null) {
            errorHeaders.put("receipt-id", receiptId);
        }

        StompFrame errorFrame = new StompFrame("ERROR", errorHeaders, description + "\n\n" + causeFrame.toString());
        connections.send(connectionId, errorFrame.toString());
        
        // STOMP requires closing the connection immediately after sending an ERROR frame
        shouldTerminate = true;
    }
}