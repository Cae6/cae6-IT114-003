package Project.Server;

import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.TimerType;

/**
 * Server-only data about a player
 * Added in ReadyCheck lesson/branch for non-chatroom projects.
 * If chatroom projects want to follow this design update the following in this lesson:
 * Player class renamed to User
 * clientPlayer class renamed to ClientUser (or the original ClientData)
 * ServerPlayer class renamed to ServerUser
 */
public class ServerPlayer extends Player{
    private ServerThread client; // reference to wrapped ServerThread
    public ServerPlayer(ServerThread clientToWrap){
        client = clientToWrap;
        setClientId(client.getClientId());
    }
    /**
     * Used only for passing the ServerThread to the base class of Room.
     * Favor creating wrapper methods instead of interacting with this directly.
     * @return ServerThread reference
     */
    public ServerThread getServerThread(){
        return client;
    }
    // add any wrapper methods to call on the ServerThread
    // don't used the exposed full ServerThread object
    public boolean sendReadyStatus(long clientId, boolean isReady, boolean quiet){
        return client.sendReadyStatus(clientId, isReady, quiet);
    }
    public boolean sendReadyStatus(long clientId, boolean isReady){
       return client.sendReadyStatus(clientId, isReady);
    }

    public boolean sendResetReady(){
        return client.sendResetReady();
    }

      public boolean sendCurrentTime(TimerType timerType, int time) {
        return client.sendCurrentTime(timerType, time);
    }

    public boolean sendEliminated(long clientID, String clientName ) {
        return client.sendEliminated(clientID,clientName);
    }

    public boolean sendSpectatorStatus(long clientId, String clientName, boolean isSpectator) {
        return client.sendSpectatorStatus(clientId, clientName, isSpectator);
    }


    public boolean sendCurrentPhase(Phase phase){
        return client.sendCurrentPhase(phase);
    }

    public boolean sendTurnStatus(long clientId, boolean didTakeTurn, String choice, boolean isAway) {
        return client.sendTurnStatus(clientId, didTakeTurn, choice,isAway());
    }

    public boolean sendChoice(String choice) {
        return client.sendChoice(choice);
    }

    public boolean sendPointsUpdate(long clientId, int points, String clientName) {
        return client.sendPointsUpdate(clientId, clientName, points);
    }

    public String getClientName() {
        return client.getClientName();
    }

    public boolean sendGameEvent(String message){
        return client.sendGameEvent(message);
    }
}