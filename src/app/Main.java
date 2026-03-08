package app;

import processing.core.*;
import org.opencv.videoio.Videoio;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Point;
import g4p_controls.*;
import java.util.ArrayList;

public class Main extends PApplet {
    private CamCapture cam;
    private PImage frame;
    private final int width = 1280, height = 720;
    private final int CAM_INDEX = 0;
    private MarkerManager markerManager;
    private ArrayList<Marker> rawMarkers = new ArrayList<>();
    private final float markerMoveThreshold = 1.0f;
    private Calibration calibration;
    private boolean isCalibrating = true;
    private Mat homography;
    private StaticSegment candidateProlongation = null;

    //Package-private members
    ArrayList<TrackedMarker> trackedMarkers = new ArrayList<>();
    ArrayList<MarkerButton> markerBtns = new ArrayList<MarkerButton>();
    PGraphics instructionLayer;


    GButton connectBtn;
    GButton disconnectBtn;
    GButton slideNextBtn;
    GButton slidePrevBtn;
    GButton instrNextBtn;
    GButton instrPrevBtn;
    GButton hintBtn;
    GButton prolongBtn;
    GButton endProlongBtn;

    ArrayList<MarkerButton> selected = new ArrayList<>();
    UtilsUI utilsUI;
    ProjectorWindow projector;
    SegmentManager segManager;
    TriangleManager triangleManager;
    int curStep = 1;
    boolean prolongBtnClicked = false;
    StaticSegment baseSeg = null;
    LiveSegment originalBaseSeg = null;

    AppState state = AppState.PHASE_ONE_EXPLORATION;

    public static void main(String[] args) {
        PApplet.main(Main.class.getName());
    }

    public void settings() {
        size(this.width, this.height);
    }

    public void setup() {
        this.cam = new CamCapture(this.CAM_INDEX);
        this.cam.open();
        if (!cam.isOpen()) {
            System.err.println("Camera failed to open.");
            exit();
            return;
        }
        int frameW = (int) this.cam.getCamera().get(Videoio.CAP_PROP_FRAME_WIDTH);
        int frameH = (int) this.cam.getCamera().get(Videoio.CAP_PROP_FRAME_HEIGHT);

        this.frame = createImage(frameW, frameH, RGB);
        this.markerManager = new MarkerManager(this);
        this.projector = new ProjectorWindow();
        this.utilsUI = new UtilsUI(this);
        this.segManager = new SegmentManager(markerMoveThreshold);
        this.calibration = new Calibration(this.projector.getW(), this.projector.getH());
        this.triangleManager = new TriangleManager(this.cam);

        //Create buttons
        this.prolongBtn = this.utilsUI.createUiBtn(180, 400, 550, 120, 60, "Prolonger");
        this.endProlongBtn = this.utilsUI.createUiBtn(180, 400, 650, 120, 60, "Prolongation terminé");
        this.connectBtn = this.utilsUI.createUiBtn(180, 600, 600, 120, 60, "Connecter");
        this.disconnectBtn = this.utilsUI.createUiBtn(180, 800, 600, 120, 60, "Déconnecter");
        this.slidePrevBtn = this.utilsUI.createUiBtn(180, 200, 600, 120, 60, "Section Précédente");
        this.slideNextBtn = this.utilsUI.createUiBtn(180, 1000, 600, 120, 60, "Section Suivante");
        this.instrNextBtn = this.utilsUI.createUiBtn(180, 900, 100, 100, 30, "Étape Suivante");
        this.instrPrevBtn = this.utilsUI.createUiBtn(180, 800, 100, 100, 30, "Étape Précédente");
        this.hintBtn = this.utilsUI.createUiBtn(180, 1100, 100, 100, 30, "?");

        textAlign(LEFT, TOP);

        this.instructionLayer = createGraphics(this.width, 200);
        //Start the second processing window and display it through projector
        String[] args = {"ProjectorWindow"};
        PApplet.runSketch(args, this.projector);

        //Start calibration
        this.projector.showCalibrationDots(this.calibration.getProjPoints());
    }

    public void draw() {
        if (isCalibrating) {
            proceedCalibration();
            return;
        }
        switch (this.state) {
            case PHASE_ONE_EXPLORATION:
                phaseOneExploration();
                break;
            case PHASE_ONE_TASK:
                phaseOneTask();
                break;
            case PHASE_TWO_EXPLORATION:
                phaseTwoExploration();
                break;
            case PHASE_TWO_TASK:
                phaseTwoTask();
                break;
        }
    }

    private void proceedCalibration() {
        if (this.cam.readIntoPImage(this.frame)) {
            image(this.frame, 0, 0);
            fill(255, 0, 0);
            text("Cliquer sur les 4 points projetés sur les coins pour la calibration du program", 10, 20);
            if (this.calibration.isCalibDone()) {
                this.isCalibrating = false;
                this.homography = calibration.getHomography();
                this.projector.setHomography(this.homography);
                this.connectBtn.setVisible(true);
                this.disconnectBtn.setVisible(true);
                this.slideNextBtn.setVisible(true);
                this.slidePrevBtn.setVisible(true);
            }
        }
    }

    private void phaseOneExploration() {
        if (this.cam.readIntoPImage(this.frame)) {
            this.slideNextBtn.setEnabled(true);
            this.utilsUI.drawRectZone(255, 0, 0, this.width, this.height);
            this.utilsUI.renderInstruction(this.utilsUI.getSlideOneInstruction());
            if (frameCount % 2 == 0)
                this.rawMarkers = this.markerManager.detectZXingMarkers(this.frame);
            this.markerManager.updateTrackedMarkers(this.rawMarkers, this.trackedMarkers);
            this.utilsUI.setMarkerBtns(this.trackedMarkers, this.markerBtns, 80, 60);
            this.utilsUI.setSegOpeBtnStatus(); 
            this.segManager.updateLiveSegments(this.trackedMarkers, this.markerManager);
            this.projector.setConnections(this.segManager.getLiveSegs());
            ArrayList<Triangle> triangles = this.segManager.findTriangles(this.trackedMarkers);
            this.projector.setTriangles(triangles);
        } else {
            fill(255, 50, 50);
            text("Aucun frame", 10, 20);
        }
    }

    private void phaseOneTask() {
        if (this.cam.readIntoPImage(this.frame)) {
            this.utilsUI.drawRectZone(255, 0, 0, this.width, this.height);
            this.utilsUI.renderInstruction(this.utilsUI.getSlideTwoInstruction());
            if (frameCount % 2 == 0)
                this.rawMarkers = this.markerManager.detectZXingMarkers(this.frame);
            this.markerManager.updateTrackedMarkers(this.rawMarkers, this.trackedMarkers);
            this.utilsUI.setMarkerBtns(this.trackedMarkers, this.markerBtns, 80, 60);
            this.utilsUI.setSegOpeBtnStatus();
            this.segManager.updateLiveSegments(this.trackedMarkers, this.markerManager);
            this.projector.setConnections(this.segManager.getLiveSegs());
            ArrayList<Triangle> triangles = this.segManager.findTriangles(this.trackedMarkers);
            this.projector.setTriangles(triangles);
        }
    }

    private void phaseTwoExploration() {
        if (this.cam.readIntoPImage(this.frame)) {
            this.utilsUI.drawRectZone(255, 0, 0, this.width, this.height);
            this.utilsUI.renderInstruction(this.utilsUI.getSlideThreeInstruction());
            
            if (this.curStep > 1) {
                this.instrPrevBtn.setEnabled(true);
                this.instrNextBtn.setEnabled(false);
                this.prolongBtn.setVisible(true);
                this.endProlongBtn.setVisible(true);
            }
            else {
                this.instrPrevBtn.setEnabled(false);
                this.instrNextBtn.setEnabled(true);
                this.prolongBtn.setVisible(false);
                this.endProlongBtn.setVisible(false);
            }
            if (frameCount % 2 == 0)
                this.rawMarkers = this.markerManager.detectZXingMarkers(this.frame);
            this.markerManager.updateTrackedMarkers(this.rawMarkers, this.trackedMarkers);
            this.utilsUI.setMarkerBtns(this.trackedMarkers, this.markerBtns, 80, 60);
            this.utilsUI.setSegOpeBtnStatus();
            if (!this.prolongBtnClicked) {
                this.segManager.updateLiveSegments(this.trackedMarkers, this.markerManager);
                this.projector.setConnections(this.segManager.getLiveSegs());
                return;
            }
            if (this.selected.size() == 2) {
                TrackedMarker tm1 = this.markerManager.findMarkerById(selected.get(0).marker.id);
                TrackedMarker tm2 = this.markerManager.findMarkerById(selected.get(1).marker.id);
                if (tm1 != null && tm2 != null) {
                    this.candidateProlongation = new StaticSegment(tm1.pos, tm2.pos);
                    if (this.candidateProlongation.onSameLineWith(this.baseSeg) && this.candidateProlongation.isLongerThan(this.baseSeg))
                        this.projector.drawTargetProlongation(this.candidateProlongation, tm1, tm2);
                    else
                        this.projector.drawTargetBaseSeg(this.baseSeg, tm1, tm2);
                }
            }
        }
    }

    private void phaseTwoTask() {
        if (this.cam.readIntoPImage(this.frame)) {
            this.utilsUI.drawRectZone(255, 0, 0, this.width, this.height);
            this.utilsUI.renderInstruction(this.utilsUI.getSlideFourInstruction());
            if (frameCount % 2 == 0)
                this.rawMarkers = this.markerManager.detectZXingMarkers(this.frame);
            this.markerManager.updateTrackedMarkers(this.rawMarkers, this.trackedMarkers);
            this.utilsUI.setMarkerBtns(this.trackedMarkers, this.markerBtns, 80, 60);
            this.utilsUI.setSegOpeBtnStatus();
            if (!this.prolongBtnClicked) {
                this.segManager.updateLiveSegments(this.trackedMarkers, this.markerManager);
                this.projector.setConnections(this.segManager.getLiveSegs());
                ArrayList<Triangle> triangles = this.segManager.findTriangles(this.trackedMarkers);
                this.projector.setTriangles(triangles);
                return;
            }
            if (this.selected.size() == 2) {
                TrackedMarker tm1 = this.markerManager.findMarkerById(selected.get(0).marker.id);
                TrackedMarker tm2 = this.markerManager.findMarkerById(selected.get(1).marker.id);
                if (tm1 != null && tm2 != null) {
                    this.candidateProlongation = new StaticSegment(tm1.pos, tm2.pos);
                    if (this.candidateProlongation.onSameLineWith(this.baseSeg) && this.candidateProlongation.isLongerThan(this.baseSeg))
                        this.projector.drawTargetProlongation(this.candidateProlongation, tm1, tm2);
                    else
                        this.projector.drawTargetBaseSeg(this.baseSeg, tm1, tm2);
                }
            }
        }
    }

    public void mousePressed() {
        if (isCalibrating)
            this.calibration.addCamPoints(mouseX, mouseY);
    }

    public void handleButtonEvents(GButton btn, GEvent event) {
        if (event == GEvent.CLICKED) {
            this.utilsUI.onBtnClick(btn);
        }
    }

    public void dispose(){
        if (this.cam != null)
            this.cam.close();
    }
}
