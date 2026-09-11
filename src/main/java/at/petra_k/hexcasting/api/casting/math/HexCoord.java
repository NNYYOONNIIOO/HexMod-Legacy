package at.petra_k.hexcasting.api.casting.math;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Immutable axial coordinates for the hexagonal pattern grid. */
public final class HexCoord {
    public static final HexCoord Origin = new HexCoord(0, 0);

    private final int q;
    private final int r;

    public HexCoord(int q, int r) {
        this.q = q;
        this.r = r;
    }

    public int getQ() {
        return q;
    }

    public int getR() {
        return r;
    }

    public int component1() {
        return q;
    }

    public int component2() {
        return r;
    }

    public int s() {
        return -q - r;
    }

    public HexCoord copy(int newQ, int newR) {
        return new HexCoord(newQ, newR);
    }

    public HexCoord shiftedBy(HexCoord other) {
        return new HexCoord(q + other.q, r + other.r);
    }

    public HexCoord shiftedBy(HexDir direction) {
        return shiftedBy(direction.asDelta());
    }

    public HexCoord delta(HexCoord other) {
        return new HexCoord(q - other.q, r - other.r);
    }

    /** Kotlin's {@code operator fun plus(HexCoord)} equivalent. */
    public HexCoord plus(HexCoord other) {
        return shiftedBy(other);
    }

    /** Kotlin's {@code operator fun plus(HexDir)} equivalent. */
    public HexCoord plus(HexDir direction) {
        return shiftedBy(direction);
    }

    /** Kotlin's {@code operator fun minus(HexCoord)} equivalent. */
    public HexCoord minus(HexCoord other) {
        return delta(other);
    }

    public int distanceTo(HexCoord other) {
        return (Math.abs(q - other.q)
            + Math.abs(q + r - other.q - other.r)
            + Math.abs(r - other.r)) / 2;
    }

    public Iterator<HexCoord> rangeAround(int radius) {
        return new RingIter(this, radius);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HexCoord)) {
            return false;
        }
        HexCoord coord = (HexCoord) other;
        return q == coord.q && r == coord.r;
    }

    @Override
    public int hashCode() {
        return Objects.hash(q, r);
    }

    @Override
    public String toString() {
        return "HexCoord(q=" + q + ", r=" + r + ")";
    }

    private static final class RingIter implements Iterator<HexCoord> {
        private final HexCoord center;
        private final int radius;
        private int q;
        private int r;

        private RingIter(HexCoord center, int radius) {
            this.center = center;
            this.radius = radius;
            this.q = -radius;
            this.r = Math.max(-radius, 0);
        }

        @Override
        public boolean hasNext() {
            return r <= radius + Math.min(0, -q) || q < radius;
        }

        @Override
        public HexCoord next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (r > radius + Math.min(0, -q)) {
                q++;
                r = -radius + Math.max(0, -q);
            }
            HexCoord out = new HexCoord(center.q + q, center.r + r);
            r++;
            return out;
        }
    }
}
