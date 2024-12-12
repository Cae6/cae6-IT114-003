package Project.Common;

public class SpectatorPayload extends Payload{
    private boolean isSpectator;
    
    public SpectatorPayload(){
        setPayloadType(PayloadType.SPECTATOR);
    }
    public boolean isSpectator() {
        return isSpectator;
    }

    public void setSpectator(boolean isSpectator) {
        this.isSpectator = isSpectator;
    }

    

    
    public String toString(){
        return super.toString() + String.format(" isSpectator [%s]", isSpectator?"spectator":"not spectator");
    }
    
}
