package bgu.spl.net.impl.echo;

import bgu.spl.net.srv.Server;

public class EchoServer {

    public static void main(String[] args) {

        // ORIGINAL:
        // // you can use any server... 
        // Server.threadPerClient(
        //         7777, //port
        //         () -> new EchoProtocol(), //protocol factory
        //         LineMessageEncoderDecoder::new //message encoder decoder factory
        // ).serve();
        // CHANGED TO: Commented out the active threadPerClient block to avoid compilation errors with StompMessagingProtocol
        
        System.out.println("EchoServer is disabled. Please run StompServer instead.");

        // Server.reactor(
        //         Runtime.getRuntime().availableProcessors(),
        //         7777, //port
        //         () -> new EchoProtocol<>(), //protocol factory
        //         LineMessageEncoderDecoder::new //message encoder decoder factory
        // ).serve();
    }
}