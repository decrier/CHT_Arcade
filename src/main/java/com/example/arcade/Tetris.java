package com.example.arcade;

import javafx.animation.AnimationTimer;
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

    private int blockCol = COLS / 2;    // колонка блока
    private int blockRow = 0;           // строка блока (0 = вверху)

    // Добавь поля тайминга
    private double fallInterval = 0.5;  // секунды между шагами падения
    private double acc = 0;             // аккумулятор времени

    // Canvas
    private Canvas canvas = new Canvas(COLS * CELL, ROWS * CELL);
    private GraphicsContext g = canvas.getGraphicsContext2D();

    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(new StackPane(canvas));
        stage.setTitle(this.getClass().getSimpleName());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // игровой цикл с dt
        final long[] last = { System.nanoTime() };
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - last[0]) / 1_000_000_000.0;
                last[0] = now;
                update(dt);
                render();
            }
        }.start();
    }

    private void update(double dt) {
        acc += dt;
        if (acc >= fallInterval) {
            acc -= fallInterval;
            if (blockRow + 1 < ROWS) {
                blockRow++;
            }
        }
    }

    private void render() {
        renderGrid();
        drawCell(blockCol, blockRow, Color.web("#22d3ee"));
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

    private void drawCell(int col, int row, Color c){
        g.setFill(c);
        g.fillRect(col * CELL + 1, row * CELL + 1, CELL - 2, CELL - 2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
