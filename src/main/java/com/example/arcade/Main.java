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
    private double ballVX = 200;
    private double ballVY = 180;

    // ракетка
    private double paddleW = 100;
    private double paddleH = 15;
    private double paddleX = (WIDTH - paddleW) / 2.0;
    private double paddleY = HEIGHT - 40;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private double paddleSpeed = 300;

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
            }
        });
        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case LEFT, A -> moveLeft = false;
                case RIGHT, D ->  moveRight = false;
            }
        });
        stage.setTitle("Arcade — Шаг 1: мяч и стены");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void update(double dt) {
        ballX += ballVX * dt;
        ballY += ballVY * dt;
        if (ballX + ballR > WIDTH) {
            ballX = WIDTH - ballR;
            ballVX = -Math.abs(ballVX);
        }
        if (ballX - ballR < 0) {
            ballX = ballR;
            ballVX = Math.abs(ballVX);
        }
        if (ballY + ballR > HEIGHT) {
            ballY = HEIGHT - ballR;
            ballVY = -Math.abs(ballVY);
        }
        if (ballY - ballR < 0) {
            ballY = ballR;
            ballVY = Math.abs(ballVY);
        }
    }

    private void render(GraphicsContext g) {
        g.setFill(Color.web("0f172a"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFill(Color.web("#dd1100"));
        g.fillOval(ballX - ballR, ballY - ballR, ballR * 2, ballR * 2);
        g.setFill(Color.web("#facc15"));
        g.fillRect(paddleX, paddleY, paddleW, paddleH);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
