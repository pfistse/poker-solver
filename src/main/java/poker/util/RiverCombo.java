package poker.util;

public class RiverCombo implements Comparable<RiverCombo> {

    public byte card1;
    public byte card2;
    public int rank;
    public int reachProbIdx;

    RiverCombo(byte card1, byte card2, int reachProbIdx, int rank) {
        this.card1 = card1;
        this.card2 = card2;
        this.reachProbIdx = reachProbIdx;
        this.rank = rank;
    }

    @Override
    public int compareTo(RiverCombo o) {
        if (this.rank > o.rank)
            return 1;
        if (this.rank < o.rank)
            return -1;
        return 0;
    }

    @Override
    public String toString() {
        return "(" + CardUtility.cardToString(card1) + ", " + CardUtility.cardToString(card2) + ") [" + reachProbIdx + "]";
    }

}
