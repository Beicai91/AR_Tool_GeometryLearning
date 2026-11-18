package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class MarkerTracker {

    private static final int LOSS_TOLERANCE = 20;

    private ArrayList<TrackedMarker> markerStorage =
        new ArrayList<>();

    public void updateTrackedMarkers(
        ArrayList<Marker> rawMarkers,
        ArrayList<TrackedMarker> trackedMarkers
    ) {
        HashMap<Integer, TrackedMarker> tracked =
            new HashMap<>();

        for (TrackedMarker tm : trackedMarkers) {
            tracked.put(tm.id, tm);
        }

        HashSet<Integer> rawIds = new HashSet<>();

        for (Marker rm : rawMarkers) {
            rawIds.add(rm.id);

            TrackedMarker tm = tracked.get(rm.id);

            if (tm == null) {
                tm = new TrackedMarker(
                    rm.pos, rm.color, rm.id
                );

                tracked.put(rm.id, tm);
                trackedMarkers.add(tm);
            } else {
                tm.markSeen(rm.pos);
            }
        }

        Iterator<TrackedMarker> iter =
            trackedMarkers.iterator();

        while (iter.hasNext()) {
            TrackedMarker tm = iter.next();

            if (!rawIds.contains(tm.id)) {
                int missedFrames = tm.getMissedFrames();

                if (missedFrames > LOSS_TOLERANCE) {
                    iter.remove();
                } else {
                    tm.markMissing();
                }
            }
        }

        this.markerStorage = trackedMarkers;
    }

    public TrackedMarker findMarkerById(int id) {
        for (TrackedMarker marker : markerStorage) {
            if (marker.id == id) {
                return marker;
            }
        }
        return null;
    }
}
