package com.example.arcade;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Tetris extends Application {

    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int CELL = 30; // pixels

    // Canvas
    private Canvas canvas = new Canvas(COLS * CELL, ROWS * CELL);
    private GraphicsContext g = canvas.getGraphicsContext2D();

    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(new StackPane(canvas));
        stage.setTitle("Tetris");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        renderGrid();
    }

    private void renderGrid() {
        // background
        g.setFill(Color.web("#0f172a"));
        g.fillRect(0, 0, COLS * CELL, ROWS * CELL);

        // grid
        g.setStroke(Color.web("#1f2937"));
        g.setLineWidth(1);
        for (int x = 0; x <= COLS; x++) {
            g.strokeLine(x * CELL, 0, x * CELL, ROWS * CELL);
        }
        for (int y = 0; y <= ROWS; y++) {
            g.strokeLine(0, y * CELL, CELL * COLS, y * CELL);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
