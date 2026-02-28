package bgu.spl.net.srv;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map; // Added for Map.Entry

public class ConnectionsImpl<T> implements Connections<T> {
    // fields - thread safe
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> map;
    
    // --- OLD CODE ---
    // private ConcurrentHashMap<String, Set<Integer>> topics;
    
    // --- NEW CODE ---
    // Map from channel -> (ConnectionId -> SubscriptionId)
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>> topics;
    
    private AtomicInteger size;
    
    // constructor
    public ConnectionsImpl() {
        this.map = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
        
        // --- OLD CODE ---
        // this.topics = new ConcurrentHashMap<String,Set<Integer>>();
        
        // --- NEW CODE ---
        this.topics = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>>();
        
        // count the number of connected clients
        this.size = new AtomicInteger(0);
    }
    // class methods
    // implemantation of the abstract methods

    // sends the message to the client, return false if the client is not connected to the server
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = map.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false;
    }

    // send a message to every client in a given topic
    public void send(String channel, T msg) {
        // --- OLD CODE ---
        // Set<Integer> subscribers = topics.get(channel);
        // if (subscribers != null) { 
        //     for (Integer id : subscribers) {
        //         send(id, msg); // calls the other "send" method
        //     }
        // }
        
        // --- NEW CODE ---
        ConcurrentHashMap<Integer, String> subscribers = topics.get(channel);
        if (subscribers != null) { 
            for (Map.Entry<Integer, String> entry : subscribers.entrySet()) {
                Integer id = entry.getKey();
                String subId = entry.getValue();
                
                // Assuming T is String in our STOMP implementation, we dynamically 
                // replace the placeholder in the message with the exact subscriptionId
                if (msg instanceof String) {
                    String stringMsg = (String) msg;
                    // מחליף את שם הערוץ ששמנו כפלייסבולדר ב-ID הספציפי של הלקוח
                    stringMsg = stringMsg.replace("subscription:" + channel, "subscription:" + subId);
                    send(id, (T) stringMsg);
                } else {
                    send(id, msg); 
                }
            }
        }
    }
    
    // remove a client from the server
    public void disconnect(int connectionId) {
        // remove the client from clients map
        map.remove(connectionId);
        // remove the client from every topic
        
        // --- OLD CODE ---
        // for (Set<Integer> currentChannel : topics.values()) {
        //     currentChannel.remove(connectionId);
        // }
        
        // --- NEW CODE ---
        for (ConcurrentHashMap<Integer, String> currentChannel : topics.values()) {
            currentChannel.remove(connectionId);
        }
    }
    
    // addition methods
    public int addClient(ConnectionHandler<T> handler) {
        int id = size.incrementAndGet(); // creates a unique ID number for each client
        map.put(id, handler);
        return id;
    }
    
    // --- OLD CODE ---
    // public void subscribe(String channel, int connectionId) {
    //     // if the channel do not exists - add it
    //     topics.putIfAbsent(channel, ConcurrentHashMap.newKeySet());
    //     // insert the new client to the channel
    //     topics.get(channel).add(connectionId);
    // }
    
    // --- NEW CODE ---
    public void subscribe(String channel, int connectionId, String subscriptionId) {
        // if the channel does not exist - add it
        topics.putIfAbsent(channel, new ConcurrentHashMap<Integer, String>());
        // insert the new client and its unique subscription ID to the channel
        topics.get(channel).put(connectionId, subscriptionId);
    }
    
    public void unsubscribe(String channel, int connectionId) {
        if (topics.containsKey(channel)) {
            topics.get(channel).remove(connectionId);
        }
    }    
}