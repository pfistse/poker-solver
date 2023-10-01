package poker.explorer;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import poker.util.CardUtility;

public class RangeMonitor extends Stage {

    private static final double TILE_SIZE = 50;
    private static final double TILE_GAP = 1;

    public RangeMonitor() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(TILE_GAP);
        gridPane.setVgap(TILE_GAP);
        gridPane.setStyle("-fx-background-color: lightgray;");

        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                gridPane.add(new Rectangle(TILE_SIZE, TILE_SIZE, Color.GREY), x, y);
            }
        }

        double stageSize = (TILE_SIZE + TILE_GAP) * 13 - TILE_GAP;
        setScene(new Scene(gridPane, stageSize, stageSize));
        show();
    }

    public void displayStrategy(float[] strategy, float[] weighting, byte[] preflopCombos, int numActions) {

        assert (strategy.length == preflopCombos.length * numActions);

        int numHands = preflopCombos.length / 2;

        double[][] tileDistr = new double[13 * 13][3];
        String[] tileLabels = new String[13 * 13];

        byte card1, card2;
        byte rank1, rank2;
        int idx;

        for (int h = 0; h < numHands; h++) {

            card1 = preflopCombos[2 * h];
            card2 = preflopCombos[2 * h + 1];

            rank1 = CardUtility.rankOfCard(card1);
            rank2 = CardUtility.rankOfCard(card2);

            byte maxRank = (byte) Math.max(rank1, rank2);
            byte minRank = (byte) Math.min(rank1, rank2);

            if (CardUtility.suitOfCard(card1) == CardUtility.suitOfCard(card2)) {
                idx = (14 - maxRank) * 13 + (14 - minRank);
                tileLabels[idx] = CardUtility.rankToString(maxRank) + CardUtility.rankToString(minRank)
                        + "s";
            } else {
                idx = (14 - minRank) * 13 + (14 - maxRank);
                tileLabels[idx] = CardUtility.rankToString(maxRank) + CardUtility.rankToString(minRank)
                        + (rank1 == rank2 ? "" : "o");
            }

            for (int j = 0; j < numActions && j < 3; j++)
                tileDistr[idx][j] += strategy[j * numHands + h];

        }

        GridPane gridPane = new GridPane();
        gridPane.setHgap(TILE_GAP);
        gridPane.setVgap(TILE_GAP);
        gridPane.setStyle("-fx-background-color: black;");

        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                idx = 13 * y + x;
                double[] distr = tileDistr[idx];

                Node tile;
                if (distr[0] == 0 && distr[1] == 0 && distr[2] == 0)
                    tile = createGreyTile(TILE_SIZE);
                else
                    tile = createDistributionTile(TILE_SIZE, distr[0], distr[1], distr[2], tileLabels[idx]);

                gridPane.add(tile, x, y);
            }
        }

        getScene().setRoot(gridPane);
    }

    public void displayEquity(float[] equity, byte[] hands) {

        assert (equity.length == hands.length);

        int numHands = hands.length / 2;

        int[] tileCount = new int[13 * 13];
        double[] tileValues = new double[13 * 13];
        String[] tileLabels = new String[13 * 13];

        byte card1, card2;
        byte rank1, rank2;
        int idx;

        for (int h = 0; h < numHands; h++) {
            card1 = hands[2 * h];
            card2 = hands[2 * h + 1];

            rank1 = CardUtility.rankOfCard(card1);
            rank2 = CardUtility.rankOfCard(card2);

            byte maxRank = (byte) Math.max(rank1, rank2);
            byte minRank = (byte) Math.min(rank1, rank2);

            if (CardUtility.suitOfCard(card1) == CardUtility.suitOfCard(card2)) {
                idx = (14 - maxRank) * 13 + (14 - minRank);
                tileLabels[idx] = CardUtility.rankToString(maxRank) + CardUtility.rankToString(minRank)
                        + "s";
            } else {
                idx = (14 - minRank) * 13 + (14 - maxRank);
                tileLabels[idx] = CardUtility.rankToString(maxRank) + CardUtility.rankToString(minRank)
                        + (rank1 == rank2 ? "" : "o");
            }

            if (!Float.isNaN(equity[h])) {
                tileValues[idx] += equity[h];
                tileCount[idx]++;
            }

        }

        // normalize tile values
        for (int t = 0; t < tileValues.length; t++)
            tileValues[t] /= tileCount[t];

        GridPane gridPane = new GridPane();
        gridPane.setHgap(TILE_GAP);
        gridPane.setVgap(TILE_GAP);
        gridPane.setStyle("-fx-background-color: black;");

        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                idx = 13 * y + x;

                Node tile;
                if (Double.isNaN(tileValues[idx])) {
                    tile = createGreyTile(TILE_SIZE);
                } else {
                    tile = createIntensityTile(TILE_SIZE, tileValues[idx], 1, 0, tileLabels[idx]);
                }

                gridPane.add(tile, x, y);
            }
        }

        getScene().setRoot(gridPane);
    }

    private Node createIntensityTile(double size, double intensity, double max, double min, String text) {
        double relativeIntensity = (intensity + min) / (max - min);
        int green = (int) (relativeIntensity * 255);

        Group group = new Group();
        Rectangle redRect = new Rectangle(size, size, Color.rgb(0, green, 0));

        Font fontHand = Font.font("Helvetica", FontWeight.BOLD, FontPosture.REGULAR, 15);
        Font fontIntensity = Font.font("Helvetica", FontWeight.NORMAL, FontPosture.REGULAR, 13);

        Label handLabel = new Label(text);
        handLabel.setFont(fontHand);
        handLabel.setTextFill(Color.WHITESMOKE);

        Label intensityLabel = new Label(String.format("%.2f", intensity));
        intensityLabel.setFont(fontIntensity);
        intensityLabel.setTextFill(Color.WHITESMOKE);
        intensityLabel.setAlignment(Pos.BOTTOM_RIGHT);

        group.getChildren().addAll(redRect, intensityLabel);
        return group;
    }

    private Node createDistributionTile(double size, double red, double green, double blue, String text) {

        double totalWidth = red + green + blue;
        double redWidth = size * red / totalWidth;
        double greenWidth = size * green / totalWidth;
        double blueWidth = size * blue / totalWidth;

        Group group = new Group();
        Rectangle redRect = new Rectangle(redWidth, size, Color.RED);
        Rectangle greenRect = new Rectangle(greenWidth, size, Color.GREEN);
        greenRect.setTranslateX(redWidth);
        Rectangle blueRect = new Rectangle(blueWidth, size, Color.BLUE);
        blueRect.setTranslateX(redWidth + greenWidth);

        Label label = new Label(text);
        Font font = Font.font("Helvetica", FontWeight.BOLD, FontPosture.REGULAR, 15);
        label.setFont(font);
        label.setTextFill(Color.WHITESMOKE);

        group.getChildren().addAll(redRect, greenRect, blueRect, label);
        return group;
    }

    private Node createGreyTile(double size) {
        return new Rectangle(size, size, Color.rgb(25, 25, 25));
    }
}