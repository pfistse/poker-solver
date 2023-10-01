package poker.training;

import java.util.Arrays;

public class DiscountedCfrTrainable implements Trainable {

    float alpha = 1.5f;
    float beta = 0.5f;
    float gamma = 2;
    float theta = 0.9f;

    int numActions;
    int numHands;

    float[] regrets;
    float[] regretsSum;
    float[] cumulativeRegrets;
    float[] cumulativeRegretsSum;

    public DiscountedCfrTrainable(int numActions, int numHands) {
        this.numActions = numActions;
        this.numHands = numHands;

        this.regrets = new float[numActions * numHands];
        this.regretsSum = new float[numHands];
        this.cumulativeRegrets = new float[numActions * numHands];
        this.cumulativeRegretsSum = new float[numHands];
    }

    // called each iteration, therefore buffering
    public float[] getStrategy() {
        float[] strategy = new float[numActions * numHands];
        int idx;

        for (int a = 0; a < numActions; a++) {
            for (int h = 0; h < numHands; h++) {
                idx = a * numHands + h;

                if (regretsSum[h] > 0)
                    strategy[idx] = Math.max(0, regrets[idx]) / regretsSum[h];
                else {
                    strategy[idx] = 1F / numActions;
                }
            }
        }
        return strategy;
    }

    public float[] getAverageStrategy() {
        float[] strategy = new float[numActions * numHands];
        int idx;

        for (int a = 0; a < numActions; a++) {
            for (int h = 0; h < numHands; h++) {
                idx = a * numHands + h;

                if (cumulativeRegretsSum[h] > 0)
                    strategy[idx] = cumulativeRegrets[idx] / cumulativeRegretsSum[h];
                else {
                    strategy[idx] = 1F / numActions;
                }
            }
        }
        return strategy;
    }

    @Override
    public void updateRegrets(float[] utility, float[][] utilityActions, int itr, String path) {

        int idx = 0;
        float regret = 0;

        Arrays.fill(this.regretsSum, 0);
        Arrays.fill(this.cumulativeRegretsSum, 0);

        float alphaD = (float) Math.pow((double) itr, this.alpha);
        alphaD = alphaD / (1 + alphaD);

        for (int a = 0; a < numActions; a++) {
            for (int h = 0; h < numHands; h++) {
                idx = a * numHands + h;
                regret = utilityActions[a][h] - utility[h];

                this.regrets[idx] += regret;
                this.regrets[idx] *= this.regrets[idx] > 0 ? alphaD : this.beta;

                this.regretsSum[h] += Math.max(0, this.regrets[idx]);

                // this.cumulativeRegrets[idx] += itr * this.regrets[idx];
                // this.cumulativeRegretsSum[h] += this.cumulativeRegrets[idx];
            }
        }

        float[] currentStrategy = getStrategy();
        float strategyCoef = (float) Math.pow(((float) itr / (itr + 1)), this.gamma);

        for (int a = 0; a < numActions; a++) {
            for (int h = 0; h < numHands; h++) {
                idx = a * numHands + h;
                this.cumulativeRegrets[idx] *= this.theta;
                this.cumulativeRegrets[idx] += currentStrategy[idx] * strategyCoef; // TODO multiply with reach probs
                this.cumulativeRegretsSum[h] += this.cumulativeRegrets[idx];
            }
        }
    }
}
