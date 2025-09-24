package com.example.arcade;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Pulse extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 300;

    // Осциллограф
    private double sweepX = 0;                 // текущее положение «луча»
    private double sweepSpeed = 200;           // пикселей в секунду (скорость развертки)
    private double baseline = HEIGHT * 0.6;    // базовая линия

    // Сердечный ритм
    private double bpm = 72;                   // базовый пульс
    private double bpmJitter = 2.0;            // небольшая вариация
    private double phase = 0;                  // фаза удара [0..1)
    private double amplitude = 60;             // высота сигнала в пикселях
    private double noiseAmp = 1.5;             // лёгкий шум

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // фон + сетка
        drawGrid(g);

        final long[] last = { System.nanoTime() };

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - last[0]) / 1_000_000_000.0;
                last[0] = now;

                update(dt);
                render(g, dt);
            }
        }.start();

        stage.setTitle("Pulse Oscilloscope");
        stage.setScene(new Scene(new StackPane(canvas)));
        stage.setResizable(false);
        stage.show();
    }

    private void update(double dt) {
        // Лёгкая вариация частоты, чтобы было «живее»
        double bpmNow = bpm + Math.sin(System.nanoTime() * 1e-9 * 0.5) * bpmJitter;
        double beatsPerSecond = bpmNow / 60.0;

        // Обновляем фазу удара [0..1)
        phase += beatsPerSecond * dt;
        if (phase >= 1.0) phase -= 1.0;

        // Двигаем развертку
        sweepX += sweepSpeed * dt;
        if (sweepX >= WIDTH) {
            sweepX -= WIDTH;
        }
    }

    private void render(GraphicsContext g, double dt) {
        // Лёгкое «затухание» следа (прозрачная заливка), чтобы оставалась дорожка
        g.setGlobalAlpha(0.08); // чем больше, тем быстрее гаснет след
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setGlobalAlpha(1.0);

        // (опционально) перерисовать сетку раз в N кадров — но для простоты подмешаем слегка:
        g.setGlobalAlpha(0.08);
        drawGrid(g);
        g.setGlobalAlpha(1.0);

        // Текущая выборка сигнала
        double signal = ecgWave(phase);
        // Немного шума
        signal += (Math.random() - 0.5) * 2.0 * (noiseAmp / amplitude);

        double y = baseline - signal * amplitude;

        // Цвет «луча»
        g.setStroke(Color.LIME);
        g.setLineWidth(2.0);

        // Рисуем короткий отрезок по направлению движения, чтобы линия была непрерывной
        double prevX = sweepX - sweepSpeed * dt;
        if (prevX < 0) prevX += WIDTH;

        // Чтобы не было линии через левую границу — разбиваем на два случая
        if (prevX <= sweepX) {
            g.strokeLine(prevX, y, sweepX, y);
        } else {
            // кусок до правого края
            g.strokeLine(prevX, y, WIDTH, y);
            // и от начала до текущего X
            g.strokeLine(0, y, sweepX, y);
        }

        // Яркая «головка» — точка-луч
        g.setFill(Color.web("#A8FF00"));
        g.fillOval(sweepX - 2, y - 2, 4, 4);
    }

    // Простейшая сетка и базовая линия
    private void drawGrid(GraphicsContext g) {
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setStroke(Color.web("#093a09")); // тёмно-зелёная сетка
        g.setLineWidth(1.0);

        int small = 20; // шаг мелкой клетки (аналог 1 мм в пикселях — условно)
        for (int x = 0; x < WIDTH; x += small) {
            g.strokeLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y < HEIGHT; y += small) {
            g.strokeLine(0, y, WIDTH, y);
        }

        // Базовая линия
        g.setStroke(Color.web("#1aff1a"));
        g.setLineWidth(1.5);
        g.strokeLine(0, baseline, WIDTH, baseline);
    }

    /**
     * Нормализованный «удар сердца» на интервале [0..1).
     * Это НЕ медицинская модель, а стилизованная форма:
     * маленькая P-волна, резкий QRS, плавная T-волна.
     */
    private double ecgWave(double t) {
        // центр и ширина участков во «фазе»
        double pCenter = 0.18, pWidth = 0.08, pAmp = 0.1;
        double qCenter = 0.38, qWidth = 0.02, qAmp = -0.35;
        double rCenter = 0.40, rWidth = 0.015, rAmp = 1.0;
        double sCenter = 0.42, sWidth = 0.02, sAmp = -0.45;
        double tCenter = 0.70, tWidth = 0.16, tAmp = 0.25;

        // Гауссовы «бугорки» для P и T, и узкие пики/провалы для QRS
        double p = gaussian(t, pCenter, pWidth) * pAmp;
        double q = gaussian(t, qCenter, qWidth) * qAmp;
        double r = gaussian(t, rCenter, rWidth) * rAmp;
        double s = gaussian(t, sCenter, sWidth) * sAmp;
        double tt = gaussian(t, tCenter, tWidth) * tAmp;

        // Слабая дрожь изоэлектрической линии
        double baselineRipple = 0.015 * Math.sin(2 * Math.PI * (t * 4.0));

        return p + q + r + s + tt + baselineRipple;
    }

    // Гаусс без нормировки (достаточно для формы)
    private double gaussian(double x, double mu, double sigma) {
        double dx = wrap01(x - mu);     // учитываем цикличность фазы
        // выбираем ближайший период (0..1) к центру mu
        if (dx > 0.5) dx -= 1.0;
        return Math.exp(-(dx * dx) / (2 * sigma * sigma));
    }

    // Сворачиваем значение в диапазон [0..1)
    private double wrap01(double v) {
        v = v % 1.0;
        if (v < 0) v += 1.0;
        return v;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
