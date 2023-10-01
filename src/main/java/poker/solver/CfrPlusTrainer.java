package poker.solver;

import javafx.application.Application;
import javafx.stage.Stage;
import poker.explorer.GameTreeExplorer;
import poker.tree.GameState;
import poker.tree.GameTreeBuildSettings;
import poker.tree.GameTreeBuilder;
import poker.tree.GameTreeNode;
import poker.tree.Street;
import poker.util.CardUtility;
import poker.util.RangeManager;

public class CfrPlusTrainer extends Application {

    public static void startApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        GameState gameState = new GameState(Street.FLOP,
                CardUtility.boardArrToLong(CardUtility.cardFromString("Qs"),
                        CardUtility.cardFromString("Jh"), CardUtility.cardFromString("2h")),
                180,
                new int[] { 910, 910 });

        GameTreeBuildSettings settings = new GameTreeBuildSettings();
        settings.flopBetSizes = new float[] { 0.52f };
        settings.turnBetSizes = new float[] { 0.55f };
        settings.riverBetSizes = new float[] { 0.70f };

        settings.flopRaiseSizes = new float[] { 0.45f };
        settings.turnRaiseSizes = new float[] { 0.45f };
        settings.riverRaiseSizes = new float[] { 0.45f };

        RangeManager rangeManager = new RangeManager("AA,KK,QQ,JJ,TT,99,88,AK,AQ,AJ,KQ,KJ,KTo,QJ,QT,JT,J9,T9,98", "AA,KK,QQ,JJ,TT,99,88,AK,AQ,AJs,KQ,KJs,KTs,QJ,QTs,JT,J9s,T9,98");

        GameTreeBuilder builder = new GameTreeBuilder(gameState, settings, rangeManager);
        builder.build();
        GameTreeNode root = builder.getRoot();

        System.out.println("nodes " + builder.getNumberNodes());
        System.out.println("Start Training");

        int i = 1;
        int plr = 0;
        for (; i <= 50; i++, plr = i % 2) {
            root.computeCFValuesRecursive(plr, rangeManager.getInitialWeights(1 - plr), i);
            System.out.println("round " + i);
        }
    
        GameTreeExplorer viz = new GameTreeExplorer(root, rangeManager, 0);

        Thread t = new Thread(() -> viz.awaitCommand());
        t.start();
    }
}
