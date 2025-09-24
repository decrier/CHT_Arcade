package com.example.arcade;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Radar extends Application {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;

    // центр и радиус экрана радара
    private final double cx = WIDTH / 2.0;
    private final double cy = HEIGHT / 2.0;
    private final double rangeR = Math.min(WIDTH, HEIGHT) * 0.45;

    // параметры развертки
    private double sweepAngleDeg = -90;       // стартовый угол (вверх)
    private double sweepSpeedDeg = 90;        // градусов в секунду
    private double beamWidthDeg = 3.0;        // «толщина» луча (для попадания)
    private double trailFade = 0.08;          // затухание следа (0..1), больше — быстрее гаснет

    // цель (в полярных координатах относительно центра)
    private double targetR = rangeR * 0.65;   // дистанция до цели
    private double targetAngDeg = 30;         // угол до цели
    private double targetAngularSpeed = 12;   // цель медленно вращается (град/с)
    private double targetRadialOscAmp = rangeR * 0.05;
    private double targetRadialOscSpeed = 0.8; // Гц

    // «пинг» при обнаружении: таймер вспышки и ряби
    private double pingTimer = 0.0;           // 0 — нет пинга, (0..1] — идёт анимация
    private double pingDuration = 0.8;        // сек
    private double lastDetectTime = -10;      // защита от дребезга

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // статическая сетка — можно перерисовать один раз при старте
        drawStaticGrid(g);

        final long[] last = { System.nanoTime() };
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = (now - last[0]) / 1_000_000_000.0;
                last[0] = now;
                update(dt, now * 1e-9);
                render(g, dt);
            }
        }.start();

        stage.setTitle("Radar Sweep Demo");
        stage.setScene(new Scene(new StackPane(canvas)));
        stage.setResizable(false);
        stage.show();
    }

    private void update(double dt, double t) {
        // вращаем луч
        sweepAngleDeg = wrapDeg(sweepAngleDeg + sweepSpeedDeg * dt);

        // двигаем цель (лёгкое движение для наглядности)
        targetAngDeg = wrapDeg(targetAngDeg + targetAngularSpeed * dt);
        double rOsc = targetRadialOscAmp * Math.sin(2 * Math.PI * targetRadialOscSpeed * t);
        double curTargetR = clamp(targetR + rOsc, rangeR * 0.2, rangeR * 0.9);

        // обнаружение: когда луч «накрывает» цель по углу
        double delta = angleDiffDeg(sweepAngleDeg, targetAngDeg); // кратчайшая разница
        boolean underBeam = Math.abs(delta) <= beamWidthDeg * 0.5;

        // один «пинг» на проход, чтобы не свистело каждый кадр
        if (underBeam && (t - lastDetectTime) > 0.2) {
            pingTimer = 1.0;
            lastDetectTime = t;
        }

        // затухание пинга
        if (pingTimer > 0) {
            pingTimer -= dt / pingDuration;
            if (pingTimer < 0) pingTimer = 0;
        }

        // обновим «истинные» декартовы координаты цели (для рендера)
        targetX = cx + curTargetR * Math.cos(Math.toRadians(targetAngDeg));
        targetY = cy + curTargetR * Math.sin(Math.toRadians(targetAngDeg));
    }

    // вычисленные координаты цели (для удобства рендера)
    private double targetX, targetY;

    private void render(GraphicsContext g, double dt) {
        // полупрозрачная заливка для эффекта шлейфа
        g.setGlobalAlpha(trailFade);
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setGlobalAlpha(1.0);

        // (лёгкая) сетка поверх гашения, чтобы не исчезала совсем
        g.setGlobalAlpha(0.05);
        drawStaticGrid(g);
        g.setGlobalAlpha(1.0);

        // сектор/луч
        g.setStroke(Color.LIME);
        g.setLineWidth(2.0);
        double angRad = Math.toRadians(sweepAngleDeg);
        double x2 = cx + rangeR * Math.cos(angRad);
        double y2 = cy + rangeR * Math.sin(angRad);
        g.strokeLine(cx, cy, x2, y2);

        // можно подсветить небольшим «веером» (мягкий луч)
        g.setGlobalAlpha(0.25);
        g.setFill(Color.LIME);
        double start = sweepAngleDeg - beamWidthDeg * 0.5;
        g.fillArc(cx - rangeR, cy - rangeR, rangeR * 2, rangeR * 2, -start, -beamWidthDeg, javafx.scene.shape.ArcType.ROUND);
        g.setGlobalAlpha(1.0);

        // цель (тусклая точка)
        g.setFill(Color.web("#2ee62e"));
        g.fillOval(targetX - 3, targetY - 3, 6, 6);

        // при пинге — яркая вспышка и расходящаяся рябь
        if (pingTimer > 0) {
            double ease = 1.0 - pingTimer;                     // 0..1
            double maxRipple = rangeR * 0.18;                  // радиус ряби
            double rippleR = 6 + maxRipple * ease;
            double alpha = 1.0 - ease;                         // гаснет со временем

            // вспышка
            g.setGlobalAlpha(0.7 * alpha);
            g.setFill(Color.web("#A8FF00"));
            g.fillOval(targetX - 5, targetY - 5, 10, 10);

            // рябь
            g.setGlobalAlpha(0.9 * alpha);
            g.setStroke(Color.LIME);
            g.setLineWidth(1.5);
            g.strokeOval(targetX - rippleR, targetY - rippleR, rippleR * 2, rippleR * 2);

            g.setGlobalAlpha(1.0);
        }

        // центральная «метка» и рамка
        g.setStroke(Color.web("#146914"));
        g.setLineWidth(1.0);
        g.strokeOval(cx - rangeR, cy - rangeR, rangeR * 2, rangeR * 2);

        // перекрестие в центре
        g.setStroke(Color.web("#1aff1a"));
        g.strokeLine(cx - 8, cy, cx + 8, cy);
        g.strokeLine(cx, cy - 8, cx, cy + 8);
    }

    private void drawStaticGrid(GraphicsContext g) {
        // фон
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // концентрические круги
        g.setStroke(Color.web("#093a09"));
        g.setLineWidth(1.0);
        for (int i = 1; i <= 4; i++) {
            double r = rangeR * i / 4.0;
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }

        // радиальные лучи каждые 30°
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            double x = cx + rangeR * Math.cos(rad);
            double y = cy + rangeR * Math.sin(rad);
            g.strokeLine(cx, cy, x, y);
        }
    }

    // ===== утилиты углов/клампа =====
    private static double wrapDeg(double a) {
        a %= 360.0;
        if (a < 0) a += 360.0;
        return a;
    }

    /** кратчайшая разница углов (−180..+180) */
    private static double angleDiffDeg(double a, double b) {
        double d = wrapDeg(a) - wrapDeg(b);
        d = ((d + 540) % 360) - 180;
        return d;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

