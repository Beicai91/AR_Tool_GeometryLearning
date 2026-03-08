package app;

import processing.core.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class SegmentManager {
    private ArrayList<LiveSegment> liveSegs = new ArrayList<>();
    private float moveThreshold;
    private double[] triangleSegLens = new double[3];

    public SegmentManager(float moveThreshold) {
        this.moveThreshold = moveThreshold;
    }

    public void updateLiveSegments(ArrayList<TrackedMarker> markers, MarkerManager markerManager) {

        HashSet<Integer> curIds = new HashSet<>();
        for (Marker m: markers) 
            curIds.add(m.id);
        
        Iterator<LiveSegment> iter = this.liveSegs.iterator();
        while (iter.hasNext()) {
            LiveSegment liveSeg = iter.next();

            //One or both markers disappeared -> remove line
            //if (!curIds.contains(liveSeg.getMarker1().id) || !curIds.contains(liveSeg.getMarker2().id)) {
                //iter.remove();
                //continue;
            //}

            TrackedMarker updated1 = markerManager.findMarkerById(liveSeg.getMarker1().id);
            TrackedMarker updated2 = markerManager.findMarkerById(liveSeg.getMarker2().id);
            if (updated1 != null && updated2 != null) {
                //Every frame markers are newly created, always refresh marker references used by lines
                liveSeg.setMarker1(new TrackedMarker(updated1.pos, updated1.color, updated1.id));
                liveSeg.setMarker2(new TrackedMarker(updated2.pos, updated2.color, updated2.id));
                if (PVector.dist(liveSeg.getStartPos(), updated1.pos) > this.moveThreshold || PVector.dist(liveSeg.getEndPos(), updated2.pos) > this.moveThreshold) {
                    liveSeg.setStartPos(updated1.pos.copy());
                    liveSeg.setEndPos(updated2.pos.copy());
                }
            }
        }
    }

    public ArrayList<Triangle> findTriangles(ArrayList<TrackedMarker> markers) {
        ArrayList<Triangle> triangles = new ArrayList<>();
        int markerNum = markers.size();

        for (int i = 0; i < markerNum; i++) {
            for (int j = i + 1; j < markerNum; j++) {
                for (int k = j + 1; k < markerNum; k++) {
                    TrackedMarker a = markers.get(i);
                    TrackedMarker b = markers.get(j);
                    TrackedMarker c = markers.get(k);

                    boolean ab = false;
                    boolean bc = false;
                    boolean ac = false;
                    for (LiveSegment liveSeg: this.liveSegs) {
                        if (liveSeg.connectedBy(a, b)) {
                            ab = true;
                            this.triangleSegLens[0] = getLen(a, b);
                        }
                        if (liveSeg.connectedBy(b, c)) {
                            bc = true;
                            this.triangleSegLens[1] = getLen(b, c);
                        }
                        if (liveSeg.connectedBy(a, c))
                        {
                            ac = true;
                            this.triangleSegLens[2] = getLen(a, c);
                        }
                        if (ab && bc && ac)
                            break;
                    }
                    if (ab && bc && ac) {
                        Arrays.sort(this.triangleSegLens);
                        triangles.add(new Triangle(a, b, c));    
                    }
                }
            }
        }
        return (triangles);
    }

    public void addLiveSeg(LiveSegment newSeg) {
        if (newSeg == null)
            return;
        if (!this.formASeg(newSeg.getMarker1(), newSeg.getMarker2()))
            this.liveSegs.add(newSeg);
    }

    public void removeLiveSeg(LiveSegment targetSeg) {
        this.liveSegs.remove(targetSeg);
    }
    
    public ArrayList<LiveSegment> getLiveSegs() {
        return (this.liveSegs);
    }

    public double[] getProjTriangleSegLens() {
        return (this.triangleSegLens);
    }

    public void clearSegStorage() {
        this.liveSegs.clear();
    }

    public boolean formASeg(TrackedMarker a, TrackedMarker b) {
        for (LiveSegment liveSeg: this.liveSegs) {
            if (liveSeg.connectedBy(a, b))
                return (true);
        }
        return (false);
    }
    
    private double getLen(TrackedMarker a, TrackedMarker b) {
        double dx = a.pos.x - b.pos.y;
        double dy = b.pos.x - b.pos.y;
        return (Math.sqrt(dx * dx + dy * dy));
    }
}
