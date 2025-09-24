package com.example.arcade;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // мяч
    private double ballX = WIDTH / 2.0;
    private double ballY = HEIGHT / 2.0;
    private double ballR = 10; // радиус
    private double ballVX = 300;
    private double ballVY = -250;

    // ракетка
    private double paddleW = 100;
    private double paddleH = 15;
    private double paddleX = (WIDTH - paddleW) / 2.0;
    private double paddleY = HEIGHT - 40;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private double paddleSpeed = 300;

    private int lives = 3;
    private int score = 0;
    private enum GameState { READY, RUNNING, PAUSE, GAME_OVER}
    private GameState state = GameState.READY;

    // кирпичики
    private static final int BRICK_COLS = 10; // столбцов
    private static final int BRICK_ROWS = 5; // строк
    private static final double BRICK_W = 70; // ширина кирпича
    private static final double BRICK_H = 20; // высота кирпича
    private static final double BRICK_GAP = 6; // зазор между кирпичами
    private static final double BRICK_OFFSET_X = 20; // отступ слева
    private static final double BRICK_OFFSET_Y = 60; // отступ сверху
    private boolean[][] bricks = new boolean[BRICK_ROWS][BRICK_COLS];

    @Override
    public void start(Stage stage) throws Exception {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();

        final long[] last = { System.nanoTime() };
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - last[0]) / 1_000_000_000.0;
                last[0] = now;
                update(dt);
                render(g);
            }
        }.start();


        Scene scene = new Scene(new StackPane(canvas));
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case LEFT, A -> moveLeft = true;
                case RIGHT, D ->  moveRight = true;
                case SPACE -> {
                    if (state == GameState.READY) {
                        state = GameState.RUNNING;
                        initBricks();
                    } else if (state == GameState.GAME_OVER){
                        restartGame();
                    }
                }
                case P -> {
                    if (state == GameState.RUNNING) state = GameState.PAUSE;
                    else if(state == GameState.PAUSE) state = GameState.RUNNING;
                }
                default -> {}
            }
        });
        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case LEFT, A -> moveLeft = false;
                case RIGHT, D ->  moveRight = false;
                default -> {}
            }
        });
        stage.setTitle("Arcade — Шаг 1: мяч и стены");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void update(double dt) {
        if (state != GameState.RUNNING) return;

        // движение мяча
        ballX += ballVX * dt;
        ballY += ballVY * dt;

        // отскоки от стен
        if (ballX + ballR > WIDTH) {
            ballX = WIDTH - ballR;
            ballVX = -Math.abs(ballVX);
        }
        if (ballX - ballR < 0) {
            ballX = ballR;
            ballVX = Math.abs(ballVX);
        }
        if (ballY - ballR < 0) {
            ballY = ballR;
            ballVY = Math.abs(ballVY);
        }

        // столкновение с кирпичом
        int col = (int) ((ballX - BRICK_OFFSET_X) / (BRICK_W + BRICK_GAP));
        int row = (int) ((ballY - BRICK_OFFSET_Y) / (BRICK_H + BRICK_GAP));
        if (row >= 0 && row < BRICK_ROWS && col >= 0 && col < BRICK_COLS && bricks[row][col]) {
            // координаты целевого кирпича
            double bx = BRICK_OFFSET_X + col * (BRICK_W + BRICK_GAP);
            double by = BRICK_OFFSET_Y + row * (BRICK_H + BRICK_GAP);
            // Проверим точнее: круг (мяч) против прямоугольника (кирпич)
            if (circleIntersectsRect(ballX, ballY, ballR, bx, by, BRICK_W, BRICK_H)) {
                bricks[row][col] = false;   // разбили
                score += 50;                // очки за кирпич
                // определим сторону удара и отразим скорость
                resolveBounceAgainstRect(bx, by, BRICK_W, BRICK_H);
            }
        }

        // столкновение с ракеткой
        boolean intersectX = ballX >= paddleX && ballX <= paddleX + paddleW;
        boolean intersectY = ballY + ballR >= paddleY && ballY + ballR <= paddleY + paddleH;
        if (intersectX && intersectY && ballVY > 0) {
            // 1. Выталкиваем мяч наверх
            ballY = paddleY - ballR;
            score += 10;

            // 2. Считаем смещение от центра ракетки
            double hitPos = (ballX - (paddleX + paddleW / 2.0)) / (paddleW / 2.0);

            // 3. Задаем новую скорость
            double speed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);
//            ballVX = speed * hitPos;
//            ballVY = -Math.abs(speed * (1 - Math.abs(hitPos)));
            ballVY = -Math.abs(ballVY);
        }

        // потеря мяча
        if (ballY - ballR > HEIGHT) {
            lives--;
            System.out.println("Жизней осталось: " + lives);
            if (lives > 0) {
                resetBall();
            } else {
                state = GameState.GAME_OVER;
                System.out.println("Игра окончена!");
                ballVX = 0;
                ballVY = 0;
            }
        }

        // движение ракетки по нажатым клавишам
        if (moveLeft) paddleX -= paddleSpeed * dt;
        if (moveRight) paddleX += paddleSpeed * dt;

        // ограничение ракетки по краям
        if (paddleX < 0) paddleX = 0;
        if (paddleX + paddleW > WIDTH) paddleX = WIDTH - paddleW;
    }

    private void render(GraphicsContext g) {
        g.setFill(Color.web("0f172a"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFill(Color.web("#dd1100"));
        g.fillOval(ballX - ballR, ballY - ballR, ballR * 2, ballR * 2);
        g.setFill(Color.web("#facc15"));
        g.fillRect(paddleX, paddleY, paddleW, paddleH);
        g.setFill(Color.WHITE);
        g.fillText("Lives: " + lives, 10, 20);
        g.fillText("Score: " + score, 10, 40);
        if (state == GameState.READY) {
            g.fillText("Press SPACE to begin", WIDTH / 2.0 - 80, HEIGHT / 2.0);
        }
        if (state == GameState.PAUSE) {
            g.fillText("Pause. P - continue", WIDTH / 2.0 - 90, HEIGHT / 2.0 + 20);
        }
        if (state == GameState.GAME_OVER) {
            g.fillText("GAME OVER, Press SPACE to start a new game", WIDTH / 2.0 - 130, HEIGHT / 2.0);
        }
        for (int row = 0; row < BRICK_ROWS; row++) {
            for (int col = 0; col < BRICK_COLS; col++) {
                if (!bricks[row][col]) continue;
                double x = BRICK_OFFSET_X + col * (BRICK_W + BRICK_GAP);
                double y = BRICK_OFFSET_Y + row * (BRICK_H + BRICK_GAP);
                g.setFill(Color.hsb(40 + row * 30, 0.8, 0.9));
                g.fillRect(x, y, BRICK_W, BRICK_H);
            }
        }
        if (allBricksCleared()) {
            g.fillText("YOU WON!", WIDTH / 2.0 - 40, HEIGHT / 2.0 - 20);
            restartGame();
        }
    }

    private void resetBall(){
        ballX = WIDTH / 2.0;
        ballY = HEIGHT / 2.0;
        ballVX = 200;
        ballVY = 180;
    }

    private void restartGame() {
        lives = 3;
//        score = 0;
        resetBall();
        state = GameState.READY;
        paddleX = (WIDTH - paddleW)/ 2.0;
        score = 0;
        initBricks();
    }

    private void initBricks() {
        for (int row = 0; row < BRICK_ROWS; row++) {
            for (int col = 0; col < BRICK_COLS; col++) {
                bricks[row][col] = true;
            }
        }
    }

    private boolean circleIntersectsRect(
            double cx, double cy, double cr,
            double rx, double ry, double rw, double rh) {

        // находим ближайшую точку прямоугольника к центру круга
        double nearestX = Math.max(rx, Math.min(cx, rx + rw));
        double nearestY = Math.max(ry, Math.min(cy, ry + rh));

        // 2) расстояние от центра круга до этой точки (квадрат)
        double dx = cx - nearestX;
        double dy = cy - nearestY;

        // 3) пересечение есть, если расстояние ≤ радиуса (в квадрате)
        return dx * dx + dy * dy <= cr * cr;
    }

    private void resolveBounceAgainstRect(double rx, double ry, double rw, double rh) {
        // Перекрытие по осям: насколько мяч зашёл внутрь прямоугольника
        double cx = ballX;
        double cy = ballY;
        double r = ballR;
        double leftOverlap = (cx + r) - rx;             // зашли слева внутрь
        double rightOverlap = (rx + rw) - (cx - r);     // зашли справа внутрь
        double topOverlap = (cy + r) - ry;              // зашли сверху внутрь
        double bottomOverlap = (ry + rh) - (cy - r);    // зашли снизу внутрь

        // Берём минимальное положительное перекрытие — это «наименее глубокая» ось столкновения
        double minX = Math.min(leftOverlap, rightOverlap);
        double minY = Math.min(topOverlap, bottomOverlap);

        if (minX < minY) {
            ballVX = -ballVX;
            if (leftOverlap < rightOverlap) {
                ballX = rx - r;
            } else {
                ballX = rx + rw + r;
            }
        } else {
            ballVY = -ballVY;
            if (topOverlap < bottomOverlap) {
                ballY = ry - r;
            } else {
                ballY = ry + rh + r;
            }
        }
    }

    private boolean allBricksCleared() {
        for (int row = 0; row < BRICK_ROWS; row++) {
            for (int col = 0; col < BRICK_COLS; col++) {
                if (bricks[row][col]) return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
