package app;

import processing.core.*;

//In the context of this program, StaticSegment is defined as the prolonged segment. It doesn't dynamically follow the markers' movements except when markers move at the correct prolongation direction.
public class StaticSegment {
    public final PVector a;
    public final PVector b;
    public static final double SEGLEN_COMPARE_TOLERANCE = 1.02;

    public StaticSegment(PVector a, PVector b) {
        this.a = a.copy();
        this.b = b.copy();
    }

    public boolean onSameLineWith(StaticSegment baseSeg) {
        //Base segment points
        PVector A = baseSeg.a;
        PVector B = baseSeg.b;

        //Candidate prolonged segment points (this)
        PVector C = this.a;
        PVector D = this.b;

        PVector AB = PVector.sub(B, A);
        double baseLen = AB.mag();
        if (baseLen == 0)
            return (false);

        //distance from C/D to line AB
        double disCToAB = pointLineDistance(C, A, AB, baseLen);
        double disDToAB = pointLineDistance(D, A, AB, baseLen);

        double tolerance = 0.01 * baseLen;
        return (disCToAB < tolerance && disDToAB < tolerance);
    }

    public boolean isLongerThan(StaticSegment baseSeg) {
        float candLen = PVector.dist(this.a, this.b);
        float baseLen = PVector.dist(baseSeg.a, baseSeg.b);

        return candLen > (SEGLEN_COMPARE_TOLERANCE * baseLen);
    }

    // Formula to compute the distance from a point to a line to which a segment belongs: distance = area of parallelogram / base length of the segment
    private double pointLineDistance(PVector P, PVector A, PVector AB, double ABlen) {
        PVector AP = PVector.sub(P, A);
        double cross = Math.abs(AP.x * AB.y - AP.y * AB.x);
        return (cross / ABlen);
    }
}
