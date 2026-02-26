package bgu.spl.net.srv;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T> {
    // fields - thread safe
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> map;
    private ConcurrentHashMap<String, Set<Integer>> topics;
    private AtomicInteger size;
    // constructor
    public ConnectionsImpl() {
        this.map = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
        this.topics = new ConcurrentHashMap<String,Set<Integer>>();
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
        Set<Integer> subscribers = topics.get(channel);
        if (subscribers != null) { 
            for (Integer id : subscribers) {
                send(id, msg); // calls the other "send" method
            }
        }
    }
    // remove a client from the server
    public void disconnect(int connectionId) {
        // remove the client from clients map
        map.remove(connectionId);
        // remove the client from every topic
        for (Set<Integer> currentChannel : topics.values()) {
            currentChannel.remove(connectionId);
        }
    }
    // addition methods
    public int addClient(ConnectionHandler<T> handler) {
        int id = size.incrementAndGet(); // creates a unique ID number for each client
        map.put(id, handler);
        return id;
    }
    public void subscribe(String channel, int connectionId) {
        // if the channel do not exists - add it
        topics.putIfAbsent(channel, ConcurrentHashMap.newKeySet());
        // insert the new client to the channel
        topics.get(channel).add(connectionId);
    }
    public void unsubscribe(String channel, int connectionId) {
        if (topics.containsKey(channel)) {
            topics.get(channel).remove(connectionId);
        }
    }    
}
