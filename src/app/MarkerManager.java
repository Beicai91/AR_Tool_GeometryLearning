package app;
import processing.core.*; //PApplet, PImage, PVector, PConstants
import org.opencv.aruco.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.CC;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Collections;
import java.awt.image.BufferedImage;

import com.google.zxing.*;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

public class MarkerManager {
    private final PApplet p;
    private PImage frame;
    private int w;
    private int h;
    private ArrayList<TrackedMarker> markerStorage;
    private static final int LOSS_TOLERANCE = 20;
    private final QRCodeMultiReader multiReader = new QRCodeMultiReader();
    private final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);


    // Color threshold constants for RED
    static final float MIN_RH = 340; //340-360 or 0-20
    static final float MAX_RH = 15;
    static final float MIN_RS = 40;
    static final float MIN_RV = 30;

    // Color threshold constants for GREEN
    static final float MIN_GH = 80;
    static final float MAX_GH = 160;
    static final float MIN_GS = 40;
    static final float MIN_GV = 10;

    // Color threshold constants for YELLOW
    static final float MIN_YH = 40;
    static final float MAX_YH = 70;
    static final float MIN_YS = 40;
    static final float MIN_YV = 30;

    // Color threshold constants for BLUE
    static final float MIN_BH = 180;
    static final float MAX_BH = 260;
    static final float MIN_BS = 40;
    static final float MIN_BV = 30;

    // Color threshold constants for ORANGE
    static final float MIN_OH = 20;
    static final float MAX_OH = 35;
    static final float MIN_OS = 40;
    static final float MIN_OV = 30;

    // Color constants
    static final int RED    = 1;
    static final int GREEN  = 2;
    static final int BLUE   = 3;
    static final int YELLOW = 4;
    static final int ORANGE = 5;

    public MarkerManager(PApplet parent) {
        this.p = parent;
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    //==================================================================================//
    //`detectMarker`: zxing markers                                                 //                   
    //                                                                                  //
    //                                                                                  //   
    //==================================================================================//

    public ArrayList<Marker> detectZXingMarkers(PImage frame) {
        ArrayList<Marker> markers = new ArrayList<>();

        try {
            // Convert PImage (Processing) → BufferedImage
            BufferedImage img = (BufferedImage) frame.getNative();

            // Convert image into grayscale + binary for ZXing
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // Try to decode all visible QR codes
            Result[] results = multiReader.decodeMultiple(bitmap, hints);

            // Process each detected QR code
            for (Result result : results) {
                String qrText = result.getText(); // e.g. "RED"

                // Estimate center position of this QR
                ResultPoint[] pts = result.getResultPoints();
                if (pts == null || pts.length < 3)
                    continue;
                
                float cx = 0, cy = 0;
                int count = 0;
                for (ResultPoint p : pts) {
                    if (p != null) {
                        cx += p.getX();
                        cy += p.getY();
                        count++;
                    }
                }
                if (count == 0)
                    continue;
                cx /= count;
                cy /= count;
                PVector qrCenter = new PVector(cx, cy);
                //calculate offsets depending on QR code rotation
                      
                ResultPoint[] patterns = new ResultPoint[] {
                    pts[0], pts[1], pts[2]
                };

                ResultPoint.orderBestPatterns(patterns);
                
                ResultPoint p0 = patterns[1];
                ResultPoint p1 = patterns[0];
                if (p0 != null && p1 != null) {
                    float dx = p1.getX() - p0.getX();
                    float dy = p1.getY() - p0.getY();

                    float angleRad = (float) Math.atan2(dy, dx);
                    PVector offset = new PVector((float)Math.cos(angleRad), (float)Math.sin(angleRad)).mult(80);
                    PVector anchorPos = qrCenter.copy().add(offset);

                    // Interpret color or ID from QR text
                    int colorConstant = parseColorFromQR(qrText);
                    int displayColor = convertToColor(colorConstant);

                    // Create marker object and add to list
                    markers.add(new Marker(anchorPos, displayColor, colorConstant));
                }
               
            }

        } catch (NotFoundException e) {
            // No QR codes found in this frame — fine, just skip
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (markers);
    }

    private int parseColorFromQR(String txt) {
        txt = txt.toUpperCase();
        if (txt.contains("RED")) return RED;
        if (txt.contains("GREEN")) return GREEN;
        if (txt.contains("BLUE")) return BLUE;
        if (txt.contains("YELLOW")) return YELLOW;
        if (txt.contains("ORANGE")) return ORANGE;
        return -1;
    }

    public TrackedMarker findMarkerById(int id) {
        for (TrackedMarker m: this.markerStorage) {
            if (m.id == id)
                return (m);
        }
        return (null);
    }

    public void updateTrackedMarkers(ArrayList<Marker> rawMarkers, ArrayList<TrackedMarker> trackedMarkers) {
        HashMap<Integer, TrackedMarker> tracked = new HashMap<>();
        for (TrackedMarker tm: trackedMarkers) {
            tracked.put(tm.id, tm);
        }

        HashSet<Integer> rawIds = new HashSet<>();
        for (Marker rm: rawMarkers) {
            rawIds.add(rm.id);
            TrackedMarker tm = tracked.get(rm.id);
            if (tm == null) {
                tm = new TrackedMarker(rm.pos, rm.color, rm.id);
                tracked.put(rm.id, tm);
                trackedMarkers.add(tm);
            } else {
                tm.markSeen(rm.pos); 
            }
        }

        Iterator<TrackedMarker> iter = trackedMarkers.iterator();
        while (iter.hasNext()) {
            TrackedMarker tm = iter.next();
            if (!rawIds.contains(tm.id)) {
                int missedFrames = tm.getMissedFrames();
                if (missedFrames > MarkerManager.LOSS_TOLERANCE)
                    iter.remove();
                else
                    tm.markMissing();
            }
        }
        this.markerStorage = trackedMarkers;
    }

    //==================================================================================//
    //`detectMarker`: aruco markers                                                 //                   
    //Constraints: need opencv_contrib module to run                                    //
    //                                                                                  //   
    //==================================================================================//
    
    public ArrayList<Marker> detectArucoMarkers(Mat frame) {
        ArrayList<Marker> markers = new ArrayList<>();
        Dictionary dict = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50);
        ArrayList<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();

        Aruco.detectMarkers(frame, dict, corners, ids);
        if (!ids.empty()) {
            for (int i = 0; i < ids.rows(); i++) {
                int id = (int) ids.get(i, 0)[0];

                //get four corners of the marker
                Mat cornerMatrix = corners.get(i);
                double[] pTL = cornerMatrix.get(0, 0);
                double[] pTR = cornerMatrix.get(1, 0);
                double[] pBR = cornerMatrix.get(2, 0);
                double[] pBL = cornerMatrix.get(3, 0);
                
                //compute center of marker
                float centerX = (float) ((pTL[0] + pTR[0] + pBR[0] + pBL[0]) / 4.0);
                float centerY = (float) ((pTL[1] + pTR[1] + pBR[1] + pBL[1]) / 4.0);
                
                PVector pos = new PVector(centerX, centerY);
                markers.add(new Marker(pos, convertArucoIdToColor(id), id));
            }
        }
        return (markers);

    }

    // Private helper functions for `detectArcoMarkers`
    private int convertArucoIdToColor(int id) {
        switch (id) {
            case 0: return (this.p.color(255, 0, 0)); //red
            case 1: return (this.p.color(0, 255, 0)); //green
            case 3: return (this.p.color(0, 0, 255)); // blue
            case 4: return (this.p.color(255, 255, 0)); //yellow
            case 5: return (this.p.color(255, 165, 0)); //orange
            default: return (this.p.color(100)); // gray fallback
        }
    }

    //==================================================================================//
    //`detectMarkerOnColor` is a color-based detection                                  //                   
    //Advantage: fast                                                                   //
    //Constraints: highly affected by lightening and other environmental colors         //
    //              can't handle large images. CC crashes due to recursive overflow     //   
    //==================================================================================//

    public ArrayList<Marker> detectMarkersOnColorShape(PImage frame) {
        ArrayList<Marker> markers = new ArrayList<>();
        setFrame(frame);
        setFrameWidth(frame.width);
        setFrameHeight(frame.height);
        this.frame.loadPixels();
        int n = this.w * this.h;
        Graph graphRed = new Graph(n);
        Graph graphGreen = new Graph(n);
        Graph graphBlue = new Graph(n);
        Graph graphYellow = new Graph(n);
        Graph graphOrange = new Graph(n);

        // build graph: each pixel is a vertex
        for (int y = 0; y < this.h; y++) {
            for (int x = 0; x < this.w; x++) {
                int idx = y * this.w + x;
                int pixelColor = this.frame.pixels[idx];
                if (!isTargetColor(pixelColor))
                    continue;
                int color = classifyColor(pixelColor);
                if (color == RED) 
                    buildTargetGraph(graphRed, x, y, RED);
                else if (color == GREEN)
                    buildTargetGraph(graphGreen, x, y, GREEN);
                else if (color == BLUE)
                    buildTargetGraph(graphBlue, x, y, BLUE);
                else if (color == YELLOW)
                    buildTargetGraph(graphYellow, x, y, YELLOW);
                else if (color == ORANGE)
                    buildTargetGraph(graphOrange, x, y, ORANGE);
            }
        }

        populateMarkers(graphRed, RED, markers);
        populateMarkers(graphGreen, GREEN, markers);
        populateMarkers(graphBlue, BLUE, markers);
        populateMarkers(graphYellow, YELLOW, markers);
        populateMarkers(graphOrange, ORANGE, markers);

        return (markers);
    }

    // Private helper functions for `detectMarkersOnColors`
    private boolean isTargetColor(int pixelColor){
        float h = this.p.hue(pixelColor);
        float s = this.p.saturation(pixelColor);
        float v = this.p.brightness(pixelColor);
        return (((h >= MIN_RH || h <= MAX_RH) && s >= MIN_RS && v >= MIN_RV) || 
                ((h >= MIN_GH || h <= MAX_GH) && s >= MIN_GS && v >= MIN_GV) || 
                ((h >= MIN_BH || h <= MAX_BH) && s >= MIN_BS && v >= MIN_BV) ||
                ((h >= MIN_YH || h <= MAX_YH) && s >= MIN_YS && v >= MIN_YV) ||
                ((h >= MIN_OH || h <= MAX_OH) && s >= MIN_OS && v >= MIN_OV));
    }

    private int classifyColor(int pixelColor) {
        float h = this.p.hue(pixelColor);
        float s = this.p.saturation(pixelColor);
        float v = this.p.brightness(pixelColor);

        if ((h >= MIN_RH || h <= MAX_RH) && s >= MIN_RS && v >= MIN_RV) return RED;
        if (h >= MIN_GH && h <= MAX_GH && s >= MIN_GS && v >= MIN_GV) return GREEN;
        if (h >= MIN_BH && h <= MAX_BH && s >= MIN_BS && v >= MIN_BV) return BLUE;
        if (h >= MIN_YH && h <= MAX_YH && s >= MIN_YS && v >= MIN_YV) return YELLOW;
        if (h >= MIN_OH && h <= MAX_OH && s >= MIN_OS && v >= MIN_OV) return ORANGE;
        return -1;
    }

    private void buildTargetGraph(Graph g, int x, int y, int colorConstant) {
        int idx = y * this.w + x;

        // add edge only to right and down neighbors = same effect as checking all four neighbours nut better performance
        if (x + 1 < w && classifyColor(frame.pixels[y * w + (x + 1)]) == colorConstant)
            g.addEdge(idx, y * w + (x + 1));
        if (y + 1 < h && classifyColor(frame.pixels[(y + 1) * w + x]) == colorConstant)
            g.addEdge(idx, (y + 1) * w + x);
    }

    private void populateMarkers(Graph g, int colorConstant, ArrayList<Marker> markers) {
        CC connectedComponents = new CC(g);

        // pixels of each marker(same color) are connected in a component
        int markerNum = connectedComponents.count();
        float[] xSum = new float[markerNum];
        float[] ySum = new float[markerNum];
        int[] count = new int[markerNum];

        for (int y = 0; y < this.h; y++) {
            for (int x = 0; x < this.w; x++) {
                int idx = y * this.w + x;
                int pixelColor = this.frame.pixels[idx];
                if (classifyColor(pixelColor) == colorConstant) {
                    int markerIdx = connectedComponents.id(idx);
                    xSum[markerIdx] += x;
                    ySum[markerIdx] += y;
                    count[markerIdx]++;
                }
            }
        }

        for (int i = 0; i < markerNum; i++) {
            if (count[i] > 100) {
                float centerX = xSum[i] / count[i];
                float centerY = ySum[i] / count[i];
                float area = count[i];
                float perimeter = estimatePerimeter(i, connectedComponents);
                float circularity = (4 * PConstants.PI * area) / (perimeter * perimeter);
                if (circularity > 0.7) {
                    PVector pos = new PVector(centerX, centerY);
                    markers.add(new Marker(pos, convertToColor(colorConstant), colorConstant));
                }
            
            }
        }
    }

    private float estimatePerimeter(int componentId, CC cc)
    {
        float perimeter = 0;
        for (int y = 0; y < this.h; y++)
        {
            for (int x = 0; x < this.w; x++)
            {
                int idx = y * this.w + x;
                if (cc.id(idx) != componentId)
                    continue;
                //check four neighbours of each pixel in the blob. If one neighbour is not in blob, the pixel is on edge
                if (x == 0 || cc.id(y * this.w + (x - 1)) != componentId)
                    perimeter++;
                else if (x == this.w - 1 || cc.id(y * this.w + (x + 1)) != componentId)
                    perimeter++;
                else if (y == 0 || cc.id((y - 1) * this.w + x) != componentId)
                    perimeter++;
                else if (y == this.h - 1 || cc.id((y + 1) * this.w + x) != componentId)
                    perimeter++;
            }
        }
        return (perimeter);
    }

    private int convertToColor(int colorConstant) {
        switch (colorConstant) {
            case RED: return (this.p.color(255, 0, 0));
            case GREEN: return (this.p.color(0, 255, 0));
            case BLUE: return (this.p.color(0, 0, 255));
            case YELLOW: return (this.p.color(255, 255, 0));
            case ORANGE: return (this.p.color(255, 165, 0));
            default: return (this.p.color(0)); 
        }

    }

    //setters
    private void setFrame(PImage frame) {
        this.frame = frame;
    }

    private void setFrameWidth(int w) {
        this.w = w;
    }

    private void setFrameHeight(int h) {
        this.h = h;
    }
}