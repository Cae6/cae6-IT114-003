package Project.Common;

public class EliminatedPayload extends Payload {

    public EliminatedPayload(){
        setPayloadType(PayloadType.ELIMINATED);
    }


    private boolean eliminated = false;

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }
}
