package app;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import java.util.ArrayList;
import java.util.Arrays;

public class TriangleManager {
    private Mat frame = new Mat();
    private Mat fgMask = new Mat(); //foreground mask
    private Mat cleanedMask = new Mat();
    private BackgroundSubtractorMOG2 bgSub = Video.createBackgroundSubtractorMOG2();
    private boolean triangleFound = false;
    private CamCapture cam;
    private Point[] trianglePts;

    private boolean backgroundCaptured = false;

    public TriangleManager(CamCapture cam) {
        this.cam = cam;
    }

    // Allow the camera to stabilize (auto-exposure, auto-white-balance, noise).
    // Build a clean background model by feeding about 1 second of frames,
    // blending each frame into the model with a learning rate of 0.1(each frame updates the background model only a little)
    public void captureBackground() {
        for (int i = 0; i < 30; i++) {
            this.cam.getCamera().read(this.frame);
            this.bgSub.apply(this.frame, this.fgMask, 0.1);
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.backgroundCaptured = true;
    }

    public void detectTriangle() {
        if (triangleFound)
            return;
        this.cam.getCamera().read(this.frame);
        if (this.frame.empty())
            return;
        //Apply background substruction: fgMask is generated with 0 for background and 255 for foreground(new object added)
        this.bgSub.apply(this.frame, this.fgMask, 0); //0: freeze the background model, no update cause this step is to detect what's new in the frame compared to the background model
        //Threshold to clean noise
        Imgproc.threshold(this.fgMask, this.cleanedMask, 50, 255, Imgproc.THRESH_BINARY);
        // Optional: morphological cleanup
        Imgproc.erode(this.cleanedMask, this.cleanedMask, Mat.ones(3,3,CvType.CV_8U));
        Imgproc.dilate(this.cleanedMask, this.cleanedMask, Mat.ones(5,5,CvType.CV_8U));

        //Get contours
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        //hierarchy is for nested contours (ie. a donut shaped object). RETR_EXTERNAL-deteect only the outmost contour so hierarchy is needed only for the function, can be ignored afterwards
        Imgproc.findContours(this.cleanedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.size() == 0)
            return;
        //Find the largest contour (triangle should be the biggest new object)
        double maxArea = 0;
        MatOfPoint biggest = null;

        for (MatOfPoint c : contours) {
            double area = Imgproc.contourArea(c);
            if (area > maxArea) {
                maxArea = area;
                biggest = c;
            }
        }

        if (biggest == null)
            return;

        // Approximate polygon
        MatOfPoint2f curve = new MatOfPoint2f(biggest.toArray());
        MatOfPoint2f approx = new MatOfPoint2f();
        double epsilon = 0.02 * Imgproc.arcLength(curve, true);
        Imgproc.approxPolyDP(curve, approx, epsilon, true);

        this.trianglePts = approx.toArray();
        if (this.trianglePts.length == 3) 
            triangleFound = true;
    }

    public Point[] getTrianglePts() {
        return (this.trianglePts);
    }

    public boolean isTriangleFound() {
        return (this.triangleFound);
    }

    public double[] getObjTriangleSegLens() {
        double[] res = new double[3];
        res[0] = getLen(this.trianglePts[0], this.trianglePts[1]);
        res[1] = getLen(this.trianglePts[0], this.trianglePts[2]);
        res[2] = getLen(this.trianglePts[1], this.trianglePts[2]);
        Arrays.sort(res);
        return (res);
    }
    
    private double getLen(Point p1, Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return (Math.sqrt(dx * dx + dy * dy));
    }
}
