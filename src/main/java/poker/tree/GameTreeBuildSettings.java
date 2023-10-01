package poker.tree;

public class GameTreeBuildSettings {

    public float[] flopBetSizes;
    public float[] turnBetSizes;
    public float[] riverBetSizes;

    public float[] flopRaiseSizes;
    public float[] turnRaiseSizes;
    public float[] riverRaiseSizes;

    public GameTreeBuildSettings() {
    }

    public float[] getBetSizes(Street street) {
        return switch (street) {
            case FLOP -> flopBetSizes;
            case TURN -> turnBetSizes;
            case RIVER -> riverBetSizes;
            default -> throw new RuntimeException("no bet sizes for streets other than flop/turn/river");
        };
    }

    public float[] getRaiseSizes(Street street) {
        return switch (street) {
            case FLOP -> flopRaiseSizes;
            case TURN -> turnRaiseSizes;
            case RIVER -> riverRaiseSizes;
            default -> throw new RuntimeException("no raise sizes for streets other than flop/turn/river");
        };
    }
}
