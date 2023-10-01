package poker.training;

import java.util.Arrays;

public class CfrPlusTrainable implements Trainable {

    int numActions;
    int numHands;

    float[] regrets;
    float[] regretsSum;
    float[] cumulativeRegrets;
    float[] cumulativeRegretsSum;

    public CfrPlusTrainable(int numActions, int numHands) {
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
                    strategy[idx] = regrets[idx] / regretsSum[h];
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

        for (int a = 0; a < numActions; a++) {
            for (int h = 0; h < numHands; h++) {
                idx = a * numHands + h;
                regret = utilityActions[a][h] - utility[h];

                this.regrets[idx] = Math.max(0, this.regrets[idx] + regret);
                this.regretsSum[h] += this.regrets[idx];

                this.cumulativeRegrets[idx] += itr * this.regrets[idx];
                this.cumulativeRegretsSum[h] += this.cumulativeRegrets[idx];
            }
        }
    }
}
