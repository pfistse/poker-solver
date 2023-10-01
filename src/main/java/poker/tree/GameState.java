package poker.tree;

import java.util.Arrays;

import poker.SolverConfig;

public class GameState {

    public int raiseLimit;
    public int streetCheckCnt, streetRaiseCnt, streetCallCnt;

    public Street street;

    public long board;
    public long flop;
    public byte turn;

    public int[] stackPlayers;

    public int pot;
    public int streetSharedWager;
    public int streetCallAmount;

    public boolean oopIsPlaying;
    public boolean onePlayerFolded;

    public GameState(Street street, long board, int initialPot, int[] stackPlayers) {
        this.street = street;
        this.pot = initialPot;
        this.stackPlayers = stackPlayers;
        this.board = board;
        this.flop = board;
        this.onePlayerFolded = false;
        this.oopIsPlaying = true;
        this.streetSharedWager = 0;
        this.streetCallAmount = 0;
        this.streetCheckCnt = 0;
        this.streetCallCnt = 0;
        this.streetRaiseCnt = 0;
        raiseLimit = SolverConfig.RAISE_LIMIT;
    }

    public GameState(GameState prev) {
        this.street = prev.street;
        this.pot = prev.pot;
        this.stackPlayers = prev.stackPlayers.clone();
        this.raiseLimit = prev.raiseLimit;
        this.oopIsPlaying = prev.oopIsPlaying;
        this.streetSharedWager = prev.streetSharedWager;
        this.streetCallAmount = prev.streetCallAmount;
        this.board = prev.board;
        this.onePlayerFolded = prev.onePlayerFolded;

        this.streetCheckCnt = prev.streetCheckCnt;
        this.streetCallCnt = prev.streetCallCnt;
        this.streetRaiseCnt = prev.streetRaiseCnt;
        this.flop = prev.flop;
        this.turn = prev.turn;
    }

    public GameState afterRaise(int raiseAmount) {
        assert (raiseAmount > streetCallAmount);

        GameState state = new GameState(this);
        state.streetSharedWager = streetCallAmount;
        state.streetCallAmount = raiseAmount;
        state.oopIsPlaying = !oopIsPlaying;
        state.streetRaiseCnt++;

        return state;
    }

    public GameState afterCall() {
        GameState state = new GameState(this);
        state.streetSharedWager = streetCallAmount;
        state.oopIsPlaying = !oopIsPlaying;
        state.streetCallCnt++;
        state.collectWagers();

        return state;
    }

    public GameState afterCheck() {
        GameState state = new GameState(this);
        state.oopIsPlaying = !oopIsPlaying;
        state.streetCheckCnt++;

        return state;
    }

    public GameState afterFold() {
        GameState state = new GameState(this);
        state.onePlayerFolded = true;
        state.oopIsPlaying = !oopIsPlaying;
        state.collectWagers();

        return state;
    }

    private void collectWagers() {
        this.pot += 2 * streetSharedWager;
        this.stackPlayers[0] -= streetSharedWager;
        this.stackPlayers[1] -= streetSharedWager;
        this.streetSharedWager = 0;
        this.streetCallAmount = 0;

    }

    public GameState finishStreet(byte... newCards) {
        GameState state = new GameState(this);
        state.streetCheckCnt = 0;
        state.streetCallCnt = 0;
        state.streetRaiseCnt = 0;

        if (onePlayerFolded)
            return state;

        state.street = Street.values()[street.ordinal() + 1];
        switch (state.street) {
            case FLOP:
                state.board |= 1L << newCards[0];
                state.board |= 1L << newCards[1];
                state.board |= 1L << newCards[2];
                state.flop = state.board;
                break;
            case TURN:
                state.turn = newCards[0];
                state.board |= 1L << newCards[0];
                break;
            case RIVER:
                state.board |= 1L << newCards[0];
                break;
            default:
                break;
        }
        return state;
    }

    public boolean streetTerminated() {
        return onePlayerFolded || streetCheckCnt == 2 ||
                streetCallCnt == 1 || stackPlayers[0] == 0
                || stackPlayers[1] == 0;
    }

    public boolean gameShowdown() {
        return (streetTerminated() && street == Street.RIVER) || stackPlayers[0] == 0
                || stackPlayers[1] == 0;
    }

    public boolean gameTerminated() {
        return onePlayerFolded;
    }

    @Override
    public String toString() {
        return "(OOP is playing:" + oopIsPlaying + ", " + Arrays.toString(stackPlayers) + ")";
    }
}
