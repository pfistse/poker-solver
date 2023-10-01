package poker.tree;

public class Action {


    public enum Type {
        FOLD,
        CHECK,
        CALL,
        BET,
        RAISE
    }

    Type type;
    int amount;

    public Action(Type type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public Type getType() {
        return this.type;
    }

    public int getAmount() {
        return this.amount;
    }

    @Override
    public String toString() {
        if (amount > 0)
            return type.toString().toLowerCase() + " " + amount;
        return type.toString();
    }

    public static boolean isValidPlayerAction(GameState gameState, boolean oopIsPlaying, Action action) {
        return switch (action.type) {
            case BET, RAISE -> action.amount <= gameState.stackPlayers[oopIsPlaying ? 0 : 1];
            default -> true;
        };
    }
}