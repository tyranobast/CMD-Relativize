package fr.skytale.cmdrelativize;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class SelectionBox {
    private @Nullable Point p1;
    private @Nullable Point p2;

    public SelectionBox() {
        this.p1 = null;
        this.p2 = null;
    }

    public void setP1(@NotNull Point p1) {
        this.p1 = p1;
    }

    public void setP2(@NotNull Point p2) {
        this.p2 = p2;
    }

    public double volume() {
        if (p1 == null || p2 == null) {
            return -1;
        }

        double dx = Math.abs(p1.x - p2.x);
        double dy = Math.abs(p1.y - p2.y);
        double dz = Math.abs(p1.z - p2.z);
        return dx * dy * dz;
    }

    public void forVolume(Consumer<Point> consumer) {
        if (p1 == null || p2 == null) {
            return;
        }

        for (double x = Math.min(p1.x, p2.x); x < Math.max(p1.x, p2.x); x++) {
            for (double y = Math.min(p1.y, p2.y); y < Math.max(p1.y, p2.y); y++) {
                for (double z = Math.min(p1.z, p2.z); z < Math.max(p1.z, p2.z); z++) {
                    consumer.accept(new Point(x, y, z));
                }
            }
        }
    }

    public void forVertices(Consumer<Point> consumer, double step) {
        if (p1 == null || p2 == null) {
            return;
        }

        for (double x = Math.min(p1.x, p2.x); x < Math.max(p1.x, p2.x); x += step) {
            consumer.accept(new Point(x, p1.y, p1.z));
            consumer.accept(new Point(x, p2.y, p1.z));
            consumer.accept(new Point(x, p1.y, p2.z));
            consumer.accept(new Point(x, p2.y, p2.z));
        }

        for (double y = Math.min(p1.y, p2.y); y < Math.max(p1.y, p2.y); y += step) {
            consumer.accept(new Point(p1.x, y, p1.z));
            consumer.accept(new Point(p2.x, y, p1.z));
            consumer.accept(new Point(p1.x, y, p2.z));
            consumer.accept(new Point(p2.x, y, p2.z));
        }

        for (double z = Math.min(p1.z, p2.z); z < Math.max(p1.z, p2.z); z += step) {
            consumer.accept(new Point(p1.x, p1.y, z));
            consumer.accept(new Point(p2.x, p1.y, z));
            consumer.accept(new Point(p1.x, p2.y, z));
            consumer.accept(new Point(p2.x, p2.y, z));
        }
    }

    public boolean isComplete() {
        return p1 != null && p2 != null;
    }

    public record Point(double x, double y, double z) {
        public String getAsText() {
            return "(" + x + ", " + y + ", " + z + ")";
        }
    }
}
