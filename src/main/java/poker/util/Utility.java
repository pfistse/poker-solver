package poker.util;

public class Utility {
    public static float[] normalize(float[] vec) {
        float sum = 0;
        float[] normVec = new float[vec.length];

        for (float e : vec)
            sum += e;

        if (sum == 0)
            return vec;

        for (int i = 0; i < vec.length; i++)
            normVec[i] = vec[i] / sum;

        return normVec;
    }
}
