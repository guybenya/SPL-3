package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

// --- NEW CODE ---
import bgu.spl.net.impl.data.Database;
import java.util.Scanner;
// ----------------

public class StompServer {

    public static void main(String[] args) {
        
        // We must receive exactly 2 arguments: port and server type (tpc/reactor)
        if (args.length < 2) {
            System.out.println("Usage: StompServer <port> <server_type(tpc/reactor)>");
            return;
        }

        // Parse the port from the first argument
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port must be a valid integer.");
            return;
        }

        // Parse the server type from the second argument
        String serverType = args[1].toLowerCase();

        // --- NEW CODE ---
        // Start a background thread to listen for console commands (like "report")
        Thread cliThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Server Console: Type 'report' to print the database report.");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("report")) {
                    Database.getInstance().printReport();
                }
            }
            scanner.close();
        });
        cliThread.setDaemon(true); // Ensures this thread doesn't prevent the JVM from exiting
        cliThread.start();
        // ----------------

        // Initialize and start the requested server type
        if (serverType.equals("tpc")) {
            System.out.println("Starting Thread-Per-Client (TPC) Server on port " + port);
            
            // Using the standard SPL Server.threadPerClient factory method
            Server.threadPerClient(
                    port,
                    () -> new StompMessagingProtocolImpl(), // Factory for the Stomp Protocol
                    () -> new StompEncoderDecoder()         // Factory for the Stomp Encoder/Decoder
            ).serve();

        } else if (serverType.equals("reactor")) {
            System.out.println("Starting Reactor Server on port " + port);
            
            // Using the standard SPL Server.reactor factory method
            // We use the number of available cores for the thread pool size
            Server.reactor(
                    Runtime.getRuntime().availableProcessors(), 
                    port,
                    () -> new StompMessagingProtocolImpl(),     // Factory for the Stomp Protocol
                    () -> new StompEncoderDecoder()             // Factory for the Stomp Encoder/Decoder
            ).serve();

        } else {
            System.out.println("Invalid server type. Please use 'tpc' or 'reactor'.");
        }
    }
}