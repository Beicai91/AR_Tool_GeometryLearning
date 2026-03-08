package app;

import processing.core.PApplet;
import processing.core.PVector;
import java.util.ArrayList;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.util.Iterator;

import org.bytedeco.opencv.cvkernels;
import org.opencv.core.*;

//open another processing window and display it on the projector
public class ProjectorWindow extends PApplet {
    private ArrayList<LiveSegment> connections = new ArrayList<>();
    private ArrayList<Triangle> triangles = new ArrayList<>();
    private final int width = 1280, height = 720;
    private Rectangle projectorDisplay;
    private ArrayList<PVector> calibDots = null;
    private Mat homography;
    StaticSegment candidateProlongation = null;
    ArrayList<StaticSegment> finalProlongations = new ArrayList<>();
    private StaticSegment baseSeg = null;
    private StaticSegment baseSegCpy;
    private boolean giveHint = false;

    public void settings() {
        size(this.width, this.height); //temporary default size of window for startup rendering
    }

    public void setup() {
        noCursor();
        //get two displays' coordinates, width and height
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] displays = ge.getScreenDevices();
        this.projectorDisplay = displays[1].getDefaultConfiguration().getBounds();
        //move this processing window to the projector display
        surface.setLocation(this.projectorDisplay.x, this.projectorDisplay.y);
        surface.setSize(this.projectorDisplay.width, this.projectorDisplay.height); //resize processing window to adjust to projector resolution
    }

    public void draw() {
        background(220);
        if (this.calibDots != null) {
            fill(0);
            noStroke();
            for (PVector p: this.calibDots) 
                ellipse(p.x, p.y, 20, 20);
        }
        if (this.homography == null) 
            return;
        this.calibDots.clear();
        this.drawLiveSegments();
        this.drawTriangles();
        if (this.candidateProlongation != null)
            this.drawSingleSeg(this.candidateProlongation);
        if (this.finalProlongations.size() != 0)
            this.drawMultipleSegs(this.finalProlongations);
        if (this.baseSeg != null)
            this.drawSingleSeg(this.baseSeg);
        if (this.giveHint)
            drawHintLine();
    }

    private void drawLiveSegments() {
        stroke(255, 0, 0, 60);
        strokeWeight(15);
        if (this.connections.size() == 0)
            return;
        for (LiveSegment connection: this.connections) {
            //convert camera-space coordination to projector-space coordinates
            Point startCam = new Point(connection.getStartPos().x, connection.getStartPos().y);
            Point endCam = new Point(connection.getEndPos().x, connection.getEndPos().y);
            MatOfPoint2f camPoints = new MatOfPoint2f(startCam, endCam);
            MatOfPoint2f projMatPoints = new MatOfPoint2f();
            Core.perspectiveTransform(camPoints, projMatPoints, this.homography);

            double[] p1 = projMatPoints.get(0, 0);
            double[] p2 = projMatPoints.get(1, 0);

            line((float)p1[0], (float)p1[1], (float)p2[0], (float)p2[1]);
        }
    }

    private void drawTriangles() {
        noStroke();
        fill(0, 255, 0, 80);
        if (this.triangles.size() == 0)
            return;
        for (Triangle t: this.triangles) {
            Point aCam = new Point(t.a.pos.x, t.a.pos.y);
            Point bCam = new Point(t.b.pos.x, t.b.pos.y);
            Point cCam = new Point(t.c.pos.x, t.c.pos.y);

            MatOfPoint2f camPoints = new MatOfPoint2f(aCam, bCam, cCam);
            MatOfPoint2f projMatPoints = new MatOfPoint2f();
            Core.perspectiveTransform(camPoints, projMatPoints, this.homography);

            double[] aProj = projMatPoints.get(0, 0);
            double[] bProj = projMatPoints.get(1, 0);
            double[] cProj = projMatPoints.get(2, 0);

            triangle((float)aProj[0], (float)aProj[1],
                     (float)bProj[0], (float)bProj[1],
                     (float)cProj[0], (float)cProj[1]
            );
        }
    }

    private void drawSingleSeg(StaticSegment seg) {
        stroke(255, 0, 0, 20);
        strokeWeight(15);
        if (seg == null)
            return;
        Point startCam = new Point(seg.a.x, seg.a.y);
        Point endCam = new Point(seg.b.x, seg.b.y);
        MatOfPoint2f camPoints = new MatOfPoint2f(startCam, endCam);
        MatOfPoint2f projMatPoints = new MatOfPoint2f();
        Core.perspectiveTransform(camPoints, projMatPoints, this.homography);

        double[] p1 = projMatPoints.get(0, 0);
        double[] p2 = projMatPoints.get(1, 0);

        line((float)p1[0], (float)p1[1], (float)p2[0], (float)p2[1]);
    }

    private void drawMultipleSegs(ArrayList<StaticSegment> segs) {
        for (StaticSegment seg: segs)
            this.drawSingleSeg(seg);
    }

    private void drawHintLine() {
        stroke(255, 0, 0, 20);
        strokeWeight(15);
        //Infinite line direction
        PVector A = this.baseSegCpy.a;
        PVector B = this.baseSegCpy.b;
        PVector lineDir = PVector.sub(B, A).normalize();

        float L = max(this.width, this.height) * 2;
        PVector p1 = PVector.add(A, PVector.mult(lineDir, L));
        PVector p2 = PVector.add(A, PVector.mult(lineDir, -L));

    

        Point startCam = new Point(p1.x, p1.y);
        Point endCam = new Point(p2.x, p2.y);
        MatOfPoint2f camPoints = new MatOfPoint2f(startCam, endCam);
        MatOfPoint2f projMatPoints = new MatOfPoint2f();
        Core.perspectiveTransform(camPoints, projMatPoints, this.homography);

        double[] projP1 = projMatPoints.get(0, 0);
        double[] projP2 = projMatPoints.get(1, 0);
        line((float)projP1[0], (float)projP1[1], (float)projP2[0], (float)projP2[1]);
        
  
    }

    public void drawTargetProlongation(StaticSegment candidateProlongation, TrackedMarker tm1, TrackedMarker tm2) {
        Iterator<LiveSegment> iter = this.connections.iterator();
        while (iter.hasNext()) {
            LiveSegment target = iter.next();
            if (target.connectedBy(tm1, tm2))
                iter.remove();
        }
        this.candidateProlongation = candidateProlongation;
        this.baseSeg = null;
    }

    public void drawTargetBaseSeg(StaticSegment baseSeg, TrackedMarker tm1, TrackedMarker tm2) {
        Iterator<LiveSegment> iter = this.connections.iterator();
        while (iter.hasNext()) {
            LiveSegment target = iter.next();
            if (target.connectedBy(tm1, tm2))
                iter.remove();
        }
        this.baseSeg = baseSeg;
        this.baseSegCpy = baseSeg;
        this.candidateProlongation = null;
    }

    public void registerFinalProlongation() {
        this.finalProlongations.add(this.candidateProlongation);
    }

    public void freecandidateProlongation() {
        this.candidateProlongation = null;
    }

    public void giveHint() {
        this.giveHint = true;
    }

    public void removeHint() {
        this.giveHint = false;
    }

    public void setConnections(ArrayList<LiveSegment> liveSegs) {
        this.connections = new ArrayList<>(liveSegs);
    }

    public void setTriangles(ArrayList<Triangle> triangles) {
        this.triangles = triangles;
    }

    public void showCalibrationDots(ArrayList<Point> projPoints) {
        this.calibDots = new ArrayList<PVector>();
        for (Point p: projPoints) {
            this.calibDots.add(new PVector((float)p.x, (float)p.y));
        }
    }

    public int getW() {
        return (this.width);
    }

    public int getH() {
        return (this.height);
    }

    public void setHomography(Mat homography) {
        this.homography = homography;
    }

    public void eraseAllSegsFromWin() {
        if (this.connections.size() != 0)
            this.connections.clear();
        if (this.baseSeg != null)
            this.baseSeg = null;
        if (this.candidateProlongation != null)
            this.candidateProlongation = null;
        if (this.finalProlongations.size() != 0)
            this.finalProlongations.clear();
        this.removeHint();
    }

    public void eraseAllTrianglesFromWin() {
        this.triangles.clear();
    }
}
