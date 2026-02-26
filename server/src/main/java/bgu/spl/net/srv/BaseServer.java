package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    //private final Supplier<MessagingProtocol<T>> protocolFactory;
    private final Supplier<StompMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    // new field
    ConnectionsImpl<T> connections;

    public BaseServer(
            int port,
            //Supplier<MessagingProtocol<T>> protocolFactory,
            Supplier<StompMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        // addition to constructor
        this.connections = new ConnectionsImpl<T>();
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();

                // create a protocol for the client
                StompMessagingProtocol<T> protocol = protocolFactory.get();

                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        // changed from protocolFactory.get()
                        protocol);
                
                // insert the new client to the connections DS    
                int connectionId = connections.addClient(handler);
                // initialize the proctocol
                protocol.start(connectionId, connections);
                
                // --- due to page 10 in the instructions file: start function must end before excecution ---

                execute(handler);
            }

        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
