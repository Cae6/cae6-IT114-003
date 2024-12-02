package Module4.Part3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;




public class Server {
    private int port = 3000;
    private Random random =new Random(); //new declaring  random
    private int answer =100000; //new  declaring int
    // connected clients
    // Use ConcurrentHashMap for thread-safe client management
    private final ConcurrentHashMap<Long, ServerThread> connectedClients = new ConcurrentHashMap<>();
    private boolean isRunning = true;

    private void start(int port) {
        this.port = port;
        // server listening
        System.out.println("Listening on port " + this.port);
        // Simplified client connection loop
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (isRunning) {
                System.out.println("Waiting for next client");
                Socket incomingClient = serverSocket.accept(); // blocking action, waits for a client connection
                System.out.println("Client connected");
                // wrap socket in a ServerThread, pass a callback to notify the Server they're initialized
                ServerThread sClient = new ServerThread(incomingClient, this, this::onClientInitialized);
                // start the thread (typically an external entity manages the lifecycle and we
                // don't have the thread start itself)
                sClient.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("Closing server socket");
        }
    }
    /**
     * Callback passed to ServerThread to inform Server they're ready to receive data
     * @param sClient
     */
    private void onClientInitialized(ServerThread sClient) {
        // add to connected clients list
        connectedClients.put(sClient.getClientId(), sClient);
        relay(String.format("*User[%s] connected*", sClient.getClientId()), null);
    }
    /**
     * Takes a ServerThread and removes them from the Server
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param client
     */
    protected synchronized void disconnect(ServerThread client) {
        long id = client.getClientId();
        client.disconnect();
        connectedClients.remove(id);
        // Improved logging with user ID
        relay("User[" + id + "] disconnected", null);
    }

    /**
     * Relays the message from the sender to all connectedClients
     * Internally calls processCommand and evaluates as necessary.
     * Note: Clients that fail to receive a message get removed from
     * connectedClients.
     * Adding the synchronized keyword ensures that only one thread can execute
     * these methods at a time,
     * preventing concurrent modification issues and ensuring thread safety
     * 
     * @param message
     * @param sender ServerThread (client) sending the message or null if it's a server-generated message
     */
    protected synchronized void relay(String message, ServerThread sender) {
        if (sender != null && processCommand(message, sender)) {

            return;
        }
        // let's temporarily use the thread id as the client identifier to
        // show in all client's chat. This isn't good practice since it's subject to
        // change as clients connect/disconnect
        // Note: any desired changes to the message must be done before this line
        String senderString = sender == null ? "Server" : String.format("User[%s]", sender.getClientId());
        final String formattedMessage = String.format("%s: %s", senderString, message);
        // end temp identifier

        // loop over clients and send out the message; remove client if message failed
        // to be sent
        // Note: this uses a lambda expression for each item in the values() collection,
        // it's one way we can safely remove items during iteration
        
        connectedClients.values().removeIf(client -> {
            boolean failedToSend = !client.send(formattedMessage);
            if (failedToSend) {
                System.out.println(String.format("Removing disconnected client[%s] from list", client.getClientId()));
                disconnect(client);
            }
            return failedToSend;
        });
    }

    /**
     * Attempts to see if the message is a command and process its action
     * 
     * @param message
     * @param sender
     * @return true if it was a command, false otherwise
     */
     // Adding the math game activity
     private void randomME() { // creates random mathe equation that relays the message to all clients
        int a = random.nextInt(11); // 2 random int's that range from 0-10
        int b = random.nextInt(11);
        String[] ops = {"+", "-", "*"}; // 3 random operators
        int index = random.nextInt(ops.length);
        String op = ops[index];
        switch (op) {
            case "+":
                answer = a + b; // If operation is selected it will add
                break;
            case "-":
                answer = a - b; // subtract
                break;
            case "*":
                answer = a * b; // multiply
                break;
        }
        String msg = String.format("The equation: %d %s %d", a, op, b); // printed with placeholders
        relay(msg, null); // gets sents to clients
    }

    private boolean processCommand(String message, ServerThread sender) {
        if (sender == null) {
            return false;
        }
        System.out.println("Checking command: " + message);

        if (message.equalsIgnoreCase("/start")) {
            randomME();  
            return true;
            // message starts if relayed
        } else if (message.startsWith("/answer")) {
            if (answer == 100000) {
                relay("A game is not running, use /start to begin.", sender); /* Here the client answers the question and makes a guess
                The answer is also set too 100k to see if the the game is running, so if not, it relays the message below. */
            } else {
                String guessString = message.replace("/answer", "").trim();
                try {
                    int guess = Integer.parseInt(guessString);// clinet gues --> int
                    if (guess == answer) { // if guess = answer, client id number and number guessed is diplayed
                        relay(String.format("User[%d] guessed %s and it was correct", sender.getClientId(), guess), sender);
                        randomME();// Another random equation is created if correct
                    } else {
                        relay(String.format("User[%d] guessed %s and it was wrong.", sender.getClientId(), guess), sender);
                    }
                } catch (NumberFormatException e) {
                    relay("Wrong format. Please provide a number.", sender);
                    e.printStackTrace(); 
                }// will relay invalid format error to client and will show client

            } 
            return true;
        } else if (message.equalsIgnoreCase("/stop")) {
            answer = 100000;
            relay("Math game stopped.", sender);
            return true;
            // stops math game
            // Coin flip begins
        } else if (message.equalsIgnoreCase("/flip"))
         {// If flip is entered, a random int will be made and it will either be 0(heads) or 1 (tails)
            String result = random.nextInt(2) == 0 ? "heads" : "tails";
            relay(String.format("Flipped a coin and got %s.", result), sender);
            return true;
            // relays the coin flip results along with the client ID
        } else if ("/disconnect".equalsIgnoreCase(message)) {
            ServerThread removedClient = connectedClients.get(sender.getClientId());
            if (removedClient != null) {
                disconnect(removedClient);
            }
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // will default to the defined value
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}