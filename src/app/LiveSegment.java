package app;

import processing.core.*;

//In the context of this program, LiveSegment is defined as those segments created by connecting two markers and which follow dynamically the movements of the markers.
public class LiveSegment {
    private PVector start;
    private PVector end;
    private TrackedMarker m1;
    private TrackedMarker m2;

    public LiveSegment(TrackedMarker m1, TrackedMarker m2) {
        this.m1 = m1;
        this.m2 = m2;
        this.start = m1.pos;
        this.end = m2.pos;
    }

    public boolean connectedBy(TrackedMarker mA, TrackedMarker mB) {
        return ((this.m1.id == mA.id && this.m2.id == mB.id) || (this.m1.id == mB.id && this.m2.id == mA.id));
    }

    PVector getStartPos()
    {
        return (this.start);
    }

    PVector getEndPos()
    {
        return (this.end);
    }

    TrackedMarker getMarker1() {
        return (this.m1);
    }

    TrackedMarker getMarker2() {
        return (this.m2);
    }

    void setStartPos(PVector start)
    {
        this.start = start;
    }

    void setEndPos(PVector end) {
        this.end = end;
    }

    void setMarker1(TrackedMarker m1) {
        this.m1 = m1;
    }

    void setMarker2(TrackedMarker m2) {
        this.m2 = m2;
    }
}
