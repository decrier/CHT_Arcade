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
    private boolean gameOver = false;
    private enum GameState { READY, RUNNING, GAME_OVER}
    private GameState state = GameState.READY;

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
                    } else if (state == GameState.GAME_OVER){
                        restartGame();
                    }
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

        // столкновение с ракеткой
        boolean intersectX = ballX >= paddleX && ballX <= paddleX + paddleW;
        boolean intersectY = ballY + ballR >= paddleY && ballY + ballR <= paddleY + paddleH;
        if (intersectX && intersectY && ballVY > 0) {
            // 1. Выталкиваем мяч наверх
            ballY = paddleY - ballR;

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
        g.fillText("Жизни: " + lives, 10, 20);
        if (state == GameState.READY) {
            g.fillText("Нажми SPACE, чтобы начать", WIDTH / 2.0 - 80, HEIGHT / 2.0);
        }
        if (state == GameState.GAME_OVER) {
            g.fillText("GAME OVER, Нажми SPACE, чтобы начать новую игру", WIDTH / 2.0 - 130, HEIGHT / 2.0);
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
