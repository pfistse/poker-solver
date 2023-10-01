package poker.util;

import java.util.*;

public class PokerUtil {

    // CONSTANTS
    public static final int NUMBER_POKER_HANDS = 133784560;

    public static final int PAIR = 256;
    public static final int TWO_PAIR = 512;
    public static final int THREE_OF_A_KIND = 8192;
    public static final int STRAIGHT = 16384;
    public static final int FLUSH = 32768;
    public static final int FULL_HOUSE = 65536;
    public static final int FOUR_OF_A_KIND = 131072;
    public static final int STRAIGHT_FLUSH = 262144;
    public static final int ROYAL_FLUSH = 524288;

    /**
     * Returns a value depending on the resulting poker hand.
     * The higher the value, the better the hand.
     *
     * Rules for calculation:
     * Royal Flush: 524288
     * Straight Flush: 262144 + highestCard
     * Four of a kind: 131072 + 16 * cardValue + kicker (kicker is only added when
     * four of a kind is completely on the board)
     * Full House: 65536 + 16 * tripleValue + pairValue
     * Flush: 32768 + highestCard
     * Straight: 16384 + highestCard
     * Three of a kind: 8192 + 16 * tripleValue + kicker
     * Two Pair: 512 + 256 * highPairValue + 16 * lowPairValue + kicker
     * Pair: 256 + 16 * pairValue + kicker
     * High card: 16 * cardValue + kicker
     *
     * @param firstCard
     * @param secondCard
     * @param board
     * @return
     */
    public static int getHandValue(byte firstCard, byte secondCard, long board) {
        byte[] boardArr = CardUtility.boardLongToArr(board);
        int[] cards = { firstCard, secondCard, boardArr[0], boardArr[1], boardArr[2], boardArr[3], boardArr[4] };
        int[] values = new int[7]; // sorted array of only the card values
        int firstValue = (firstCard >> 2) + 2;
        int secondValue = (secondCard >> 2) + 2;

        for (int i = 0; i < 7; i++) {
            values[i] = (cards[i] >> 2) + 2;
        }

        // sort values for easier straight detection
        sortBoth(values, cards);

        // straight?
        int highCard = 0;
        int highestStreak = 0;

        // We can skip the straight check if values 2 and 3 are more than 1 apart
        if (values[3] - values[2] <= 1) {
            int streak = 1;
            int prev = values[0];

            if (values[6] == 14 && values[0] == 2)
                streak++;

            for (int i = 1; i < 7; i++) {
                if (values[i] == prev)
                    continue;
                if (values[i] == prev + 1) {
                    streak++;
                    if (streak > highestStreak)
                        highestStreak = streak;
                    highCard = values[i];
                } else {
                    if (i >= 3)
                        break;
                    streak = 1;
                }

                prev = values[i];
            }
        }

        int[] colors = new int[4];
        for (int c = 0; c < 7; c++) {
            colors[cards[c] & 0b11]++;
        }

        if (highestStreak >= 5) {
            // check for flush (this doesn't have to be super efficient since its very rare
            for (int i = 6; i >= 0; i--) {
                if (values[i] == highCard) {
                    // values[i] is part of the flush

                    int color = -1;
                    for (int c = 0; c < 4; c++) {
                        if (colors[c] >= 5)
                            color = c;
                    }

                    if (color == -1)
                        break;

                    List<Integer> l = new ArrayList<>();
                    for (int x = 0; x < 7; x++) {
                        if ((cards[x] & 0b11) == color) {
                            l.add(cards[x] & 15);
                        }
                    }

                    int prev = l.get(0);
                    int streak = 1;
                    highestStreak = 0;

                    if (l.get(l.size() - 1) == 14 && l.get(0) == 2)
                        streak++;

                    for (int k = 1; k < l.size(); k++) {
                        if (l.get(k) == prev)
                            continue;
                        if (l.get(k) == prev + 1) {
                            streak++;
                            if (streak > highestStreak) {
                                highestStreak = streak;
                                highCard = l.get(k);
                            }
                        } else {
                            if (k >= 3)
                                break;
                            streak = 1;
                        }

                        prev = l.get(k);
                    }

                    if (highestStreak >= 5) {
                        if (highCard == 14)
                            return ROYAL_FLUSH;
                        return STRAIGHT_FLUSH + highCard;
                    }
                }
            }
        }

        // calculate max sequence of same values
        int[] seenValues = new int[15];

        for (int i = 0; i < 7; i++) {
            seenValues[values[i]]++;
        }

        boolean four = false;
        int threeVal = 0;
        int twoVal1 = 0;
        int twoVal2 = 0;

        for (int i = 2; i < 15; i++) {
            if (seenValues[i] == 4) {
                if (firstValue != i && secondValue != i) {
                    // if all four cards are on the board, count the kicker
                    return FOUR_OF_A_KIND + i * 16 + Math.max(firstValue, secondValue);
                }
                return FOUR_OF_A_KIND + i * 16;
            } else if (seenValues[i] == 3) {
                if (i > threeVal) {
                    if (threeVal > twoVal1) {
                        twoVal1 = threeVal;
                        twoVal2 = twoVal1;
                    }

                    threeVal = i;
                } else if (i > twoVal1) {
                    twoVal2 = twoVal1;
                    twoVal1 = i;
                }

            } else if (seenValues[i] == 2) {
                twoVal2 = twoVal1;
                twoVal1 = i;
            }
        }

        // check for full house
        if (threeVal > 0 && twoVal1 > 0) {
            return FULL_HOUSE + threeVal * 16 + twoVal1;
        }

        if (highestStreak >= 5)
            return STRAIGHT + highCard;

        // flush but no straight?

        for (int c = 0; c < 4; c++) {
            if (colors[c] >= 5) {
                for (int i = 6; i >= 5; i--) {
                    if (cards[i] >> 4 == c)
                        return FLUSH + values[i];
                }

                return FLUSH + values[5];
            }
        }

        // three of a kind
        if (threeVal > 0) {
            for (int i = 6; i >= 0; i--) {
                if (values[i] != threeVal)
                    return THREE_OF_A_KIND + 16 * threeVal + values[i];
            }
        }

        // two pair and pair
        if (twoVal1 > 0) {
            if (twoVal2 > 0) {
                // two pair
                for (int i = 6; i >= 0; i--) {
                    if (values[i] != twoVal1 && values[i] != twoVal2)
                        return TWO_PAIR + 256 * twoVal1 + 16 * twoVal2 + values[i];
                }
            }

            for (int i = 6; i >= 0; i--) {
                if (values[i] != twoVal1)
                    return PAIR + 16 * twoVal1 + values[i];
            }
        }

        // high card
        return values[6] * 16 + values[5];
    }

    /**
     * Sorts the first array (of length 7) and permutes the second array the same
     * way
     * 
     * @param values
     * @param cards
     */
    public static void sortBoth(int[] values, int[] cards) {
        quickSort(values, cards, 0, values.length - 1);
    }

    /**
     * QuickSort implementation
     *
     * @param values array to be sorted
     * @param cards  array to be permuted the same way as values
     * @param low    lower bound of the array to be sorted
     * @param high   upper bound of the array to be sorted
     */
    private static void quickSort(int[] values, int[] cards, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(values, cards, low, high);
            quickSort(values, cards, low, pivotIndex - 1);
            quickSort(values, cards, pivotIndex + 1, high);
        }
    }

    // TODO: Strategic pivot selection
    /**
     * Partitions the array around the pivot (pivot strategy is to take the last
     * element as pivot)
     *
     * @param values array to be sorted
     * @param cards  array to be permuted the same way as values
     * @param low    lower bound of the array to be sorted
     * @param high   upper bound of the array to be sorted
     * @return index of the pivot
     */
    private static int partition(int[] values, int[] cards, int low, int high) {
        int pivot = values[high];
        int i = low - 1;
        for (int j = low; j <= high - 1; j++) {
            if (values[j] <= pivot) {
                i++;
                swap(values, i, j);
                swap(cards, i, j);
            }
        }
        swap(values, i + 1, high);
        swap(cards, i + 1, high);
        return i + 1;
    }

    /**
     * Swaps two elements in the array
     *
     * @param array array to be swapped
     * @param i     index of the first element
     * @param j     index of the second element
     */
    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * Shuffles array in place
     * 
     * @param arr
     * @param rand
     */
    public static void shuffleArray(int[] arr, Random rand) {
        for (int i = arr.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);

            // swap
            int a = arr[index];
            arr[index] = arr[i];
            arr[i] = a;
        }
    }

    public static int[] createDeck() {
        int[] deck = new int[52];

        for (int i = 0; i < 52; i++) {
            deck[i] = i;
        }

        // for (int i = 0; i < 4; i++) {
        // for (int j = 0; j < 13; j++) {
        // deck[i + (j << 2)] = (byte) ((j + 2) | (i << 4));
        // }
        // }

        return deck;
    }

    // public static String boardToString(byte[] board) {
    // StringBuilder s = new StringBuilder();
    // for (byte b : board) {
    // if (b == 0)
    // break;
    // s.append(new Card(b)).append(" ");
    // }
    // return s.toString();
    // }

    public static int[] getRandom7Hand(Random rand) {
        int[] deck = createDeck();
        shuffleArray(deck, rand);
        return new int[] { deck[0], deck[1], deck[2], deck[3], deck[4], deck[5], deck[6] };
    }

    /**
     * Rotates array left (in place)
     * 
     * @param arr
     * @param amount
     */
    private void rotateArray(Object[] arr, int amount) {
        Object[] temp = new Object[amount];
        System.arraycopy(arr, 0, temp, 0, amount);

        for (int i = amount; i < arr.length; i++) {
            arr[i - amount] = arr[i];
        }

        for (int i = 0; i < amount; i++) {
            arr[i + arr.length - amount] = temp[i];
        }
    }

    /**
     * Converts a card into a more readable representation
     * 
     * @param card
     * @return
     */
    public static String card2String(byte card) {
        int value = card & 0b1111;
        int color = card >> 4;
        String res = "";

        res += switch (value) {
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            case 14 -> "A";
            default -> value;
        };

        res += switch (color) {
            case 0 -> "d";
            case 1 -> "h";
            case 2 -> "s";
            case 3 -> "c";
            default -> {
                throw new RuntimeException("invalid color");
            }
        };

        return res;
    }

    /**
     * Converts a board into a more visually appealing representation
     * 
     * @param board
     * @return
     */
    public static String board2String(byte[] board) {
        String res = "";
        for (byte card : board) {
            if (card != 0)
                res += card2String(card);
        }
        return res;
    }
}
