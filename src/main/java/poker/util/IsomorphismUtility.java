package poker.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.primitives.Bytes;

public class IsomorphismUtility {

    public static class IsomorphismData {
        public boolean[] suits;
        public byte[] cards;
        public int[] references;
        public long[][][] swaps;
    }

    public static IsomorphismData getTurnIsomorphismData(RangeManager rangeManager, long flop) {

        IsomorphismData data = new IsomorphismData();
        data.suits = new boolean[4];

        byte[] rangeSuitIsomorphism = rangeManager.getRangeSuitIsomorphism(rangeManager);

        long[] rankSet = new long[4];
        long mask = 0x1111111111111L;

        rankSet[0] = flop & mask;
        rankSet[1] = (flop >>> 1) & mask;
        rankSet[2] = (flop >>> 2) & mask;
        rankSet[3] = (flop >>> 3) & mask;

        List<Byte> cards = new ArrayList<Byte>();
        List<Integer> references = new ArrayList<Integer>();
        long[][][] swaps = new long[4][2][];

        byte[] isomorphicSuit = new byte[4];
        Arrays.fill(isomorphicSuit, Byte.MIN_VALUE);

        int[] reverseTableTurn = new int[52 * 51 / 2];

        for (byte suit1 = 1; suit1 < 4; suit1++) {
            for (byte suit2 = 0; suit2 < suit1; suit2++) {
                if (rangeSuitIsomorphism[suit1] == rangeSuitIsomorphism[suit2]
                        && rankSet[suit1] == rankSet[suit2]) {

                    data.suits[suit1] = true;
                    isomorphicSuit[suit1] = suit2;
                    fillSwapList(rangeManager, swaps, reverseTableTurn, suit1, suit2);
                    break;
                }
            }
        }

        fillRefList(references, cards, flop, isomorphicSuit);

        data.cards = Bytes.toArray(cards);
        data.references = references.stream().mapToInt(i -> i).toArray();
        data.swaps = swaps;

        return data;
    }

    public static IsomorphismData getRiverIsomorphismData(RangeManager rangeManager, long flop, byte turn) {
        IsomorphismData data = new IsomorphismData();
        data.suits = new boolean[4];

        byte[] rangeSuitIsomorphism = rangeManager.getRangeSuitIsomorphism(rangeManager);

        long[] flopRankSet = new long[4];

        long board = flop | (1L << turn);
        long mask = 0x1111111111111L;

        flopRankSet[0] = flop & mask;
        flopRankSet[1] = (flop >>> 1) & mask;
        flopRankSet[2] = (flop >>> 2) & mask;
        flopRankSet[3] = (flop >>> 3) & mask;

        long[] turnRankSet = flopRankSet.clone();
        turnRankSet[turn & 3] |= 1L << turn;

        List<Byte> cards = new ArrayList<Byte>();
        List<Integer> references = new ArrayList<Integer>();
        long[][][] swaps = new long[4][2][];

        byte[] isomorphicSuit = new byte[4];
        Arrays.fill(isomorphicSuit, Byte.MIN_VALUE);

        int[] reverseTableTurn = new int[52 * 51 / 2];

        for (byte suit1 = 1; suit1 < 4; suit1++) {
            for (byte suit2 = 0; suit2 < suit1; suit2++) {
                if (rangeSuitIsomorphism[suit1] == rangeSuitIsomorphism[suit2]
                        && flopRankSet[suit1] == flopRankSet[suit2]
                        && turnRankSet[suit1] == turnRankSet[suit2]) {
                    data.suits[suit1] = true;
                    isomorphicSuit[suit1] = suit2;
                    fillSwapList(rangeManager, swaps, reverseTableTurn, suit1, suit2);
                    break;
                }
            }
        }

        fillRefList(references, cards, board, isomorphicSuit);

        data.cards = Bytes.toArray(cards);
        data.references = references.stream().mapToInt(i -> i).toArray();
        data.swaps = swaps;

        return data;
    }

    public static void fillSwapList(RangeManager rangeManager, long[][][] swapList, int[] reverseTable,
            byte suit1, byte suit2) {

        for (int plr = 0; plr < 2; plr++) {
            Arrays.fill(reverseTable, Byte.MAX_VALUE);
            List<Long> plrSwapList = new ArrayList<>();

            int plrNumHands = rangeManager.getNumHands(plr);
            byte[] plrHands = rangeManager.getHands(plr);
            for (int h = 0; h < plrNumHands; h++) {
                reverseTable[CardUtility.cardPairToIndex(plrHands[2 * h], plrHands[2 * h + 1])] = h;
            }

            for (int idx = 0; idx < plrNumHands; idx++) {
                byte card1 = CardUtility.swapSuits(plrHands[2 * idx], suit1, suit2);
                byte card2 = CardUtility.swapSuits(plrHands[2 * idx + 1], suit1, suit2);
                int mappedIdx = reverseTable[CardUtility.cardPairToIndex(card1, card2)];

                if (idx < mappedIdx) {
                    plrSwapList.add(((long) idx << 32) | mappedIdx);
                }
            }
            swapList[suit1][plr] = plrSwapList.stream().mapToLong(l -> l).toArray();
        }
    }

    public static void fillRefList(List<Integer> refList, List<Byte> cardList, long board, byte[] isomorphicSuit) {
        boolean pushCard = cardList.isEmpty();
        int counter = 0;
        int[] indices = new int[52];

        for (byte card = 0; card < 52; card++) {
            if (CardUtility.overlapBoard(board, card))
                continue;

            byte suit = (byte) (card & 3);
            byte replacedSuit;

            if ((replacedSuit = isomorphicSuit[suit]) >= 0) {
                byte replacedCard = (byte) (card - suit + replacedSuit);
                refList.add(indices[replacedCard]);

                if (pushCard)
                    cardList.add(card);
            } else {
                indices[card] = counter++;
            }
        }
    }

    public static void applySwap(float[] arr, long[] swaps) {
        for (long swap : swaps) {
            int idx1 = (int) swap;
            int idx2 = (int) (swap >>> 32);

            float tmp = arr[idx1];
            arr[idx1] = arr[idx2];
            arr[idx2] = tmp;
        }
    }

}
