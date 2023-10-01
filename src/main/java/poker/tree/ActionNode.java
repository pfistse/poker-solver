package poker.tree;

import poker.SolverConfig;
import poker.training.Trainable;
import poker.util.RangeManager;

public class ActionNode extends GameTreeNode {

    Trainable trainable;
    int nodePlr;

    public static int actionNodeCnt = 0;

    public ActionNode(GameTreeNode parent, int nodePlr, Trainable trainable) {
        super(parent);
        this.trainable = trainable;
        this.nodePlr = nodePlr;
        this.path = parent.path;
        actionNodeCnt++;
    }

    public ActionNode(long board, RangeManager rangeManager, Trainable trainable, int nodePlr) {
        super(board, rangeManager);
        this.trainable = trainable;
        this.nodePlr = nodePlr;
        actionNodeCnt++;
    }

    @Override
    public float[] computeCFValuesRecursive(int plr, float[] oppReachProbs, int itr) {
        int numActions = children.size();

        int plrNumHands = rangeManager.getNumHands(plr);
        int oppNumHands = rangeManager.getNumHands(1 - plr);

        // calculate counter factual values
        float[] newPlrCfV = new float[plrNumHands];
        float[][] plrCfvaluesActions = new float[numActions][];
        float[] strategy = trainable.getStrategy();

        for (int a = 0; a < numActions; a++) {

            float[] newOppHandWeight = oppReachProbs;

            // node player is opponent
            if (nodePlr != plr) {
                newOppHandWeight = new float[oppNumHands];

                for (int h = 0; h < oppNumHands; h++) {
                    newOppHandWeight[h] = oppReachProbs[h] * strategy[a * oppNumHands + h];
                }
            }

            plrCfvaluesActions[a] = children.get(a).computeCFValuesRecursive(plr, newOppHandWeight, itr);

            if (nodePlr == plr) {
                for (int h = 0; h < plrNumHands; h++) {
                    newPlrCfV[h] += strategy[a * plrNumHands + h] * plrCfvaluesActions[a][h];
                }
            } else {
                for (int h = 0; h < plrNumHands; h++) {
                    newPlrCfV[h] += plrCfvaluesActions[a][h];
                }
            }

        }

        // update regrets
        if (nodePlr == plr)
            trainable.updateRegrets(newPlrCfV, plrCfvaluesActions, itr, path);

        if (SolverConfig.STORE_EV) {
            float[] realizationProb = computeRealizationProbability(plr, oppReachProbs);
            this.expectedValue = new float[newPlrCfV.length];

            for (int i = 0; i < this.expectedValue.length; i++)
                this.expectedValue[i] = newPlrCfV[i] / realizationProb[i];
        }

        return newPlrCfV;
    }

    public Trainable getTrainable() {
        return trainable;
    }

    public int getNodePlayer() {
        return nodePlr;
    }

    @Override
    public String toString() {
        return "nodePlr=" + nodePlr + ", strategy=" + trainable.toString();
    }

    @Override
    public float[] computeEquityRecursive(int plr, float[] oppReachProbs) {
        int numActions = children.size();

        int plrNumHands = rangeManager.getNumHands(plr);
        int oppNumHands = rangeManager.getNumHands(1 - plr);
        int nodePlrNumHands = rangeManager.getNumHands(nodePlr);

        // calculate counter factual values
        float[] newPlrEquity = new float[plrNumHands];
        float[][] plrEquityActions = new float[numActions][];
        float[] strategy = trainable.getStrategy();

        for (int a = 0; a < numActions; a++) {
            if (children.get(a) instanceof TerminalNode) {
                float[] showdownStrategy = new float[numActions * nodePlrNumHands];
                for (int a2 = 0; a2 < numActions; a2++) {
                    if (a == a2)
                        continue;
                    for (int h = 0; h < nodePlrNumHands; h++) {
                        showdownStrategy[a2 * nodePlrNumHands + h] = strategy[a2 * nodePlrNumHands + h]
                                / (1 - strategy[a * nodePlrNumHands + h]);
                    }
                }
                strategy = showdownStrategy;
                continue;
            }
        }

        // TODO performance: change loop hierarchy
        for (int a = 0; a < numActions; a++) {

            float[] newOppHandWeight = oppReachProbs;

            // node player is opponent
            if (nodePlr != plr) {
                newOppHandWeight = new float[oppNumHands];

                for (int h = 0; h < oppNumHands; h++) {
                    newOppHandWeight[h] = oppReachProbs[h] * strategy[a * oppNumHands + h];
                }
            }

            plrEquityActions[a] = children.get(a).computeEquityRecursive(plr, newOppHandWeight);

            if (nodePlr == plr) {
                for (int h = 0; h < plrNumHands; h++) {
                    if (!Float.isNaN(plrEquityActions[a][h]))
                        newPlrEquity[h] += strategy[a * plrNumHands + h] * plrEquityActions[a][h];
                }
            } else {
                for (int h = 0; h < plrNumHands; h++) {
                    if (!Float.isNaN(plrEquityActions[a][h]))
                        newPlrEquity[h] += plrEquityActions[a][h];
                }
            }

        }

        float[] realizationProb = computeRealizationProbability(plr, oppReachProbs);
        this.equity = new float[newPlrEquity.length];
        for (int h = 0; h < equity.length; h++) {
            this.equity[h] = newPlrEquity[h] / realizationProb[h];
        }

        return newPlrEquity;
    }

}
