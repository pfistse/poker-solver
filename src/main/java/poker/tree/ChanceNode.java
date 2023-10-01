package poker.tree;

import poker.SolverConfig;
import poker.util.IsomorphismUtility;
import poker.util.IsomorphismUtility.IsomorphismData;

public class ChanceNode extends GameTreeNode {

    int[] p1PreflopComboCrossRefTable;
    int[] p2PreflopComboCrossRefTable;

    public IsomorphismData isomorphismData;

    public ChanceNode(GameTreeNode parent, IsomorphismData isomorphismData) {
        super(parent);
        this.isomorphismData = isomorphismData;
    }

    @Override
    public float[] computeCFValuesRecursive(int plr, float[] oppReachProbs, int itr) {

        int plrNumHands = rangeManager.getNumHands(plr);
        int oppNumHands = rangeManager.getNumHands(1 - plr);

        float[] newPlrCFV = new float[plrNumHands];
        float[] newOppReachProbs = new float[oppNumHands];

        float[][] plrCFVChildren = new float[children.size()][];
        for (int child = 0; child < children.size(); child++) {

            int numIsomorphicDeals = 0;
            if (SolverConfig.SUIT_ISOMORPHISM) {
                numIsomorphicDeals = isomorphismData.cards.length;
            }

            // minus 4 to account for hole cards of both players
            int numPossibleDeals = children.size() + numIsomorphicDeals - 4;

            for (int hand = 0; hand < oppNumHands; hand++)
                newOppReachProbs[hand] = oppReachProbs[hand] / numPossibleDeals;

            plrCFVChildren[child] = children.get(child).computeCFValuesRecursive(plr, newOppReachProbs, itr);

            for (int h = 0; h < plrCFVChildren[child].length; h++)
                newPlrCFV[h] += plrCFVChildren[child][h];
        }

        // TODO
        if (SolverConfig.SUIT_ISOMORPHISM) {
            for (int i = 0; i < isomorphismData.references.length; i++) {
                byte suit = (byte) (isomorphismData.cards[i] & 3);
                int ref = isomorphismData.references[i];

                IsomorphismUtility.applySwap(plrCFVChildren[ref], isomorphismData.swaps[suit][plr]);
                for (int h = 0; h < plrCFVChildren[ref].length; h++) {
                    newPlrCFV[h] += plrCFVChildren[ref][h];
                }
                IsomorphismUtility.applySwap(plrCFVChildren[ref], isomorphismData.swaps[suit][plr]);
            }
        }

        if (SolverConfig.STORE_EV) {
            float[] realizationProb = computeRealizationProbability(plr, oppReachProbs);
            this.expectedValue = new float[newPlrCFV.length];

            for (int i = 0; i < this.expectedValue.length; i++)
                this.expectedValue[i] = newPlrCFV[i] / realizationProb[i];
        }

        return newPlrCFV;
    }

    @Override
    public float[] computeEquityRecursive(int plr, float[] oppReachProbs) {
        int plrNumHands = rangeManager.getNumHands(plr);
        int oppNumHands = rangeManager.getNumHands(1 - plr);

        float[] newPlrEquity = new float[plrNumHands];
        float[] newOppReachProbs = new float[oppNumHands];

        float[][] plrEquityChildren = new float[children.size()][];
        for (int child = 0; child < children.size(); child++) {

            int numIsomorphicDeals = 0;
            if (SolverConfig.SUIT_ISOMORPHISM) {
                numIsomorphicDeals = isomorphismData.cards.length;
            }

            // minus 4 to account for hole cards of both players
            int numPossibleDeals = children.size() + numIsomorphicDeals - 4;

            for (int hand = 0; hand < oppNumHands; hand++)
                newOppReachProbs[hand] = oppReachProbs[hand] / numPossibleDeals;

            plrEquityChildren[child] = children.get(child).computeEquityRecursive(plr, newOppReachProbs);

            for (int h = 0; h < plrEquityChildren[child].length; h++)
                if (!Float.isNaN(plrEquityChildren[child][h]))
                    newPlrEquity[h] += plrEquityChildren[child][h];
        }

        if (SolverConfig.SUIT_ISOMORPHISM) {
            for (int i = 0; i < isomorphismData.references.length; i++) {
                byte suit = (byte) (isomorphismData.cards[i] & 3);
                int ref = isomorphismData.references[i];

                IsomorphismUtility.applySwap(plrEquityChildren[ref], isomorphismData.swaps[suit][plr]);
                for (int h = 0; h < plrEquityChildren[ref].length; h++) {
                    newPlrEquity[h] += plrEquityChildren[ref][h];
                }
                IsomorphismUtility.applySwap(plrEquityChildren[ref], isomorphismData.swaps[suit][plr]);
            }
        }

        float[] realizationProb = computeRealizationProbability(plr, oppReachProbs);
        this.equity = new float[newPlrEquity.length];
        for (int i = 0; i < equity.length; i++)
            this.equity[i] = newPlrEquity[i] / realizationProb[i];

        return newPlrEquity;
    }
}
