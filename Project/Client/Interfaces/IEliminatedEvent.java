package Project.Client.Interfaces;

public interface IEliminatedEvent extends IGameEvents{
    /**
     * Receives the current phase
     * 
     * @param phase
     */
    void markEliminated(long clientId, String clientName);
}
