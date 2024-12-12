package Project.Client.Interfaces;

public interface ISpectatorEvent extends IGameEvents {
    /**
     * Receives the current phase
     * 
     * @param phase
     */
    abstract void onSpectatorStatus(long clientId, String clientName, boolean isSpectator);
    
}
