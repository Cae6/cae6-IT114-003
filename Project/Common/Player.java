package Project.Common;

/**
 * Common Player data shared between Client and Server
 */
public class Player {
    public static long DEFAULT_CLIENT_ID = -1L;
    private long clientId = Player.DEFAULT_CLIENT_ID;
    private boolean isReady = false;
    private boolean takeTurn = false;
    private boolean isRemoved = false;
    private int points = 0;
    private String choice;
    private String clientName;
    private boolean isSpectator;
    private boolean isAway = false;

    
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    
    public long getClientId() {
        return clientId;
    }
    
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public boolean isReady() {
        return isReady;
    }
    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public boolean didTakeTurn() {
        return takeTurn;
    }

    public void setTakeTurn(boolean tookTurn) {
        this.takeTurn = tookTurn;
    }

    
    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public boolean getRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    public void setPoints(int p){
        this.points = p;
    }
    public void changePoints(int p){
        this.points += p;
        this.points = Math.max(this.points, 0); // minimum 0 points
    }
    public int getPoints(){
        return this.points;
    }

    private boolean eliminated = false;

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }
    public void setSpectator(boolean spectator) {
        this.isSpectator = spectator;
    }
    
    public boolean isSpectator() {
        return isSpectator;
    }

    public boolean isAway() {
        return isAway;
    }

    public void setIsAway(boolean isAway) {
        this.isAway = isAway;
    }

    
    /**
     * Resets all of the data (this is destructive).
     * You may want to make a softer reset for other data
     */
    public void reset(){
    
        this.takeTurn = false;
        this.points = 0;
        this.choice =null;
        this.eliminated = false;
     
    }
}