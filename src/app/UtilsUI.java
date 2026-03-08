package app;
import processing.core.*; //PApplet, PImage, PVector
import g4p_controls.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;

public class UtilsUI {
    private final PApplet p;
    private Main main;
    private static final double RELATIVE_TOLERANCE_FOR_TRAINGLE_COMPARE = 0.2;

    public UtilsUI(Main main) {
        this.p = main;
        this.main = main;
    }

    public void setInstrLayer(PGraphics instructionLayer, String instruction, int posX, int posY, int color, int size) {
        instructionLayer.beginDraw();
        instructionLayer.clear();
        instructionLayer.fill(color);
        instructionLayer.textSize(size);
        instructionLayer.text(instruction, posX, posY);
        instructionLayer.endDraw();
    }

    public void drawRectZone(int zoneColor, int posX, int posY, int w, int h){
        this.p.noStroke();
        this.p.fill(zoneColor);
        this.p.rect(posX, posY, w, h);
    }

    public GButton createUiBtn(int color, float posX, float posY, float w, float h, String text) {
        GButton btn = addBtn(color, posX, posY, w, h);
        btn.setText(text);
        btn.setEnabled(false);
        btn.setVisible(false);
        return (btn);
    }

    public void setSegOpeBtnStatus() {
        ArrayList<LiveSegment> liveSegs = this.main.segManager.getLiveSegs();
        ArrayList<MarkerButton> selected = new ArrayList<>();
        for (MarkerButton mb: this.main.markerBtns) {
            if (mb.clicked)
                selected.add(mb);
        }
        if (selected.size() == 2) {
            boolean connected = false;
            for (LiveSegment liveSeg: liveSegs) {
                if (liveSeg.connectedBy(selected.get(0).marker, selected.get(1).marker))
                {
                    connected = true;
                    break;
                }
            
            }
            if (!connected) {
                this.main.connectBtn.setEnabled(true);
                this.main.disconnectBtn.setEnabled(false);
                this.main.prolongBtn.setEnabled(false);
            }
            else {
                this.main.disconnectBtn.setEnabled(true);
                this.main.connectBtn.setEnabled(false);
                this.main.prolongBtn.setEnabled(true);
            }
        }
        else
        {
            this.main.connectBtn.setEnabled(false);
            this.main.disconnectBtn.setEnabled(false);
            this.main.prolongBtn.setEnabled(false);
        }
    }

    public void setMarkerBtns(ArrayList<TrackedMarker> markers, ArrayList<MarkerButton> markerBtns, float sizeW, float sizeH) {
        int curFrameMarkerNum = markers.size();
        int previousFrameMarkerNum = markerBtns.size();
        if (previousFrameMarkerNum == 0 && curFrameMarkerNum == 0)
            return;
        ArrayList<MarkerButton> newComings = new ArrayList<MarkerButton>();

        if (previousFrameMarkerNum == 0 && curFrameMarkerNum != 0) {
            for (int i = 0; i < curFrameMarkerNum; i++) {
                TrackedMarker curFrameMarker = markers.get(i);
                GButton btn = addMarkerBtn(curFrameMarker.color, curFrameMarker.pos.x, curFrameMarker.pos.y);
                markerBtns.add(new MarkerButton(btn, curFrameMarker.color, curFrameMarker));
            }
            return;
        }

        HashSet<Integer> seenIds = new HashSet<>();
        for (int i = 0; i < curFrameMarkerNum; i++) {
            boolean existing = false;
            boolean samePos = true;
            int idxIndicator = -1;
            TrackedMarker curFrameMarker = markers.get(i);

            for (int j = 0; j < previousFrameMarkerNum; j++) {
                Marker previousFrameMarker = markerBtns.get(j).marker;

                if (curFrameMarker.id == previousFrameMarker.id) {
                    existing = true;  
                    if (curFrameMarker.pos.x != previousFrameMarker.pos.x || curFrameMarker.pos.y != previousFrameMarker.pos.y) {
                        samePos = false;
                        idxIndicator = j;
                    }
                    break;
                }
            }
            seenIds.add(curFrameMarker.id);
            if (!existing) {
                GButton btn = addMarkerBtn(curFrameMarker.color, curFrameMarker.pos.x, curFrameMarker.pos.y);
                newComings.add(new MarkerButton(btn, curFrameMarker.color, curFrameMarker));
            } else if (!samePos){
                markerBtns.get(idxIndicator).updateMarkerInfo(curFrameMarker);
            }
        }

        // clean up removed markers
        Iterator<MarkerButton> iter = markerBtns.iterator();
        while (iter.hasNext()) {
            MarkerButton target = iter.next();
            if (!seenIds.contains(target.marker.id)) {
                target.btn.dispose();
                target.btn = null;
                iter.remove(); //it loses the marker here
            }
            
        }

        //add newly added buttons to markerBtns
        int num = newComings.size();
        for (int k = 0; k < num; k++) {
            markerBtns.add(newComings.get(k));
        }
    }

    public void removeAllMarkerBtns(ArrayList<MarkerButton> markerBtns) {
        Iterator<MarkerButton> iter = markerBtns.iterator();
        while (iter.hasNext()) {
            MarkerButton target = iter.next();
            target.btn.dispose();
            target.btn = null;
            iter.remove();
        }
    }

    public void onBtnClick(GButton btn) {
        //--- Case 1: marker button clicked ---
        for (MarkerButton mb: this.main.markerBtns) {
            if (mb.btn == btn) {
                mb.toggleClicked();
                mb.toggleBorderColor();
            }
        }

        // --- Case 2: Connection button clicked ---
        if (btn == this.main.connectBtn || btn == this.main.disconnectBtn) {
            ArrayList<MarkerButton> selected = new ArrayList<>();
            for (MarkerButton mb: this.main.markerBtns) {
                if (mb.clicked)
                {
                    selected.add(mb);
                    mb.toggleClicked();
                    mb.toggleBorderColor();
                }
            }

            if (selected.size() == 2) {
                TrackedMarker m1 = selected.get(0).marker;
                TrackedMarker m2 = selected.get(1).marker;
                PVector p1 = selected.get(0).marker.pos.copy();
                PVector p2 = selected.get(1).marker.pos.copy();
                    
                boolean exists = false;
                LiveSegment targetSeg = null;
                ArrayList<LiveSegment> liveSegs = this.main.segManager.getLiveSegs();
                for (LiveSegment liveSeg: liveSegs) {
                    if (liveSeg.connectedBy(m1, m2)) {
                        exists = true;
                        targetSeg = liveSeg;
                        break;
                    }
                }

                if (!exists) {
                    this.main.segManager.addLiveSeg(new LiveSegment(m1, m2));
                    this.main.projector.setConnections(this.main.segManager.getLiveSegs());
                }
                if (exists && btn == this.main.disconnectBtn) {
                    this.main.segManager.removeLiveSeg(targetSeg);
                    this.main.projector.setConnections(this.main.segManager.getLiveSegs());
                }
            }
        }

        // --- Case 3: Next button for slide clicked ---
        if (btn == this.main.slideNextBtn ) 
        {
            goToNextSlide();
            if (this.main.trackedMarkers.size() != 0)
                this.main.trackedMarkers.clear();
            if (this.main.state == AppState.PHASE_TWO_TASK)
                this.main.prolongBtnClicked = false;
            this.main.utilsUI.removeAllMarkerBtns(this.main.markerBtns);
            this.main.segManager.clearSegStorage();
            this.main.projector.eraseAllSegsFromWin();
            this.main.projector.eraseAllTrianglesFromWin();
            this.main.projector.removeHint();
            this.setUiBtnStatus();
            this.main.prolongBtnClicked = false;
            this.main.curStep = 1;
        }

        // --- Case 4: Previous button for slide clicked ---
        if ( btn == this.main.slidePrevBtn)
        {
            goToPreviousSlide();
            if (this.main.trackedMarkers.size() != 0)
                this.main.trackedMarkers.clear();
            if (this.main.state == AppState.PHASE_TWO_EXPLORATION)
                this.main.prolongBtnClicked = false;
            this.main.utilsUI.removeAllMarkerBtns(this.main.markerBtns);
            this.main.segManager.clearSegStorage();
            this.main.projector.eraseAllSegsFromWin();
            this.main.projector.eraseAllTrianglesFromWin();
            this.main.projector.removeHint();
            this.setUiBtnStatus();
            this.main.prolongBtnClicked = false;
            this.main.curStep = 1;
        }

        // --- Case 5: Previous button for instruction clicked ---
        if (btn == this.main.instrPrevBtn) {
            this.main.curStep--;
            if (this.main.state == AppState.PHASE_TWO_EXPLORATION && this.main.curStep < 2) {
                this.main.projector.eraseAllSegsFromWin();
                this.main.hintBtn.setVisible(false);
                this.main.hintBtn.setEnabled(false);
            }
        }
        // --- Case 6: Next button for instruction clicked ---
        if (btn == this.main.instrNextBtn) {
            main.curStep++;
            if (this.main.state == AppState.PHASE_TWO_EXPLORATION && this.main.curStep > 1) {
                this.main.hintBtn.setVisible(true);
                this.main.hintBtn.setEnabled(true);
            }
        }

        // --- Case 8: Hint button clicked ---
        if (btn == this.main.hintBtn) {
            this.main.projector.giveHint();
        }

        // --- Case 9: Prolong button clicked ---
        if (btn == this.main.prolongBtn) {
            this.main.prolongBtnClicked = true;
            this.main.selected.clear();

            for (MarkerButton mb: this.main.markerBtns) {
                if (mb.clicked)
                {
                    this.main.selected.add(mb);
                    mb.toggleClicked();
                    mb.toggleBorderColor();
                }
            }
            MarkerButton mb1 = this.main.selected.get(0);
            MarkerButton mb2 = this.main.selected.get(1);
            if (this.main.segManager.formASeg(mb1.marker, mb2.marker)) {
                this.main.baseSeg = new StaticSegment(mb1.marker.pos, mb2.marker.pos);
                this.main.originalBaseSeg = new LiveSegment(mb1.marker, mb2.marker);
            }
            this.main.endProlongBtn.setEnabled(true);
        }

        // --- Case 10: EndProlong button clicked ---
        if (btn == this.main.endProlongBtn) {
            this.main.prolongBtnClicked = false;
            this.main.endProlongBtn.setEnabled(false);
            this.main.segManager.addLiveSeg(this.main.originalBaseSeg);
            this.main.projector.registerFinalProlongation();
            this.main.projector.freecandidateProlongation();
        }
    }

    public String getSlideOneInstruction() {
        switch (this.main.curStep) {
            case 1:
                return ("PHASE I - Exploration\n\nPlacer les marquers sur le tapis tactile. Utiliser les boutons Connecter&Déconnecter à faire des explorations libres.\n\nUne fois terminé, cliquez sur le bouton Section Suivante pour réaliser la tache");
            default:
                return ("Aucune instruction.");
        }
    }

    public String getSlideTwoInstruction() {
        switch (this.main.curStep) {
            case 1:
                return ("PHASE I - Tache\n\nReconstruisez le triangle cible avec les marquers et les opérations apprises dans la dernière section. \nL'enseignante supervises le processus de reconstruction et donner l'evaluation une fois terminée.");
            default:
                return ("Aucune instruction.");
        }
    }

    public String getSlideThreeInstruction() {
        switch (this.main.curStep) {
            case 1:
                return ("PHASE II - Exploration\n\nDessinez des segments en utilisant les marquers et les opérations apprises auparavant \nUne fois terminé, cliquer sur le bouton Étape Suivante.");
            case 2:
                return ("Choisissez un segment à prolonger en cliquant sur les deux marqueurs à ses extrémités et ensuite le bouton Prolonger. \nProlongez-le en déplacant les marquers. Lorsque la prolongation est terminé, cliquez sur le bouton Prolongation Terminée. \nL'utilisation du bouton ? sous l'autorisation de l'enseignant.\n\n Une fois terminé, cliquez sur le bouton Section Suivante pour réaliser la tache");
            default:
                return ("Aucune instruction.");
        }
    }

    public String getSlideFourInstruction() {
        switch (this.main.curStep) {
            case 1: 
                return ("PHASE II - Tache\n\nReconstruisez le triangle en utilisant l'objet tangible, les marquers, les opérations apprises auparavant (Connecter, Déconnecter, Prolonger).\nLes enseignants doivent superviser les opérations, donner des retours et évaluer.");
            default:
                return ("Aucune instruction.");
        }
    }

    public void renderInstruction(String instruction) {
        this.setInstrLayer(this.main.instructionLayer, instruction, 50, 50, 100, 16);
        this.p.image(this.main.instructionLayer, 0, 0);
    }

    
    //private helper functions
    private GButton addBtn(int color, float posX, float posY, float sizeW, float sizeH) {
        GButton btn = new GButton(this.p, posX, posY, sizeW, sizeH);
        btn.setLocalColorScheme(toG4PScheme(color));
        btn.setLocalColor(4, color);
        btn.setLocalColor(2, this.p.color(100));
        return (btn);
    }

    private GButton addMarkerBtn(int color, float posX, float posY) {
      
        PImage icon;
        if (color == this.p.color(255, 0, 0)) 
            icon = this.p.loadImage("RedMarker.png");
        else if (color == this.p.color(0, 255, 0))
            icon = this.p.loadImage("GreenMarker.png");
        else if (color == this.p.color(0, 0, 255))
            icon = this.p.loadImage("BlueMarker.png");
        else if (color == this.p.color(255, 255, 0))
            icon = this.p.loadImage("YellowMarker.png");
        else
            icon = this.p.loadImage("OrangeMarker.png");
        GButton btn = new GButton(this.p, posX, posY, 64, 64);
        btn.setIcon(icon, 1, GAlign.CENTER, GAlign.MIDDLE);
        btn.setText("");
        btn.setOpaque(false);
        btn.setLocalColorScheme(toG4PScheme(42));   
        return (btn);         
    }

    private void setUiBtnStatus() {
        if (this.main.state == AppState.PHASE_ONE_EXPLORATION) {
            this.main.slidePrevBtn.setEnabled(false);
            this.main.slideNextBtn.setEnabled(true);
            this.main.instrNextBtn.setVisible(false);
            this.main.instrPrevBtn.setVisible(false);
        }
        if (this.main.state == AppState.PHASE_ONE_TASK) {
            this.main.slidePrevBtn.setEnabled(true);
            this.main.slideNextBtn.setEnabled(true);
        }
        if (this.main.state == AppState.PHASE_TWO_EXPLORATION) {
            this.main.slidePrevBtn.setEnabled(true);
            this.main.slideNextBtn.setEnabled(true);
            this.main.instrNextBtn.setVisible(true);
            this.main.instrPrevBtn.setVisible(true);
            this.main.prolongBtn.setVisible(false);
            this.main.endProlongBtn.setVisible(false);
        }
        if (this.main.state == AppState.PHASE_TWO_TASK) {
            this.main.slidePrevBtn.setEnabled(true);
            this.main.slideNextBtn.setEnabled(false);
            this.main.instrNextBtn.setVisible(false);
            this.main.instrPrevBtn.setVisible(false);
            this.main.hintBtn.setVisible(false);
            this.main.prolongBtn.setVisible(true);
            this.main.endProlongBtn.setVisible(true);
        }
    }

    private void goToNextSlide() {
        switch (this.main.state) {
            case PHASE_ONE_EXPLORATION:
                this.main.state = AppState.PHASE_ONE_TASK;
                break;
            case PHASE_ONE_TASK:
                this.main.state = AppState.PHASE_TWO_EXPLORATION;
                break;
            case PHASE_TWO_EXPLORATION:
                this.main.state = AppState.PHASE_TWO_TASK;
        }
    }

    private void goToPreviousSlide() {
        switch (this.main.state) {
            case PHASE_ONE_TASK:
                this.main.state = AppState.PHASE_ONE_EXPLORATION;
                break;
            case PHASE_TWO_EXPLORATION:
                this.main.state = AppState.PHASE_ONE_TASK;
                break;
            case PHASE_TWO_TASK:
                this.main.state = AppState.PHASE_TWO_EXPLORATION;
        }
    }

    private boolean trianglesCompare(double relativeTolerance) {
        double[] objTriangle = this.main.triangleManager.getObjTriangleSegLens();
        double[] projTriangle = this.main.segManager.getProjTriangleSegLens();
        if (objTriangle.length != projTriangle.length)
            return (false);
        for (int i = 0; i < objTriangle.length; i++) {
            double diff = Math.abs(objTriangle[i] - projTriangle[i]);
            double maxLen = Math.max(objTriangle[i], projTriangle[i]);
            if (diff > relativeTolerance * maxLen)
                return (false);
        }
        return (true);
    }

    private int toG4PScheme(int color) {
        if (color == p.color(255,0,0)) {
            return GCScheme.RED_SCHEME;
        } else if (color == p.color(0,255,0)) {
            return GCScheme.GREEN_SCHEME;
        } else if (color == p.color(0,0,255)) {
            return GCScheme.BLUE_SCHEME;
        } else if (color == p.color(255,255,0)) {
            return GCScheme.YELLOW_SCHEME;
        } else if (color == p.color(255,165,0)) {
            return GCScheme.ORANGE_SCHEME;
        }
        return GCScheme.GRAY; // fallback
    }
}
