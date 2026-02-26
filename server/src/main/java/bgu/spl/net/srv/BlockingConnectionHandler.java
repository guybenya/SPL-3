package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final StompMessagingProtocol<T> protocol; // changed to stomp
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    // protocol changed to stomp
    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, StompMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

                        // while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                        //     T nextMessage = encdec.decodeNextByte((byte) read);
                        //     if (nextMessage != null) {
                        //         T response = protocol.process(nextMessage);
                        //         if (response != null) {
                        //             out.write(encdec.encode(response));
                        //             out.flush();
                        //         }
                        //     }
                        // }

            // adjusting the process to stomp protocol                       
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                }
            }            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        //IMPLEMENT IF NEEDED
        if (msg != null && connected) {
            try {
                synchronized (this) {
                    if (out != null) {
                        // translate the message to bytes array
                        out.write(encdec.encode(msg));
                        // empty the byte buffer 
                        out.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
