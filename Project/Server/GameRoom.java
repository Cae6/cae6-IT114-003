package Project.Server;

import java.util.Arrays;
//import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.Player;
//import Project.Common.Player;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;
import Project.Common.TimedEvent;

public class GameRoom extends BaseGameRoom {

    private TimedEvent roundTimer = null;
    private TimedEvent turnTimer = null;
   

    public GameRoom(String name) {
        super(name);
    }

    @Override
    protected void onClientAdded(ServerPlayer sp) {
      sp.setClientName(sp.getClientName());
        syncCurrentPhase(sp);
        syncReadyStatus(sp);
    }

    @Override
    protected void onClientRemoved(ServerPlayer sp) {
        // Logic for client removal (if necessary)
    }

    // Timer handlers
    private void startRoundTimer() {
        roundTimer = new TimedEvent(50, this::onRoundEnd); // 50 second timer
        roundTimer.setTickCallback(time -> {
            if (time % 10 == 0 || time <= 3) { 
                sendMessage(ServerConstants.FROM_ROOM, String.format("Time remaining: %d seconds", time));
            }
        });}

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    /*private void startTurnTimer(ServerThread sender) {
        if (playersInRoom.size() < 2) {
            sendMessage(ServerConstants.FROM_ROOM, "Waiting for more players...");
            return;
        }
        resetTurnTimer();

        turnTimer = new TimedEvent(45, this::onTurnEnd);
        turnTimer.setTickCallback(time -> {
            if (time > 0) {
                checkEarlyEnd(time);
                if (time > 5) {
                    sendMessage(ServerConstants.FROM_ROOM, String.format("Time remaining: %d seconds", time));
                } else {
                    sendMessage(ServerConstants.FROM_ROOM, String.format("Hurry! %d seconds left!", time));
                }
            }
        });

       
        sendMessage(ServerConstants.FROM_ROOM, String.format("%s's turn - Pick /R, /P, /S, or /skip", sender.getClientName()));
    }
*/
    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }

   /*  private void checkEarlyEnd(int timeRemaining) {
        long numEnded = playersInRoom.values().stream().filter(ServerPlayer::didTakeTurn).count();
        if (numEnded >= playersInRoom.size()) {
            sendMessage(ServerConstants.FROM_ROOM, "All players have completed their turn early. Ending turn...");
            handleResult();
        }
    }
*/
    // Lifecycle methods
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.IN_PROGRESS);
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");

        // Reset player states for the new round
        resetReadyStatus();

        resetPlayer();
        changePhase(Phase.READY);
    
        // Start the round timer
        resetRoundTimer(); // Ensure any previous timer is canceled
        startRoundTimer();
    
    
        // Change the phase to indicate the game is in progress
        

        if (!areAllPlayersReady()) {
            sendMessage(ServerConstants.FROM_ROOM, "Waiting for all players to ready up...");
            return; // Do not proceed until all players are ready
        }
    
        // Transition to the TURN phase, where players make choice
        changePhase(Phase.TURN);
        sendMessage(ServerConstants.FROM_ROOM, "Choices are: Rock (R), Paper (P), Scissors (S), or skip (skip)");
    
        // Notify players
        sendMessage(ServerConstants.FROM_ROOM, "The round has started! Submit your choices now.");
        LoggerUtil.INSTANCE.info("onRoundStart() end");
    }

    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
       // startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer();  // Reset the timer for the current turn

        if (didAllTakeTurn()) {
            LoggerUtil.INSTANCE.info("All players have completed their turns. Ending the round...");
            onRoundEnd(); // End the current round
        }

        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer();

        
        handleResult(); // reset timer if round ended without the time expiring
        onSessionEnd();
        LoggerUtil.INSTANCE.info("onRoundEnd() end");
    }

    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        resetReadyStatus();
        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }

    private boolean areAllPlayersReady() {
        return playersInRoom.values().stream().allMatch(ServerPlayer::isReady);
    }

    public void handleTurn(ServerThread sender, String choice){
        try {
            ServerPlayer sp = (ServerPlayer) playersInRoom.get(sender.getClientId());
            if (sp == null) {
                sender.sendMessage("Error: Player not found in room");
                return;
            }


            checkPlayerIsReady(sp);
            checkPlayerTookTurn(sp);

            if ("skip".equalsIgnoreCase(choice)) {
                sp.setEliminated(true); // Mark player as eliminated
                sendMessage(ServerConstants.FROM_ROOM, String.format("%s skipped their turn and has been eliminated!", sp.getClientName()));
                return; // Exit as the player is no longer part of the round
            }
    

            sp.setChoice(choice);
            sp.setTakeTurn(true);

           

            if (didAllTakeTurn()) {
                sendMessage(ServerConstants.FROM_ROOM, "All players have submitted their choices!");
                resetRoundTimer();
                onRoundEnd();

            } 
           

        } catch (Exception e) {
            sender.sendMessage("Error processing turn: " + e.getMessage());
            LoggerUtil.INSTANCE.severe("Error in handleTurn", e);
        }
    }

    private boolean didAllTakeTurn() {
        long ready = playersInRoom.values().stream().filter(p -> p.isReady()&& !p.isEliminated()).count();
        long tookTurn = playersInRoom.values().stream().filter(p -> p.isReady() && p.didTakeTurn()&& !p.isEliminated()).count();
        LoggerUtil.INSTANCE.info(String.format("didAllTakeTurn() %s/%s", tookTurn, ready));
        return ready == tookTurn;
    }

    private void checkPlayerIsReady(ServerPlayer sp) throws Exception {
        if (!sp.isReady()) {
            throw new Exception("Player isn't ready");
        }
    }

    private void checkPlayerTookTurn(ServerPlayer sp) throws Exception {
        if (sp.didTakeTurn()) {
            throw new Exception("Player already took turn");
        }
    }

    private void handleResult() {
        

        System.out.println(TextFX.colorize("Handling end of turn", Color.YELLOW));
        playersInRoom.values().stream()
        .filter(player -> !player.isEliminated()&& (player.getChoice() == null || !player.didTakeTurn()))
        .forEach(player -> player.setEliminated(true));

        

        //players choices before processing 
        List<ServerPlayer> playersToProcess = playersInRoom.values().stream()
        .filter(player -> player.isReady() && player.didTakeTurn() && player.getChoice() != null&& !player.isEliminated())
        .toList();

    if (playersToProcess.isEmpty()) {
    sendMessage(ServerConstants.FROM_ROOM, "No players made a choice. Restarting the round...");
    onRoundStart();
    return;
    }

        // 
        int numPlayers = playersToProcess.size();
        for (int i = 0; i < numPlayers; i++) {
            ServerPlayer player1 = playersToProcess.get(i);
            ServerPlayer player2 = playersToProcess.get((i + 1) % numPlayers);

            String choice1 = player1.getChoice();
            String choice2 = player2.getChoice();

            int result = checkMatch(choice1, choice2);

            if (result == 0) {
                sendMessage(ServerConstants.FROM_ROOM, String.format("%s ties with %s [%s - %s]",
                        player1.getClientName(), player2.getClientName(), choice1, choice2));
            } else if (result == 1) {
                sendMessage(ServerConstants.FROM_ROOM, String.format("%s wins against %s [%s - %s]",
                        player1.getClientName(), player2.getClientName(), choice1, choice2));
                        player1.setPoints(player1.getPoints() + 1);
                        player2.setEliminated(true); 
            } else {
                sendMessage(ServerConstants.FROM_ROOM, String.format("%s wins against %s [%s - %s]",
                        player2.getClientName(), player1.getClientName(), choice2, choice1));
                        player2.setPoints(player2.getPoints() + 1);
                        player1.setEliminated(true); 
            }
        }

       
        
        evaluateRoundEnd();
        broadcastLeaderboard();

        sendMessage(ServerConstants.FROM_ROOM, "Round complete!");
        
        LoggerUtil.INSTANCE.info("onRoundEnd() end");
    }

   private int checkMatch(String choice1, String choice2) {
    List<String> c = Arrays.asList("R", "P", "S");
    int a = c.indexOf(choice1);
    int b = c.indexOf(choice2);


    // Determine the result of the match
    if (a == b) {
        // Tie
        return 0;
    }
    if ((a - b + 3) % 3 == 1) {
        // Win
        return 1;
    }
    else {
        // Lose
        return -1;
    }
}


private void resetPlayerChoices() {
    playersInRoom.values().forEach(player -> {
        if (!player.isEliminated()) {
            player.setChoice(null);
            player.setTakeTurn(false);
            
        }
    });
}

private void resetPlayer() {
    playersInRoom.values().forEach(Player::reset);
}



private void broadcastLeaderboard() {
    // going in descending order
    List<ServerPlayer> sortedPlayers = playersInRoom.values().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getPoints(), p1.getPoints()))
            .toList();

    // leaderboard message to all clients
    String leaderboard = "Leaderboard:\n" + sortedPlayers.stream()
            .map(player -> String.format("%s: %d points", player.getClientName(), player.getPoints(), player.isEliminated() ? " (Eliminated)" : ""))
            .collect(Collectors.joining("\n"));

    //  leaderboard being sent to all the clients
    sendMessage(ServerConstants.FROM_ROOM, leaderboard);
}
private void evaluateRoundEnd() {
    // Filter players who are still active and have not made a choice yet
    List<ServerPlayer> remainingPlayers = playersInRoom.values().stream()
        .filter(player -> !player.isEliminated() && player.getChoice() != null)
        .toList();

    sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(remainingPlayers.size() + " left", Color.YELLOW));

    if (remainingPlayers.size() == 1) {
        // One player remaining, they win
        ServerPlayer winner = remainingPlayers.get(0);
        sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(winner.getClientName() + " won!", Color.BLUE));
        onSessionEnd(); // End the session
    } else if (remainingPlayers.size() > 1) {
        // More than one player left with no choice, restart the round
        sendMessage(ServerConstants.FROM_ROOM, "More than 1 player remains. Restarting the round...");
        resetPlayerChoices(); // Reset choices for all players
        onRoundStart(); // Start the next round
    } else {
        // No players left or all made choices, it's a tie
        sendMessage(ServerConstants.FROM_ROOM, "It's a tie!");
        onSessionEnd(); // End the session
    }
}
}