package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
// ORIGINAL: import bgu.spl.net.api.MessagingProtocol;
// CHANGED TO: Use StompMessagingProtocol instead of the general MessagingProtocol
import bgu.spl.net.api.StompMessagingProtocol;
import java.io.Closeable;
import java.util.function.Supplier;

public interface Server<T> extends Closeable {

    void serve();

    // ORIGINAL: 
    // public static <T> Server<T>  threadPerClient(
    //         int port,
    //         Supplier<MessagingProtocol<T> > protocolFactory,
    //         Supplier<MessageEncoderDecoder<T> > encoderDecoderFactory) {
    // CHANGED TO: Updated protocolFactory signature to expect StompMessagingProtocol
    public static <T> Server<T>  threadPerClient(
            int port,
            Supplier<StompMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T> > encoderDecoderFactory) {

        return new BaseServer<T>(port, protocolFactory, encoderDecoderFactory) {
            @Override
            protected void execute(BlockingConnectionHandler<T>  handler) {
                new Thread(handler).start();
            }
        };

    }

    // ORIGINAL:
    // public static <T> Server<T> reactor(
    //         int nthreads,
    //         int port,
    //         Supplier<MessagingProtocol<T>> protocolFactory,
    //         Supplier<MessageEncoderDecoder<T>> encoderDecoderFactory) {
    // CHANGED TO: Updated protocolFactory signature to expect StompMessagingProtocol
    public static <T> Server<T> reactor(
            int nthreads,
            int port,
            Supplier<StompMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encoderDecoderFactory) {
        return new Reactor<T>(nthreads, port, protocolFactory, encoderDecoderFactory);
    }

}