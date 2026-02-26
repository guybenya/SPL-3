package bgu.spl.net.srv;

import java.io.IOException;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    void disconnect(int connectionId);

    // addition methods

    void subscribe(String channel, int connectionId);

    void unsubscribe(String channel, int connectionId);

    int addClient(ConnectionHandler<T> handler);
}
