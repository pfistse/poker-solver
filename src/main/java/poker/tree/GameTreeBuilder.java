package poker.tree;

import java.util.List;

import poker.SolverConfig;
import poker.training.DiscountedCfrTrainable;
import poker.training.DiscountedCfrTrainable2;
import poker.training.Trainable;
import poker.util.CardUtility;
import poker.util.IsomorphismUtility;
import poker.util.RangeManager;
import poker.util.IsomorphismUtility.IsomorphismData;

public class GameTreeBuilder {

    GameTreeNode root;

    GameTreeBuildSettings settings;
    RangeManager rangeManager;

    GameState initialGS;
    int numPlayersTable;
    int numberNodes;

    int nodeCnt = 0;

    public GameTreeBuilder(GameState initialGS, GameTreeBuildSettings settings, RangeManager rangeManager) {
        this.initialGS = initialGS;
        this.settings = settings;
        this.rangeManager = rangeManager;
        this.numberNodes = 1;
    }

    public void build() {
        // BUILD ROOT NODE

        int plr = initialGS.oopIsPlaying ? 0 : 1;
        int numHands = rangeManager.getNumHands(plr);

        List<ActionState> childActionStates = new ActionState(initialGS, settings).getFollowUpStates();
        Trainable trainable = new DiscountedCfrTrainable(childActionStates.size(), numHands);
        this.root = new ActionNode(initialGS.board, rangeManager, trainable, plr);

        this.root.path = "";
        this.numberNodes++;

        for (ActionState childActionState : childActionStates) {
            buildActionNodes(this.root, childActionState, childActionState.getAction().toString());
        }
    }

    private void buildActionNodes(GameTreeNode parent, ActionState actionState, String edgeLabel) {
        GameState gameState = actionState.getGameState();
        numberNodes++;

        if (gameState.streetTerminated()) {
            // what caused the termination
            if (gameState.gameTerminated()) {
                // everybody except one player folded; append TerminalNode
                buildTerminalNode(parent, gameState, edgeLabel);
            } else if (gameState.gameShowdown()) {
                if (gameState.street == Street.RIVER) {
                    buildShowdownNode(parent, gameState, edgeLabel);
                } else {
                    buildChanceNode(parent, gameState, edgeLabel);
                }
            } else {
                buildChanceNode(parent, gameState, edgeLabel);
            }
            return;
        }

        List<ActionState> childActionStates = actionState.getFollowUpStates();

        int numHands = rangeManager.getNumHands(gameState.oopIsPlaying ? 0 : 1);
        int numActions = childActionStates.size();
        Trainable trainable = new DiscountedCfrTrainable2(numActions, numHands); // TODO

        ActionNode actionNode = new ActionNode(parent, gameState.oopIsPlaying ? 0 : 1,
                trainable);
        actionNode.path = parent.path + edgeLabel;
        actionNode.board = gameState.board;

        parent.addChild(actionNode, edgeLabel);

        for (ActionState childActionState : childActionStates) {
            buildActionNodes(actionNode, childActionState, childActionState.getAction().toString());
        }
    }

    private void buildChanceNode(GameTreeNode parent, GameState gameState, String edgeLabel) {

        IsomorphismData data = null;
        if (SolverConfig.SUIT_ISOMORPHISM) {
            if (gameState.street == Street.FLOP) {
                data = IsomorphismUtility.getTurnIsomorphismData(rangeManager, gameState.flop);
            } else if (gameState.street == Street.TURN) {
                data = IsomorphismUtility.getRiverIsomorphismData(rangeManager, gameState.flop, gameState.turn);
            }
        }

        ChanceNode chanceNode = new ChanceNode(parent, data);
        chanceNode.board = gameState.board;
        chanceNode.path = parent.path + edgeLabel;
        parent.addChild(chanceNode, edgeLabel);

        if (gameState.street == Street.PRE_FLOP) {
        } else if (gameState.street == Street.FLOP || gameState.street == Street.TURN) {

            for (byte card = 0; card < 52; card++) {

                byte suit = (byte) (card & 3);

                if (CardUtility.overlapBoard(gameState.board, card))
                    continue;

                if (SolverConfig.SUIT_ISOMORPHISM && chanceNode.isomorphismData.suits[suit])
                    continue;

                GameState newGameState = gameState.finishStreet(card);

                buildActionNodes(chanceNode, new ActionState(newGameState, settings), CardUtility.cardToString(card));
            }
        }
    }

    private void buildTerminalNode(GameTreeNode parent, GameState gameState, String edgeLabel) {

        TerminalNode terminalNode = new TerminalNode(parent, gameState.board, gameState.oopIsPlaying ? 0 : 1,
                gameState.pot);
        terminalNode.board = gameState.board;
        terminalNode.path = parent.path + edgeLabel;

        parent.addChild(terminalNode, edgeLabel);
    }

    private void buildShowdownNode(GameTreeNode parent, GameState gameState, String edgeLabel) {

        ShowdownNode showdownNode = new ShowdownNode(parent, gameState.board, rangeManager, gameState.pot);
        showdownNode.board = gameState.board;
        showdownNode.path = parent.path + edgeLabel;

        parent.addChild(showdownNode, edgeLabel);
    }

    public int getNumberNodes() {
        return numberNodes;
    }

    public GameTreeNode getRoot() {
        return root;
    }
}
