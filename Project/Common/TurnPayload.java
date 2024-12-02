package Project.Common;

public class TurnPayload extends Payload {
    private long clientId; 
    private String choice; 
    private boolean takeTurn;
   

    public TurnPayload(){
        setPayloadType(PayloadType.TURN);
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
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
}
