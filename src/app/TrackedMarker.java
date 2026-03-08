package app;

import processing.core.*;

public class TrackedMarker extends Marker {
    private int missedFrames = 0;

    public TrackedMarker(PVector pos, int color, int id) {
        super(pos, color, id);
    }

    public void markSeen(PVector newPos) {
        this.pos = newPos;
        this.missedFrames = 0;
    }

    public void markMissing() {
        this.missedFrames++;
    }

    public int getMissedFrames() {
        return (this.missedFrames);
    }

}