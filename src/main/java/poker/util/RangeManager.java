package poker.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RangeManager {

    byte[] oopHands;
    byte[] ipHands;

    float[] oopInitialWeights;
    float[] ipInitialWeights;

    int[] oopHandsCrossRefTable;
    int[] ipHandsCrossRefTable;

    private final Map<Long, RiverCombo[]> riverComboBuffer = new HashMap<>();

    public RangeManager(String oopPreflopRangeStr, String ipPreflopRangeStr) {

        initPreflopRange(oopPreflopRangeStr, 0);
        initPreflopRange(ipPreflopRangeStr, 1);

        initHandsCrossRefTable(0);
        initHandsCrossRefTable(1);
    }

    private void initPreflopRange(String rangeStr, int plr) {

        int cnt = 0;
        String[] handsStr = rangeStr.split(",");

        for (String handStr : handsStr) {
            // pair
            if (handStr.length() == 2 || handStr.charAt(2) == ':') {
                if (handStr.charAt(0) == handStr.charAt(1))
                    cnt += 6; // 4 choose 2
                else
                    cnt += 16; // 4 choose 2
            }
            // suited hand
            else if (handStr.charAt(2) == 's') {
                cnt += 4; // 4 suits
            }
            // off-suit hand
            else if (handStr.charAt(2) == 'o') {
                cnt += 12; // 4 * 3
            }
        }

        byte[] hands = new byte[cnt * 2];
        float[] handWeights = new float[cnt];

        int h = 0;
        for (int i = 0; i < handsStr.length; i++) {

            String comboStr = handsStr[i];
            char firstRank = comboStr.charAt(0);
            char secondRank = comboStr.charAt(1);
            float weight = 1.0F;

            // pair
            if (comboStr.length() == 2 || comboStr.charAt(2) == ':') {

                // weighted
                if (comboStr.length() > 2)
                    weight = Float.valueOf(comboStr.substring(3, comboStr.length()));

                for (int suit1 = 0; suit1 < 4; suit1++) {
                    for (int suit2 = 0; suit2 < 4; suit2++) {

                        if (firstRank == secondRank && suit1 >= suit2)
                            continue;

                        byte card1 = CardUtility.cardFrom(firstRank, suit1);
                        byte card2 = CardUtility.cardFrom(secondRank, suit2);

                        hands[2 * h] = card1;
                        hands[2 * h + 1] = card2;
                        handWeights[h] = weight;
                        h++;
                    }
                }
            }
            // suited hand
            else if (comboStr.charAt(2) == 's') {

                // weighted
                if (comboStr.length() > 4)
                    weight = Float.valueOf(comboStr.substring(4, comboStr.length()));

                for (int suit = 0; suit < 4; suit++) {
                    byte card1 = CardUtility.cardFrom(firstRank, suit);
                    byte card2 = CardUtility.cardFrom(secondRank, suit);

                    hands[2 * h] = card1;
                    hands[2 * h + 1] = card2;
                    handWeights[h] = weight;
                    h++;
                }
            }
            // off-suit hand
            else if (comboStr.charAt(2) == 'o') {

                // weighted
                if (comboStr.length() > 4)
                    weight = Float.valueOf(comboStr.substring(4, comboStr.length()));

                for (int suit1 = 0; suit1 < 4; suit1++) {
                    for (int suit2 = 0; suit2 < 4; suit2++) {
                        if (suit1 == suit2)
                            continue;

                        byte card1 = CardUtility.cardFrom(firstRank, suit1);
                        byte card2 = CardUtility.cardFrom(secondRank, suit2);

                        hands[2 * h] = card1;
                        hands[2 * h + 1] = card2;
                        handWeights[h] = weight;
                        h++;
                    }
                }
            }
        }

        if (plr == 0) {
            oopHands = hands;
            oopInitialWeights = handWeights;
        } else {
            ipHands = hands;
            ipInitialWeights = handWeights;
        }
    }

    public int getNumHands(int plr) {
        return (plr == 0) ? oopInitialWeights.length : ipInitialWeights.length;
    }

    public byte[] getHands(int plr) {
        return (plr == 0) ? oopHands : ipHands;
    }

    public float[] getInitialWeights(int plr) {
        return (plr == 0) ? oopInitialWeights : ipInitialWeights;
    }

    public boolean isSuitIsomorphic(int plr, byte suit1, byte suit2) {
        return true;
    }

    private byte[] rangeSuitIsomorphism;

    public byte[] getRangeSuitIsomorphism(RangeManager rangeManager) {

        if (rangeSuitIsomorphism != null)
            return rangeSuitIsomorphism;

        byte nextIdx = 1;
        byte[] rangeSuitIsomorphism = new byte[4];
        outer: for (byte suit2 = 1; suit2 < 4; suit2++) {
            for (byte suit1 = 0; suit1 < suit2; suit1++) {
                if (rangeManager.isSuitIsomorphic(0, suit1, suit2)
                        && rangeManager.isSuitIsomorphic(1, suit1, suit2)) {
                    rangeSuitIsomorphism[suit2] = rangeSuitIsomorphism[suit1];
                    continue outer;
                }
            }
            rangeSuitIsomorphism[suit2] = nextIdx++;
        }
        return this.rangeSuitIsomorphism = rangeSuitIsomorphism;
    }

    public RiverCombo[] getRiverCombos(long board, int plr) {

        // check whether combo is already in buffer
        long key = (board << 1) | plr;
        if (riverComboBuffer.containsKey(key))
            return riverComboBuffer.get(key);

        byte[] hands = plr == 0 ? oopHands : ipHands;
        float[] handWeights = plr == 0 ? oopInitialWeights : ipInitialWeights;

        ArrayList<RiverCombo> riverCombos = new ArrayList<>();

        for (int h = 0; h < handWeights.length; h++) {
            byte card1 = hands[2 * h];
            byte card2 = hands[2 * h + 1];

            if (CardUtility.overlapBoard(board, card1, card2))
                continue;

            int value = PokerUtil.getHandValue(card1, card2, board);
            RiverCombo riverCombo = new RiverCombo(card1, card2, h, value);
            riverCombos.add(riverCombo);
        }

        Collections.sort(riverCombos);

        RiverCombo[] riverComboArr = new RiverCombo[riverCombos.size()];
        riverComboBuffer.put(key, riverCombos.toArray(riverComboArr));
        return riverComboArr;
    }

    public void initHandsCrossRefTable(int plr) {

        int plrNumHands = getNumHands(plr);
        int oppNumHands = getNumHands(1 - plr);

        int[] handCrossRefTable = new int[plrNumHands];

        byte[] plrHands = plr == 0 ? oopHands : ipHands;
        byte[] oppHands = plr == 0 ? ipHands : oopHands;

        for (int ph = 0; ph < plrNumHands; ph++) {

            byte card1 = plrHands[2 * ph];
            byte card2 = plrHands[2 * ph + 1];

            int oh = 0;
            while (oh < oppNumHands
                    && (oppHands[2 * oh] != card1 || oppHands[2 * oh + 1] != card2))
                oh++;

            if (oh != oppNumHands)
                handCrossRefTable[ph] = oh;
            else
                handCrossRefTable[ph] = -1;
        }

        if (plr == 0)
            this.oopHandsCrossRefTable = handCrossRefTable;
        else
            this.ipHandsCrossRefTable = handCrossRefTable;
    }

    public int[] getHandsCrossRefTable(int plr) {
        return plr == 0 ? oopHandsCrossRefTable : ipHandsCrossRefTable;
    }
}