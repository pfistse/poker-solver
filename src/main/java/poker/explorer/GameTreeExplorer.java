package poker.explorer;

import poker.training.Trainable;
import poker.tree.ActionNode;
import poker.tree.ChanceNode;
import poker.tree.GameTreeNode;
import poker.tree.ShowdownNode;
import poker.tree.TerminalNode;
import poker.util.CardUtility;
import poker.util.RangeManager;

import java.io.Console;
import java.util.Arrays;
import java.util.List;

public class GameTreeExplorer {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static final Console console = System.console();
    private GameTreeNode currentPos;
    private RangeManager rangeManager;
    private RangeMonitor strategyView;

    public GameTreeExplorer(GameTreeNode root, RangeManager rangeManager, int plr) {
        this.currentPos = root;
        this.rangeManager = rangeManager;
        this.strategyView = new RangeMonitor();
    }

    public void awaitCommand() {
        displayNode(currentPos);

        await: while (true) {
            try {
                String in = console.readLine(ANSI_WHITE);
                String[] args = in.split(" ");

                List<GameTreeNode> children = currentPos.getChildren();
                List<String> edgeLabels = currentPos.getEdgeLabel();

                if (args[0].equals("expand") || args[0].equals("e")) {

                    String edgeLabel = (args[1] + (args.length >= 3 ? " " + args[2] : ""));

                    int childIdx;
                    if ((childIdx = edgeLabels.indexOf(edgeLabel)) >= 0) {
                        currentPos = children.get(childIdx);
                        displayNode(currentPos);
                    } else {
                        System.out.println(ANSI_RED + "Unable to find node to expand.");
                    }

                    continue await;

                } else if (args[0].equals("contract") || args[0].equals("con") || args[0].equals("c")) {

                    if (this.currentPos.getParent() != null) {
                        this.currentPos = this.currentPos.getParent();
                        displayNode(this.currentPos);
                    } else {
                        System.out.println(ANSI_RED + "Root node cannot be contracted.");
                    }

                    continue await;
                }

            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            ;

            System.out.println(ANSI_WHITE + "Please chose one of the following valid commands: ");
            System.out.println("expand|e check|call|fold");
            System.out.println("expand|e raise|bet <amount>");
            System.out.println("contract|con|c");
            System.out.println();
        }

    }

    private void displayNode(GameTreeNode node) {
        List<GameTreeNode> children = node.getChildren();
        List<String> edgeLabels = node.getEdgeLabel();

        System.out.print(ANSI_BLUE);

        if (node instanceof ChanceNode) {

            String board = CardUtility.boardToString(node.board);
            System.out.println("Current board: " + board + ". Choose a card that has not been listed.");

        } else if (node instanceof ActionNode) {

            ActionNode actionNode = (ActionNode) node;
            Trainable trainable = actionNode.getTrainable();

            int nodePlr = actionNode.getNodePlayer();
            int numActions = actionNode.getChildren().size();

            byte[] nodePlrHands = rangeManager.getHands(nodePlr);
            float[] oopReachProbs = new float[rangeManager.getNumHands(0)];

            float[] optimalStrategy = trainable.getAverageStrategy();
            for (int a = 0; a < numActions; a++) {
                GameTreeNode child = children.get(a);

                String debugInfo = Arrays.toString(child.equity);

                String type = "";
                if (child instanceof ChanceNode) {
                    type = "(chance) ";
                } else if (child instanceof TerminalNode) {
                    type = "(terminal) ";
                } else if (child instanceof ShowdownNode) {
                    type = "(showdown) ";
                }

                System.out.print("\n" + edgeLabels.get(a) + " " + type + "\n" + debugInfo + "\n");
            }

            this.strategyView.displayStrategy(optimalStrategy, oopReachProbs,
                    nodePlrHands, numActions);

        } else if (node instanceof ShowdownNode) {
        } else if (node instanceof TerminalNode) {

            TerminalNode terminalNode = (TerminalNode) node;
            System.out.println(terminalNode);
        }

        // byte[] plrHands = rangeManager.getHands(plr);
        // this.strategyView.displayEquity(node.equity, plrHands);
        // System.out.println(Arrays.toString(node.equity));
    }
}
