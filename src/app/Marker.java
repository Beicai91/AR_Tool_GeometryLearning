package app;
import g4p_controls.GCScheme;
import processing.core.*;

public class Marker {
    //id constants
    public static final int ID_RED    = 1;
    public static final int ID_GREEN  = 2;
    public static final int ID_BLUE   = 3;
    public static final int ID_YELLOW = 4;
    public static final int ID_ORANGE = 5;

    public PVector pos;
    public int color;
    public int id;

    public Marker(PVector pos, int color, int id) {
        this.pos = pos;
        this.color = color;
        this.id = id;
    }
}
