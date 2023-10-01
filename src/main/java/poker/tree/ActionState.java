package poker.tree;

import java.util.ArrayList;
import java.util.List;

public class ActionState {
    GameState gameState;
    GameTreeBuildSettings settings;
    Action action;

    ActionState(GameState gameState, GameTreeBuildSettings settings) {
        this(gameState, settings, null);
    }

    private ActionState(GameState gameState, GameTreeBuildSettings settings, Action action) {
        this.gameState = gameState;
        this.settings = settings;
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    private ActionState getFollowUpCheckActionState() {
        return new ActionState(gameState.afterCheck(), settings, new Action(Action.Type.CHECK, 0));
    }

    private List<ActionState> getFollowUpBetActionStates() {
        ArrayList<ActionState> states = new ArrayList<>();

        for (float betSize : settings.getBetSizes(gameState.street)) {

            int maxBetAmount = Math.round(betSize * gameState.pot);
            int betAmount = Math.min(maxBetAmount,
                    Math.min(gameState.stackPlayers[0], gameState.stackPlayers[0]));

            Action action = new Action(Action.Type.BET, betAmount);

            if (Action.isValidPlayerAction(gameState, gameState.oopIsPlaying, action)) {
                states.add(new ActionState(gameState.afterRaise(betAmount), settings,
                        action));
            }
        }

        return states;
    }

    private List<ActionState> getFollowUpRaiseActionStates() {

        ArrayList<ActionState> states = new ArrayList<>();

        // raise limit reached or stack size of one player does not allow a raise
        if (gameState.streetRaiseCnt == gameState.raiseLimit || gameState.stackPlayers[0] == gameState.streetCallAmount
                || gameState.stackPlayers[1] == gameState.streetCallAmount)
            return states;

        for (float raiseSize : settings.getRaiseSizes(gameState.street)) {

            int maxRaiseAmount = Math.round(raiseSize * (gameState.pot + 2 * gameState.streetCallAmount))
                    + gameState.streetCallAmount;
            int raiseAmount = Math.min(maxRaiseAmount,
                    Math.min(gameState.stackPlayers[0], gameState.stackPlayers[0]));

            Action action = new Action(Action.Type.RAISE, raiseAmount);

            if (Action.isValidPlayerAction(gameState, gameState.oopIsPlaying, action))
                states.add(new ActionState(gameState.afterRaise(raiseAmount), settings,
                        action));
        }

        return states;
    }

    private ActionState getFollowUpCallActionState() {
        return new ActionState(gameState.afterCall(), settings,
                new Action(Action.Type.CALL, gameState.streetCallAmount));
    }

    private ActionState getFollowUpFoldActionState() {
        return new ActionState(gameState.afterFold(), settings, new Action(Action.Type.FOLD, 0));
    }

    public List<ActionState> getFollowUpStates() {
        if (gameState.streetTerminated())
            throw new RuntimeException("terminated");

        if (action == null) {
            List<ActionState> states = getFollowUpBetActionStates();
            states.add(getFollowUpCheckActionState());
            return states;
        }

        return switch (action.type) {
            case CHECK -> {
                List<ActionState> states = getFollowUpBetActionStates();
                states.add(getFollowUpCheckActionState());
                yield states;
            }
            case BET, RAISE, CALL, FOLD -> {
                List<ActionState> states = getFollowUpRaiseActionStates();
                states.add(getFollowUpCallActionState());
                states.add(getFollowUpFoldActionState());
                yield states;
            }
            default -> {
                throw new RuntimeException("invalid action type");
            }
        };
    }

    public boolean isInitial() {
        return action == null;
    }

    public GameState getGameState() {
        return gameState;
    }

    @Override
    public String toString() {
        if (action != null)
            return action + " " + gameState;

        return "root";
    }
}