package poker.util;

public class CardUtility {

    public static byte cardFromString(String str) {
        char rank = str.charAt(0);
        char suit = str.charAt(1);

        return (byte) ((rankToByte(rank) - 2) * 4 + suitToByte(suit));
    }

    public static byte cardFrom(char rank, int suit) {
        return (byte) ((rankToByte(rank) - 2) * 4 + suit);
    }

    public static String boardToString(long board) {
        String res = "";
        for (byte c = 0; c < 52; c++) {
            if ((board & 1) == 1)
                res += cardToString(c);
            board >>>= 1;
        }

        return res;
    }

    public static String handToString(byte card1, byte card2) {
        return cardToString(card1) + cardToString(card2);
    }

    public static String cardToString(byte card) {
        byte rank = (byte) (card / 4 + 2);
        byte suit = (byte) (card - (rank - 2) * 4);
        return rankToString(rank) + suitToString(suit);
    }

    public static String suitToString(byte suit) {
        return switch (suit) {
            case 3 -> "s";
            case 2 -> "h";
            case 1 -> "d";
            case 0 -> "c";
            default -> "x";
        };
    }

    public static String rankToString(byte rank) {
        return switch (rank) {
            case 2 -> "2";
            case 3 -> "3";
            case 4 -> "4";
            case 5 -> "5";
            case 6 -> "6";
            case 7 -> "7";
            case 8 -> "8";
            case 9 -> "9";
            case 10 -> "T";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            case 14 -> "A";
            default -> "2";
        };
    }

    public static byte rankToByte(char rank) {
        return switch (rank) {
            case '2' -> 2;
            case '3' -> 3;
            case '4' -> 4;
            case '5' -> 5;
            case '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 9;
            case 'T' -> 10;
            case 'J' -> 11;
            case 'Q' -> 12;
            case 'K' -> 13;
            case 'A' -> 14;
            default -> 2;
        };
    }

    public static byte suitToByte(char suit) {
        return switch (suit) {
            case 's' -> 3;
            case 'h' -> 2;
            case 'd' -> 1;
            case 'c' -> 0;
            default -> 0;
        };
    }

    static public boolean overlapBoard(long board, byte card1, byte card2) {
        return (board & (1L << card1 | 1L << card2)) != 0;
    }

    static public boolean overlapBoard(long board, byte card) {
        return (board & (1L << card)) != 0;
    }

    static public boolean overlap(byte card1, byte card2, byte card3) {
        return card1 == card2 || card1 == card3 || card2 == card3;
    }

    /*
     * test whether there is a overlap between the two hands
     */
    static public boolean overlap(byte h1c1, byte h1c2, byte h2c1, byte h2c2) {
        return h1c1 == h2c1 || h1c1 == h2c2 || h1c2 == h2c1 || h1c2 == h2c2;
    }

    static public byte rankOfCard(byte card) {
        return (byte) ((card >>> 2) + 2);
    }

    static public byte suitOfCard(byte card) {
        return (byte) (card & 3);
    }

    static public byte[] boardLongToArr(long board) {
        byte[] boardArr = new byte[5];
        int idx = 0;
        for (byte c = 0; c < 52; c++) {
            if ((board & 1) == 1)
                boardArr[idx++] = c;
            board >>>= 1;
        }
        return boardArr;
    }

    static public long boardArrToLong(byte... board) {
        long boardLong = 0;
        for (byte c : board)
            boardLong |= 1L << c;
        return boardLong;
    }

    static public int cardPairToIndex(byte card1, byte card2) {
        if (card1 > card2) {
            byte tmp = card2;
            card2 = card1;
            card1 = tmp;
        }

        return card1 * (101 - card1) / 2 + card2 - 1;
    }

    static public byte swapSuits(byte card, byte suit1, byte suit2) {
        if ((card & 3) == suit1) {
            card += suit2 - suit1;
        } else if ((card & 3) == suit2) {
            card += suit1 - suit2;
        }
        return card;
    }
}
