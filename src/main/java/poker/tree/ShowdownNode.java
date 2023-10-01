package poker.tree;

import java.util.Arrays;

import poker.SolverConfig;
import poker.util.CardUtility;
import poker.util.RangeManager;
import poker.util.RiverCombo;

public class ShowdownNode extends GameTreeNode {

    int pot;

    public ShowdownNode(GameTreeNode parent, long board, RangeManager rangeManager, int pot) {
        super(parent);
        this.board = board;
        this.pot = pot;
    }

    public ShowdownNode(long board, RangeManager rangeManager, int pot) {
        super(board, rangeManager);
        this.pot = pot;
    }

    public float[] computeCFValuesRecursive(int plr, float[] oppReachProbs, int itr) {

        int plrNumHands = rangeManager.getNumHands(plr);
        float[] plrCfV = new float[plrNumHands];

        RiverCombo[] plrRiverCombos = rangeManager.getRiverCombos(board, plr);
        RiverCombo[] oppRiverCombos = rangeManager.getRiverCombos(board, 1 - plr);

        float halfPot = 0.5F * pot;
        float winAmount = halfPot;
        float loseAmount = halfPot;

        float oppWeight = 0;
        float oppWeightSum = 0;
        float[] minusWeight = new float[52];

        // counterfactual win probability
        int j = 0;
        for (int i = 0; i < plrRiverCombos.length; i++) {
            RiverCombo plrCombo = plrRiverCombos[i];

            if (CardUtility.overlapBoard(board, plrCombo.card1, plrCombo.card2))
                continue;

            while (j < oppRiverCombos.length && plrCombo.rank > oppRiverCombos[j].rank) {
                RiverCombo oppCombo = oppRiverCombos[j];
                oppWeight = oppReachProbs[oppCombo.reachProbIdx];

                if (oppWeight != 0
                        && !CardUtility.overlapBoard(board, oppCombo.card1, oppCombo.card2)) {
                    oppWeightSum += oppWeight;
                    minusWeight[oppCombo.card1] += oppWeight;
                    minusWeight[oppCombo.card2] += oppWeight;
                }
                j++;
            }

            plrCfV[plrCombo.reachProbIdx] = (oppWeightSum
                    - minusWeight[plrCombo.card1]
                    - minusWeight[plrCombo.card2])
                    * winAmount;
        }

        oppWeightSum = 0;
        Arrays.fill(minusWeight, 0);

        // counter factual lose probability
        j = oppRiverCombos.length - 1;
        for (int i = plrRiverCombos.length - 1; i >= 0; i--) {
            RiverCombo plrCombo = plrRiverCombos[i];

            if (CardUtility.overlapBoard(board, plrCombo.card1, plrCombo.card2))
                continue;

            while (j >= 0 && plrCombo.rank < oppRiverCombos[j].rank) {
                RiverCombo oppCombo = oppRiverCombos[j];
                oppWeight = oppReachProbs[oppCombo.reachProbIdx];

                if (oppWeight != 0
                        && !CardUtility.overlapBoard(board, oppCombo.card1, oppCombo.card2)) {
                    oppWeightSum += oppWeight;
                    minusWeight[oppCombo.card1] += oppWeight;
                    minusWeight[oppCombo.card2] += oppWeight;
                }
                j--;
            }

            plrCfV[plrCombo.reachProbIdx] -= (oppWeightSum
                    - minusWeight[plrCombo.card1]
                    - minusWeight[plrCombo.card2])
                    * loseAmount;
        }

        if (SolverConfig.STORE_EV) {
            float[] realizationProb = computeRealizationProbability(plr, oppReachProbs);
            this.expectedValue = new float[plrCfV.length];

            for (int i = 0; i < this.expectedValue.length; i++)
                this.expectedValue[i] = plrCfV[i] / realizationProb[i];
        }

        return plrCfV;
    }

    @Override
    public float[] computeEquityRecursive(int plr, float[] oppReachProbs) {
        int plrNumHands = rangeManager.getNumHands(plr);

        float[] plrEquity = new float[plrNumHands];

        RiverCombo[] plrRiverCombos = rangeManager.getRiverCombos(board, plr);
        RiverCombo[] oppRiverCombos = rangeManager.getRiverCombos(board, 1 - plr);

        float oppWeight = 0;
        float oppWeightSum = 0;
        float[] minusWeight = new float[52];

        int tieRank = Integer.MAX_VALUE;

        float oppTieWeightSum = 0;
        float[] minusTieWeight = new float[52];

        float sameHandWeight[] = new float[52 * 51 / 2];
        float tieSameHandWeight[] = new float[52 * 51 / 2];

        // counterfactual win probability
        int j = 0;
        for (int i = 0; i < plrRiverCombos.length; i++) {
            RiverCombo plrCombo = plrRiverCombos[i];
            int plrCardPairIdx = CardUtility.cardPairToIndex(plrCombo.card1, plrCombo.card2);

            if (CardUtility.overlapBoard(board, plrCombo.card1, plrCombo.card2))
                continue;

            if (plrCombo.rank > tieRank) {
                oppWeightSum += oppTieWeightSum;
                oppTieWeightSum = 0;

                for (int k = 0; k < minusWeight.length; k++) {
                    minusWeight[k] += minusTieWeight[k];
                    minusTieWeight[k] = 0;
                }

                for (int k = 0; k < sameHandWeight.length; k++) {
                    sameHandWeight[k] = tieSameHandWeight[k];
                    tieSameHandWeight[k] = 0;
                }

                tieRank = Integer.MAX_VALUE;
            }

            while (j < oppRiverCombos.length && plrCombo.rank > oppRiverCombos[j].rank) {
                RiverCombo oppCombo = oppRiverCombos[j];
                oppWeight = oppReachProbs[oppCombo.reachProbIdx];

                if (oppWeight != 0
                        && !CardUtility.overlapBoard(board, oppCombo.card1, oppCombo.card2)) {

                    oppWeightSum += oppWeight;
                    minusWeight[oppCombo.card1] += oppWeight;
                    minusWeight[oppCombo.card2] += oppWeight;
                    sameHandWeight[CardUtility.cardPairToIndex(oppCombo.card1, oppCombo.card2)] = oppWeight;
                }
                j++;
            }

            while (j < oppRiverCombos.length && plrCombo.rank == oppRiverCombos[j].rank) {
                RiverCombo oppCombo = oppRiverCombos[j];
                oppWeight = oppReachProbs[oppCombo.reachProbIdx];

                if (oppWeight != 0
                        && !CardUtility.overlapBoard(board, oppCombo.card1, oppCombo.card2)) {

                    tieRank = oppCombo.rank;
                    oppTieWeightSum += oppWeight;
                    minusTieWeight[oppCombo.card1] += oppWeight;
                    minusTieWeight[oppCombo.card2] += oppWeight;
                    tieSameHandWeight[CardUtility.cardPairToIndex(oppCombo.card1, oppCombo.card2)] = oppWeight;
                }
                j++;
            }

            plrEquity[plrCombo.reachProbIdx] = (oppWeightSum
                    - minusWeight[plrCombo.card1]
                    - minusWeight[plrCombo.card2]
                    + sameHandWeight[plrCardPairIdx])
                    + 0.5F * (oppTieWeightSum
                            - minusTieWeight[plrCombo.card1]
                            - minusTieWeight[plrCombo.card2]
                            + tieSameHandWeight[plrCardPairIdx]);
        }

        float[] realizationProb = computeRealizationProbability(plr, oppReachProbs);
        this.equity = new float[plrEquity.length];
        for (int i = 0; i < equity.length; i++)
            this.equity[i] = plrEquity[i] / realizationProb[i];

        return plrEquity;
    }
}