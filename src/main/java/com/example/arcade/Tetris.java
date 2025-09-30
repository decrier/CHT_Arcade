package com.example.arcade;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;

public class Tetris extends Application {

    // Размеры сетки
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int CELL = 30; // pixels

    // Поле (матрица)
    private int[][] field = new int[ROWS][COLS];

    // Тетромино
    private int [][] shapes = {
            // I
            {0, -1, 0, 0, 0, 1, 0, 2},
            // O
            {0, 0, 1, 0, 0, 1, 1, 1},
            // Z
            {0, 0, 1, 0, 1, 1, 2, 1},
            // S
            {1, 0, 2, 0, 0, 1, 1, 1},
            // T
            {0, 0, 1, 0, 2, 0, 1, 1},
            // L
            {0, 0, 0, 1, 0, 2, 1, 2},
            // J
            {1, 0, 1, 1, 1, 2, 0, 2},
    };
    private int currentType;
    private int[] currentShape; // массив длиной 8 (x0,y0,x1,y1,...) выбранного типа
    private int curX, curY; // позиция якоря

    // Таймер
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

        // управление
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case LEFT, A -> {
                    if (canPlace(curX - 1, curY, currentShape)) curX--;
                }
                case RIGHT, D -> {
                    if (canPlace(curX + 1, curY, currentShape)) curX++;
                }
                case DOWN, S -> {
                    if (canPlace(curX, curY + 1, currentShape)) curY++;
                }
                default -> {}
            }
        });

        spawnPiece();

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
            if (canPlace(curX, curY + 1, currentShape)) {
                curY++;
            } else {
                lockPiece();
            }

        }
    }

    private void render() {
        renderGrid();
        renderField();
        renderPiece();
    }

    private void renderPiece() {
        Color c = colorOf(currentType);
        for (int i = 0; i < currentShape.length; i += 2) {
            int x = curX + currentShape[i];
            int y = curY + currentShape[i + 1];
            if (y >= 0) drawCell(x, y, c); // клетки выше экрана не рисуем
        }
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

    private void renderField() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int v = field[r][c];
                if (v != 0) {
                    drawCell(c, r, colorOf(v));
                }
            }
        }
    }

    private Color colorOf(int v) {
        return switch (v) {
            case 1 -> Color.web("#22d3ee"); // I
            case 2 -> Color.web("#facc15"); // O
            case 3 -> Color.web("#f43f5e"); // Z
            case 4 -> Color.web("#10b981"); // S
            case 5 -> Color.web("#a78bfa"); // T
            case 6 -> Color.web("#fb923c"); // L
            case 7 -> Color.web("#60a5fa"); // J
            default -> Color.GRAY;
        };
    }

    private void drawCell(int col, int row, Color c){
        g.setFill(c);
        g.fillRect(col * CELL + 1, row * CELL + 1, CELL - 2, CELL - 2);
    }

    private void spawnPiece() {
        currentType = 1 + (int) (Math.random() * 7);
        currentShape = shapes[currentType - 1].clone();
        curX = COLS / 2 - 1;
        curY = 0;
    }

    private boolean canPlace(int x, int y, int[] shape) {
        for (int i = 0; i < shape.length; i += 2) {
            int cx = x + shape[i];
            int cy = y + shape[i + 1];
            if (cx < 0 || cx >= COLS || cy >= ROWS) return false;
            if (cy >= 0 && field[cy][cx] != 0) return false;
        }
        return true;
    }

    private void lockPiece() {
        // переносим клетки фигуры в field
        for (int i = 0; i < currentShape.length; i += 2) {
            int x = curX + currentShape[i];
            int y = curY + currentShape[i + 1];
            if (y >= 0 && y < ROWS && x >= 0 && x < COLS) {
                field[y][x] = currentType;
            }
        }
        // проверка проигрыша: если заспавнить новую нельзя — GAME OVER
        spawnPiece();
        if (!canPlace(curX, curY, currentShape)) {
            // очищаем поле и начинаем заново (упростим на этом шаге)
            for (int r = 0; r < ROWS; r++) {
                Arrays.fill(field[r], 0);
            }
            spawnPiece();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
