package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JList;

/**
 * A multi-threaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * Name of each client and its respective writer will be stored so the writer of a specific client can be accessed easily.
     * Check if the client exists before adding him/her to the HashMap.
     * Add the writer when the client is added and remove the write when the client is removed.
     */
    private static HashMap<String, PrintWriter> nameAndItsWriter = new HashMap<String, PrintWriter>();
  
    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private ObjectOutputStream objOut;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (nameAndItsWriter) {
                        if (!nameAndItsWriter.containsKey(name)) {
                            // Now that a successful name has been chosen, 
                            // add that name and socket's PrintWriter to the HashMap,
                            // as a pair so the relevant writer of a specific client,
                            // can be easily accessed.
                            // Iterate through the whole HashMap if a global broadcast is needed.
                            out.println("NAMEACCEPTED");
                            nameAndItsWriter.put(name, out);
                            
                            // Send the activer users' list to every client(basically broadcast).
                            sendActiveUserListToOutputStream();
                            
                            break;
                        }
                    }
                    
                    
                }

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        System.err.println("null");
                        return;
                    }
                    
                    // If the receiving user is specified then only that user should get the message.
                    // Therefore the message should be printed only on the sender and the receiver.
                    // This is indicated by the notation: Receiver's_name>>The Message
                    // Example:  Anushka>>Hello  indicates that the message "Hello" should be sent to Anushka.
                    if (input.contains(">>")) {
                        /*String specificReceiver = input.substring(0, input.indexOf(">>"));    // sequence before ">>"
                        String specificMessage = input.substring(input.indexOf(">>") + 2);    // sequence after ">>"
                        
                        // print to the sender itself.
                        out.println("MESSAGE " + name + ": " + specificMessage);
                        
                        // print to the specific receiver.
                        nameAndItsWriter.get(specificReceiver).println("MESSAGE " + name + ": " + specificMessage);
                        */
                        
                        // All the targeted clients are denoted by its name followed by >>.
                        // Example: Messaging "Hello" to A, B and C clients will look as follows.
                        //          A>>B>>C>>Hello
                        // Therefore if we split the inputstream's line by >>, the last element will be the message
                        // and the rest will be the targeted clients.
                        String [] inputContent = input.split(">>");
                        String message = inputContent[inputContent.length -1 ];
                        
                        for (int i = 0; i < inputContent.length - 2; i++) {
                            // check if the user exists in the hashmap;
                            // because since we split the input by >> even if the actual message contains >> it will be
                            // considered as another client which will result in a null pointer.
                            nameAndItsWriter.get(inputContent[i]).println("MESSAGE " + name + ": " + input);
                            
                        }
                    }
                    else {
                        // broadcast to everyone.
                        for (String nameKey: nameAndItsWriter.keySet()) {
                            nameAndItsWriter.get(nameKey).println("MESSAGE " + name + ": " + input);
                        }
                    }
                    
                    
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  
                // Remove the client name and its PrintWriter pair from the HashMap.
                if (name != null ) {
                    nameAndItsWriter.remove(name, out);
                    
                    // Send the updated activer users' list to every client(basically broadcast).
                    sendActiveUserListToOutputStream();
                }
                try {
                    socket.close();
                } 
                catch (IOException e) {
                }
            }
        }
        
        public static Iterator getIterator(LinkedHashMap lhm) {
            // To iterator over the LinkedHashMap.
            Set entrySet = lhm.entrySet();
            return entrySet.iterator();
        }
        
        public static void sendActiveUserListToOutputStream() {
            // Send the updated activer users' list to every client(basically broadcast).
            for (String nameKey: nameAndItsWriter.keySet()) {
                nameAndItsWriter.get(nameKey).println("ACTIVEUSERS" + String.join(":", new ArrayList<String>(nameAndItsWriter.keySet())));
            }    
        }
    }

}