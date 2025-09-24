package com.example.arcade;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TwoDots extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // стартовые позиции
    private final double startPlayerX = WIDTH / 2.0;
    private final double startPlayerY = HEIGHT - 30.0;
    private final double startEnemyX = WIDTH / 4.0;
    private final double startEnemyY = HEIGHT / 3.0;

    private double playerX = startPlayerX;
    private double playerY = startPlayerY;
    private double playerR = 10;
    private double playerSpeed = 200;
    private boolean playerMoveRight = false;
    private boolean playerMoveLeft = false;
    private boolean playerMoveUp = false;
    private boolean playerMoveDown = false;

    private double enemyX = startEnemyX;
    private double enemyY = startEnemyY;
    private double enemyR = 10;
    private double enemySpeed = 150;

    private int caughtCount = 0;

    //неузвимость
    private boolean invulnerable = false;
    private double invulnTimer = 0.0;
    private static final double INVULN_DURATION = 3.0;

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
                case LEFT -> playerMoveLeft = true;
                case RIGHT -> playerMoveRight = true;
                case UP -> playerMoveUp = true;
                case DOWN -> playerMoveDown = true;
                default -> {}
            }
        });
        scene.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case LEFT -> playerMoveLeft = false;
                case RIGHT -> playerMoveRight = false;
                case UP -> playerMoveUp = false;
                case DOWN -> playerMoveDown = false;
                default -> {}
            }
        });
        stage.setTitle("Exercise");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void update(double dt) {
        if (playerMoveRight) {
            playerX += playerSpeed * dt;
        }
        if (playerMoveLeft) {
            playerX -= playerSpeed * dt;
        }
        if (playerMoveUp) {
            playerY -= playerSpeed * dt;
        }
        if (playerMoveDown) {
            playerY += playerSpeed * dt;
        }
        if (playerX < playerR) playerX = playerR;
        if (playerX > WIDTH - playerR) playerX = WIDTH - playerR;
        if (playerY < playerR) playerY = playerR;
        if (playerY > HEIGHT - playerR) playerY = HEIGHT - playerR;

        double tx = playerX - enemyX;
        double ty = playerY - enemyY;
        double dist = Math.hypot(tx, ty);
        double touch = playerR + enemyR;

        if (dist > 1e-6) {
            double nx = tx / dist;
            double ny = ty / dist;

            double step = enemySpeed * dt;

            if (step > dist) step = dist;

            enemyX += step * nx;
            enemyY += step * ny;
        }
        // таймер неуязвимости
        if (invulnerable) {
            invulnTimer -= dt;
            if (invulnTimer <= 0) {
                invulnerable = false;
                invulnTimer = 0;
            }
        }
        if (!invulnerable && dist <= touch + 1e-6) {
            caughtCount++;
            playerX = startPlayerX;
            playerY = startPlayerY;
            enemyX = startEnemyX;
            enemyY = startEnemyY;
            invulnerable = true;
            invulnTimer = INVULN_DURATION;
        }
    }

    private void render(GraphicsContext g) {
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // игрок
        if (invulnerable) {
            boolean visible = ((int) (invulnTimer * 10) % 2 == 0);
            if (visible) {
                g.setFill(Color.GOLD);
                g.fillOval(playerX - playerR, playerY - playerR, playerR * 2, playerR * 2);
            }
        } else {
            g.setFill(Color.ORANGE);
            g.fillOval(playerX - playerR, playerY - playerR, playerR * 2, playerR * 2);
        }

        // преследователь
        g.setFill(Color.RED);
        g.fillOval(enemyX - enemyR, enemyY - enemyR, enemyR * 2, enemyR * 2);

        g.setFill(Color.WHITE);
        g.fillText("Caught: " + caughtCount, 10, 20);
        if (invulnerable) {
            g.fillText("Invulnerable: " + String.format("%.1f", invulnTimer), 10, 40);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
