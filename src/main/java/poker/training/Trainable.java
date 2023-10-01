package poker.training;

public interface Trainable {
    
    public float[] getStrategy();

    public float[] getAverageStrategy();

    public void updateRegrets(float[] utility,float[][] utilityActions, int itr, String path);
}
