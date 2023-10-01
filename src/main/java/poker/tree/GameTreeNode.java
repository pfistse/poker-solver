package poker.tree;

import java.util.ArrayList;
import java.util.List;

import poker.util.CardUtility;
import poker.util.RangeManager;

public abstract class GameTreeNode {
    GameTreeNode parent;
    RangeManager rangeManager;

    List<GameTreeNode> children;
    List<String> edgeLabel;

    public float[] expectedValue;
    public float[] equity;

    public long board;

    String path;

    public GameTreeNode(GameTreeNode parent) {
        this.parent = parent;
        this.rangeManager = parent.rangeManager;
        this.board = parent.board;

        this.children = new ArrayList<GameTreeNode>();
        this.edgeLabel = new ArrayList<String>();
    }

    public GameTreeNode(long board, RangeManager rangeManager) {
        this.parent = null;
        this.rangeManager = rangeManager;
        this.board = board;

        this.children = new ArrayList<GameTreeNode>();
        this.edgeLabel = new ArrayList<String>();
    }

    public void addChild(GameTreeNode child, String edgeLabel) {
        this.children.add(child);
        this.edgeLabel.add(edgeLabel);
    }


    public abstract float[] computeCFValuesRecursive(int plr, float[] oppReachProbs, int itr);

    public abstract float[] computeEquityRecursive(int plr, float[] oppReachProbs);

    public float[] computeRealizationProbability(int plr, float[] oppWeights) {

        int plrNumHands = rangeManager.getNumHands(plr);
        int oppNumHands = rangeManager.getNumHands(1 - plr);
        byte[] plrHands = rangeManager.getHands(plr);
        byte[] oppHands = rangeManager.getHands(1 - plr);
        int[] plrHandsCrossRefTable = rangeManager.getHandsCrossRefTable(plr);

        float[] normalizationSum = new float[plrNumHands];

        float oppWeightSum = 0;
        float[] minusWeight = new float[52];

        for (int h = 0; h < oppNumHands; h++) {
            if(oppWeights[h] == 0)
                continue;

            byte card1 = oppHands[2 * h];
            byte card2 = oppHands[2 * h + 1];

            if (CardUtility.overlapBoard(board, card1, card2))
                continue;

                minusWeight[card1] += oppWeights[h];
                minusWeight[card2] += oppWeights[h];
                oppWeightSum += oppWeights[h];
        }

        if (oppWeightSum == 0)
            return normalizationSum;

        for (int h = 0; h < plrNumHands; h++) {
            byte card1 = plrHands[2 * h];
            byte card2 = plrHands[2 * h + 1];

            if (CardUtility.overlapBoard(board, card1, card2))
                continue;

            float sameHandOppWeight = 0;
            if (plrHandsCrossRefTable[h] >= 0)
                sameHandOppWeight = oppWeights[plrHandsCrossRefTable[h]];

            normalizationSum[h] = (oppWeightSum
                    - minusWeight[card1]
                    - minusWeight[card2]
                    + sameHandOppWeight);

        }

        return normalizationSum;
    }

    public List<GameTreeNode> getChildren() {
        return this.children;
    }

    public GameTreeNode getParent() {
        return this.parent;
    }

    public List<String> getEdgeLabel() {
        return edgeLabel;
    }
}
