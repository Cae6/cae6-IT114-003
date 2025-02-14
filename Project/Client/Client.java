package Project.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Project.Client.Interfaces.ITimeEvents;

import Project.Client.Interfaces.IClientEvents;
import Project.Client.Interfaces.IConnectionEvents;
import Project.Client.Interfaces.IMessageEvents;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IPointsEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Interfaces.ITurnEvent;
import Project.Client.Interfaces.IEliminatedEvent;
//import Project.Client.Interfaces.ISpectatorEvent;
import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.EliminatedPayload;
import Project.Common.LoggerUtil;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.Phase;
import Project.Common.ReadyPayload;
import Project.Common.RoomResultsPayload;
import Project.Common.SpectatorPayload;
import Project.Common.PointsPayload;
import Project.Common.TextFX;
import Project.Common.TurnPayload;
import Project.Common.TextFX.Color;
import Project.Common.TimerPayload;
import Project.Common.TimerType;

/**
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */

public enum Client {
    INSTANCE;

    {
        // statically initialize the client-side LoggerUtil
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        // Set the logger configuration
        LoggerUtil.INSTANCE.setConfig(config);
    }
    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility
    private ConcurrentHashMap<Long, ClientPlayer> knownClients = new ConcurrentHashMap<>();
    private ClientPlayer myData;
    private Phase currentPhase = Phase.READY;

    // constants (used to reduce potential types when using them in code)
    private final String COMMAND_CHARACTER = "/";
    private final String CREATE_ROOM = "createroom";
    private final String JOIN_ROOM = "joinroom";
    private final String LIST_ROOMS = "listrooms";
    private final String DISCONNECT = "disconnect";
    private final String LOGOFF = "logoff";
    private final String LOGOUT = "logout";
    private final String SINGLE_SPACE = " ";
    private final String SPECTATOR = "spectator";
    private final String READY = "ready";
    
 private static List<IClientEvents> events = new ArrayList<IClientEvents>();
    // needs to be private now that the enum logic is handling this
    private Client() {
        LoggerUtil.INSTANCE.info("Client Created");
        myData = new ClientPlayer();
    }

    public void addCallback(IClientEvents e) {
        events.add(e);
    }
    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
        } catch (UnknownHostException e) {
            LoggerUtil.INSTANCE.warning("Unknown host", e);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("IOException", e);
        }
        return isConnected();
    }

    public boolean connect(String address, int port, String username, IClientEvents callback) {
        myData.setClientName(username);
        addCallback(callback);
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
            sendClientName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }


    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
          * @throws IOException 
          */
         private boolean processClientCommand(String text) throws IOException {
        if (isConnection(text)) {
            if (myData.getClientName() == null || myData.getClientName().length() == 0) {
                System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
                return true;
            }
            // replaces multiple spaces with a single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            sendClientName();
            return true;
        } else if ("/quit".equalsIgnoreCase(text)) {
            close();
            return true;
        } else if (text.startsWith("/name")) {
            myData.setClientName(text.replace("/name", "").trim());
            System.out.println(TextFX.colorize("Set client name to " + myData.getClientName(), Color.CYAN));
            return true;
        } else if (text.equalsIgnoreCase("/users")) {
            // chatroom version
            /*
             * System.out.println(
             * String.join("\n", knownClients.values().stream()
             * .map(c -> String.format("%s(%s)", c.getClientName(),
             * c.getClientId())).toList()));
             */
            // non-chatroom version
            System.out.println(
                    String.join("\n", knownClients.values().stream()
                            .map(c -> String.format("%s(%s) %s", c.getClientName(), c.getClientId(),
                                    c.isReady() ? "[x]" : "[ ]"))
                            .toList()));
            return true;
        } else { // logic previously from Room.java
            // decided to make this as separate block to separate the core client-side items
            // vs the ones that generally are used after connection and that send requests
            if (text.startsWith(COMMAND_CHARACTER)) {
                boolean wasCommand = false;
                String fullCommand = text.replace(COMMAND_CHARACTER, "");
                String part1 = fullCommand;
                String[] commandParts = part1.split(SINGLE_SPACE, 2);// using limit so spaces in the command value
                                                                     // aren't split
                final String command = commandParts[0];
                final String commandValue = commandParts.length >= 2 ? commandParts[1] : "";
                switch (command) {
                    case CREATE_ROOM:
                        sendCreateRoom(commandValue);
                        wasCommand = true;
                        break;
                    case JOIN_ROOM:
                        sendJoinRoom(commandValue);
                        wasCommand = true;
                        break;
                    case LIST_ROOMS:
                        sendListRooms(commandValue);
                        wasCommand = true;
                        break;
                    // Note: these are to disconnect, they're not for changing rooms
                    case DISCONNECT:
                    case LOGOFF:
                    case LOGOUT:
                        sendDisconnect();
                        wasCommand = true;
                        break;
                    // others
                    case READY:
                        sendReady();
                        wasCommand = true;
                        break;
                        case "R":
                        sendTurnAction("R");
                        wasCommand = true;
                        break;
                    case "P":
                        sendTurnAction("P");
                        wasCommand = true;
                        break;
                    case "S":
                        sendTurnAction("S");
                        wasCommand = true;
                        break;
                        case "skip":
                    sendTurnAction(commandValue);
                    wasCommand = true;
                    break;
                case "Spock":
                sendTurnAction(commandValue);
                wasCommand = true;
                break;
                case "Lizard":
                sendTurnAction(commandValue);
                wasCommand = true;
                break;
                case "away":
                sendTurnAction(commandValue);
                break;
                case SPECTATOR:
                sendSpectator(commandValue);
                wasCommand = true;
                break;
    
                }
                return wasCommand;
            }
        }
        return false;
    }

    

    // send methods to pass data to the ServerThread

    /**
     * Sends the client's intent to be ready.
     * Can also be used to toggle the ready state if coded on the server-side
     */
    public long getMyClientId() {
        return myData.getClientId();
    }

  public void clientSideGameEvent(String str) {
        events.forEach(event -> {
            if (event instanceof IMessageEvents) {
                // Note: using -2 to target GameEventPanel
                ((IMessageEvents) event).onMessageReceive(Constants.GAME_EVENT_CHANNEL, str);
            }
        });
    }
     public void sendTurnAction(String choice) throws IOException  {
        TurnPayload tp = new TurnPayload();
        tp.setPayloadType(PayloadType.TURN);
        tp.setChoice(choice);
        tp.setIsAway(choice.equalsIgnoreCase("away"));
        send(tp);
     }


   
    public void sendReady() throws IOException {
        ReadyPayload rp = new ReadyPayload();
        rp.setReady(true); // <- techically not needed as we'll use the payload type as a trigger
        send(rp);
    }

    public void sendSpectator(String clientName) throws IOException {
        SpectatorPayload sp= new SpectatorPayload();
        sp.setPayloadType(PayloadType.SPECTATOR);
        sp.setClientName(clientName);
        send(sp);
    }

    /**
     * Sends a search to the server-side to get a list of potentially matching Rooms
     * 
     * @param roomQuery optional partial match search String
     */
    public void sendListRooms(String roomQuery)throws IOException   {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_LIST);
        p.setMessage(roomQuery);
        send(p);
    }

    /**
     * Sends the room name we intend to create
     * 
     * @param room
     */
    public void sendCreateRoom(String room) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_CREATE);
        p.setMessage(room);
        send(p);
    }

    /**
     * Sends the room name we intend to join
     * 
     * @param room
     */
    public void sendJoinRoom(String room)throws IOException  {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROOM_JOIN);
        p.setMessage(room);
        send(p);
    }

    /**
     * Tells the server-side we want to disconnect
     */
    void sendDisconnect() throws IOException  {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.DISCONNECT);
        send(p);
    }

    /**
     * Sends desired message over the socket
     * 
     * @param message
     */
    public void sendMessage(String message) throws IOException{
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
        send(p);
    }

    /**
     * Sends chosen client name after socket handshake
     */
    private void sendClientName() {
        if (myData.getClientName() == null || myData.getClientName().length() == 0) {
            System.out.println(TextFX.colorize("Name must be set first via /name command", Color.RED));
            return;
        }
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientName(myData.getClientName());
        send(cp);
    }

    /**
     * Generic send that passes any Payload over the socket (to ServerThread)
     * 
     * @param p
     */
    private void send(Payload p) {
        try {
            out.writeObject(p);
            out.flush();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Socket send exception", e);
        }

    }
    // end send methods

    public void start() throws IOException {
        LoggerUtil.INSTANCE.info("Client starting");

        // Use CompletableFuture to run listenToInput() in a separate thread
        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        // Wait for inputFuture to complete to ensure proper termination
        inputFuture.join();
    }

    /**
     * Listens for messages from the server
     */
    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject(); // blocking read
                if (fromServer != null) {
                    // System.out.println(fromServer);
                    processPayload(fromServer);
                } else {
                    LoggerUtil.INSTANCE.info("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            LoggerUtil.INSTANCE.severe("Error reading object as specified type: ", cce);
        } catch (IOException e) {
            if (isRunning) {
                LoggerUtil.INSTANCE.info("Connection dropped", e);
            }
        } finally {
            closeServerConnection();
        }
        LoggerUtil.INSTANCE.info("listenToServer thread stopped");
    }

    /**
     * Listens for keyboard input from the user
     */
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            System.out.println("Waiting for input"); // moved here to avoid console spam
            while (isRunning) { // Run until isRunning is false
                String line = si.nextLine();
                if (!processClientCommand(line)) {
                    if (isConnected()) {
                        sendMessage(line);
                    } else {
                        System.out.println(
                                "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)");
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Error in listentToInput()", e);
        }
        LoggerUtil.INSTANCE.info("listenToInput thread stopped");
    }

    /**
     * Closes the client connection and associated resources
     */
    private void close() {
        isRunning = false;
        closeServerConnection();
        LoggerUtil.INSTANCE.info("Client terminated");
        // System.exit(0); // Terminate the application
    }

    /**
     * Closes the server connection and associated resources
     */
    private void closeServerConnection() {
        myData.reset();
        knownClients.clear();
        try {
            if (out != null) {
                LoggerUtil.INSTANCE.info("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing output stream", e);
        }
        try {
            if (in != null) {
                LoggerUtil.INSTANCE.info("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.info("Error closing input stream", e);
        }
        try {
            if (server != null) {
                LoggerUtil.INSTANCE.info("Closing connection");
                server.close();
                LoggerUtil.INSTANCE.info("Closed socket");
            }
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Error closing socket", e);
        }
    }

    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.info("Exception from main()", e);
        }
    }

    /**
     * Handles received message from the ServerThread
     * 
     * @param payload
     */
    private void processPayload(Payload payload) {
        try {
            LoggerUtil.INSTANCE.info("Received Payload: " + payload);
            switch (payload.getPayloadType()) {
                case PayloadType.CLIENT_ID: // get id assigned
                    ConnectionPayload cp = (ConnectionPayload) payload;
                    processClientData(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.SYNC_CLIENT: // silent add
                    cp = (ConnectionPayload) payload;
                    processClientSync(cp.getClientId(), cp.getClientName());
                    break;
                case PayloadType.DISCONNECT: // remove a disconnected client (mostly for the specific message vs leaving
                                             // a room)
                    cp = (ConnectionPayload) payload;
                    processDisconnect(cp.getClientId(), cp.getClientName());
                    // note: we want this to cascade
                case PayloadType.ROOM_JOIN: // add/remove client info from known clients
                    cp = (ConnectionPayload) payload;
                    processRoomAction(cp.getClientId(), cp.getClientName(), cp.getMessage(), cp.isConnect());
                    break;
                case PayloadType.ROOM_LIST:
                    RoomResultsPayload rrp = (RoomResultsPayload) payload;
                    processRoomsList(rrp.getRooms(), rrp.getMessage());
                    break;
                case PayloadType.MESSAGE: // displays a received message
                    processMessage(payload.getClientId(), payload.getMessage());
                    break;
                case PayloadType.READY:
                    ReadyPayload rp = (ReadyPayload) payload;
                    processReadyStatus(rp.getClientId(), rp.isReady(), false);
                    break;
                case PayloadType.SYNC_READY:
                    ReadyPayload qrp = (ReadyPayload)payload;
                    processReadyStatus(qrp.getClientId(), qrp.isReady(), true);
                    break;
                case PayloadType.RESET_READY:
                    // note no data necessary as this is just a trigger
                    processResetReady();
                    break;
                case PayloadType.PHASE:
                    processPhase(payload.getMessage());
                    break;
                case PayloadType.TURN:
                TurnPayload tp = (TurnPayload)payload;
                processTurnStatus(tp.getClientId(), tp.didTakeTurn(),tp.getChoice(),tp.isAway());
                break;
                case PayloadType.POINTS:
                PointsPayload pp = (PointsPayload) payload;
                processPoints(pp.getClientId(), pp.getPoints());
                break;
                case PayloadType.TIME:
                TimerPayload timerPayload = (TimerPayload) payload;
                processCurrentTimer(timerPayload.getTimerType(), timerPayload.getTime());
                break;
                case PayloadType.ELIMINATED:
                EliminatedPayload ep = (EliminatedPayload) payload;
                processEliminated(ep.getClientId(), ep.getClientName());
                break;
                case PayloadType.SPECTATOR:
                SpectatorPayload sp = (SpectatorPayload) payload;
                processSpectator(sp.getClientId(),sp.getClientName(), sp.isSpectator());
              
                default:
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Could not process Payload: " + payload, e);
        }
    }


    public void processEliminated(long clientId, String clientName) { //cae6
        ClientPlayer cp = knownClients.get(clientId);
        if (cp != null) {
            cp.setEliminated(true);
            System.out.println(TextFX.colorize(
                    String.format("Client %s[%s] has been eliminated", clientName, clientId),
                    Color.RED));
        }
        events.forEach(event -> {
            if (event instanceof IEliminatedEvent) {
                ((IEliminatedEvent) event).markEliminated(clientId, clientName);
            }
        });
    }

  

    public String getClientNameFromId(long id) {
        if (id == ClientPlayer.DEFAULT_CLIENT_ID) {
            return "Room";
        }
        if (knownClients.containsKey(id)) {
            return knownClients.get(id).getClientName();
        }
        return "[Unknown]";
    }

    private void processCurrentTimer(TimerType timerType, int time) { //cae6
        events.forEach(event -> {
            if (event instanceof ITimeEvents) {
                ((ITimeEvents) event).onTimerUpdate(timerType, time);
            }
        });
    }
    private void processTurnStatus(long clientId, boolean didTakeTurn, String choice, boolean isAway) {//cae6
      
            ClientPlayer cp = knownClients.get(clientId);
            if (cp != null) {
                cp.setTakeTurn(didTakeTurn);
                cp.setChoice(choice);
                cp.setIsAway(isAway);
                if (didTakeTurn) {
                        System.out.println(TextFX.colorize(
                                String.format("Client %s[%s] chose %s",choice,cp.getClientName()),
                                Color.GREEN));

                                events.forEach(event -> {
                                    if (event instanceof IMessageEvents) {
                                        ((IMessageEvents) event).onMessageReceive(Constants.GAME_EVENT_CHANNEL,
                                                String.format("%s[%s] finished their turn", cp.getClientName(), cp.getClientId()));
                                    }
                                });
                                events.forEach(event -> {
                                    if (event instanceof ITurnEvent) {
                                        ((ITurnEvent) event).onTookTurn(clientId, didTakeTurn, choice);
                                    }
                                });
                }
                if(isAway){
                    System.out.println(TextFX.colorize(
                        String.format("Client %s[%s] is away",cp.getClientName(),cp.getClientId()),
                        Color.RED));
                } else {
                    System.out.println(TextFX.colorize(
                        String.format("Client %s[%s] is back",cp.getClientName(),cp.getClientId()),
                        Color.GREEN));
                }
                events.forEach(event -> {
                    if (event instanceof IMessageEvents) {
                        String message = isAway ? "away" : "back";
                        ((IMessageEvents) event).onMessageReceive(Constants.GAME_EVENT_CHANNEL,
                                String.format("%s[%s] is %s", cp.getClientName(), clientId, message));
                    }
                });
            }
            

       
    }

   
       private void processPoints(long clientId, int points) {//cae6
       
        if (clientId == ClientPlayer.DEFAULT_CLIENT_ID) {
            // Reset points for all clients
          
            knownClients.values().forEach(cp -> cp.setPoints(0));
        } else {
            
            ClientPlayer cp = knownClients.get(clientId);
            if (cp != null) {
                cp.setPoints(points);
            } else {
                LoggerUtil.INSTANCE.severe(String.format("Client ID %s not found while processing points.", clientId));
            }
        }

          events.forEach(event -> {
            if (event instanceof IPointsEvent) {
                ((IPointsEvent) event).onPointsUpdate(clientId, points);
            }
        });
    }


   /*private void processResetTurns() {
        knownClients.values().forEach(cp -> cp.setTakeTurn(false));
        System.out.println("All turns have been reset.");
    }*/
    
    private void processSpectator(long clientId, String clientName, boolean isSpectator) {//cae6
        
        
        ClientPlayer cp = knownClients.get(clientId);
        if (cp != null) {
            cp.setSpectator(isSpectator);
        }
        
        // Optionally, log or display a message
        LoggerUtil.INSTANCE.info(String.format("Client %d is now a %s", clientName, isSpectator ? "spectator" : "player"));
    }
    
    
    private void processPhase(String phase){//cae6
        currentPhase = Enum.valueOf(Phase.class, phase);
        System.out.println(TextFX.colorize("Current phase is " + currentPhase.name(), Color.YELLOW));
         events.forEach(event -> {
            if (event instanceof IPhaseEvent) {
                ((IPhaseEvent) event).onReceivePhase(currentPhase);
            }
        });
    }

    private void processResetReady(){
        knownClients.values().forEach(cp->cp.setReady(false));
        System.out.println("Ready status reset for everyone");
    }
    private void processReadyStatus(long clientId, boolean isReady, boolean quiet) {
        if (!knownClients.containsKey(clientId)) {
            LoggerUtil.INSTANCE.severe(String.format("Received ready status [%s] for client id %s who is not known",
                    isReady ? "ready" : "not ready", clientId));
            return;
        }
        ClientPlayer cp = knownClients.get(clientId);
        cp.setReady(isReady);
        if (!quiet) {
            System.out.println(
                    String.format("%s[%s] is %s", cp.getClientName(), cp.getClientId(),
                            isReady ? "ready" : "not ready"));
        }
         events.forEach(event -> {
            if (event instanceof IReadyEvent) {
                ((IReadyEvent) event).onReceiveReady(clientId, isReady, quiet);
            }
        });
    }

    private void processRoomsList(List<String> rooms, String message) {

          events.forEach(event -> {
            if (event instanceof IRoomEvents) {
                ((IRoomEvents) event).onReceiveRoomList(rooms, message);
            }
        });

        if (rooms == null || rooms.size() == 0) {
            System.out.println(
                    TextFX.colorize("No rooms found matching your query",
                            Color.RED));
            return;
        }
        System.out.println(TextFX.colorize("Room Results:", Color.PURPLE));
        System.out.println(
                String.join("\n", rooms));
    }

    private void processDisconnect(long clientId, String clientName) {
        events.forEach(event -> {
            if (event instanceof IConnectionEvents) {
                ((IConnectionEvents) event).onClientDisconnect(clientId, clientName);
            }
        });
        System.out.println(
                TextFX.colorize(String.format("*%s disconnected*",
                        clientId == myData.getClientId() ? "You" : clientName),
                        Color.RED));
        if (clientId == myData.getClientId()) {
            closeServerConnection();
        }
    }

    private void processClientData(long clientId, String clientName) {
        if (myData.getClientId() == ClientPlayer.DEFAULT_CLIENT_ID) {
            myData.setClientId(clientId);
            myData.setClientName(clientName);
            // knownClients.put(cp.getClientId(), myData);// <-- this is handled later
        }
        events.forEach(event -> {
            if (event instanceof IConnectionEvents) {
                ((IConnectionEvents) event).onReceiveClientId(clientId);
            }
        });
    }

    private void processMessage(long clientId, String message) {//cae6
        String name = knownClients.containsKey(clientId) ? knownClients.get(clientId).getClientName() : "Room";
        System.out.println(TextFX.colorize(String.format("%s: %s", name, message), Color.BLUE));
        events.forEach(event -> {
            if (event instanceof IMessageEvents) {
                ((IMessageEvents) event).onMessageReceive(clientId, message);
            }
        });
    }

    private void processClientSync(long clientId, String clientName) {
        if (!knownClients.containsKey(clientId)) {
            ClientPlayer cd = new ClientPlayer();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
        }
        events.forEach(event -> {
            if (event instanceof IConnectionEvents) {
                ((IConnectionEvents) event).onSyncClient(clientId, clientName);
            }
        });
    }

    private void processRoomAction(long clientId, String clientName, String message, boolean isJoin) {
        if (isJoin && !knownClients.containsKey(clientId)) {
            ClientPlayer cd = new ClientPlayer();
            cd.setClientId(clientId);
            cd.setClientName(clientName);
            knownClients.put(clientId, cd);
            System.out.println(TextFX
                    .colorize(String.format("*%s[%s] joined the Room %s*", clientName, clientId, message),
                            Color.GREEN));
                            events.forEach(event -> {
                                if (event instanceof IRoomEvents) {
                                    ((IRoomEvents) event).onRoomAction(clientId, clientName, message, isJoin);
                                }
                            });
        } else if (!isJoin) {
            ClientPlayer removed = knownClients.remove(clientId);
            if (removed != null) {
                System.out.println(
                        TextFX.colorize(String.format("*%s[%s] left the Room %s*", clientName, clientId, message),
                                Color.YELLOW));
            } events.forEach(event -> {
                if (event instanceof IRoomEvents) {
                    ((IRoomEvents) event).onRoomAction(clientId, clientName, message, isJoin);
                }
            });
            // clear our list
            if (clientId == myData.getClientId()) {
                knownClients.clear();

                events.forEach(event -> {
                    if (event instanceof IConnectionEvents) {
                        ((IConnectionEvents) event).onResetUserList();
                    }
                });
            }
        }
    }
    // end payload processors

}

//./build.sh Project
//./run.sh Project server
//./run.sh Project ui
//./run.sh Project client
// /name Ali
// /name Bil
// /name cia
/// /connect localhost:3000
/// createroom 1
/// joinroom 1
/// ready
