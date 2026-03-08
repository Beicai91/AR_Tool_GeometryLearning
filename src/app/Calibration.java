package app;

import org.opencv.core.*;
import org.opencv.calib3d.Calib3d;
import processing.core.*;
import java.util.ArrayList;

public class Calibration {
    private ArrayList<Point> camPoints = new ArrayList<>();
    private ArrayList<Point> projPoints = new ArrayList<>();
    private Mat homography;
    private boolean isCalibDone = false;

    public Calibration(int projW, int projH) {
        projPoints.add(new Point(0, 0));
        projPoints.add(new Point(projW, 0));
        projPoints.add(new Point(projW, projH));
        projPoints.add(new Point(0, projH));
    }

    public void addCamPoints(float x, float y) {
        if (this.camPoints.size() < 4) {
            this.camPoints.add(new Point(x, y));
        }
        if (camPoints.size() == 4)
            computeHomography();
    }

    public void computeHomography() {   
        MatOfPoint2f camMatPoints = new MatOfPoint2f();
        MatOfPoint2f projMatPoints = new MatOfPoint2f();
        camMatPoints.fromList(this.camPoints);
        projMatPoints.fromList(this.projPoints);
        this.homography = Calib3d.findHomography(camMatPoints, projMatPoints);
        this.isCalibDone = true;
    }

    public Mat getHomography() {
        return (this.homography);
    }

    public boolean isCalibDone() {
        return (this.isCalibDone);
    }

    public ArrayList<Point> getProjPoints() {
        return (this.projPoints);
    }
}
