package app;
import g4p_controls.*;

public class MarkerButton {
    public GButton btn;
    public int color; 
    public TrackedMarker marker;
    public boolean clicked;
    private float posX;
    private float posY;

    public MarkerButton(GButton btn, int color, TrackedMarker marker) {
        this.btn = btn;
        this.color = color;
        this.marker = marker;
        this.clicked = false;
        this.posX = btn.getX();
        this.posY = btn.getY();
    }

    public void toggleClicked() {
        if (this.clicked == false)
            this.clicked = true;
        else
            this.clicked = false;
    }

    public void toggleBorderColor() {
        if (!this.clicked) 
            this.btn.setLocalColor(4, 255);
        else
            this.btn.setLocalColor(4, 200);
    }

    public void updateMarkerInfo(TrackedMarker marker) {
        this.marker = marker;
    }

}