package app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import processing.core.PVector;

class MarkerTrackerTest {

    private MarkerTracker tracker;
    private ArrayList<Marker> raw;
    private ArrayList<TrackedMarker> tracked;

    @BeforeEach
    void setUp() {
        tracker = new MarkerTracker();
        raw = new ArrayList<>();
        tracked = new ArrayList<>();
    }

    @Test
    void shouldAddNewMarker() {
        raw.add(
            new Marker(new PVector(100, 200), 0, 1)
        );

        tracker.updateTrackedMarkers(raw, tracked);

        assertEquals(1, trackedMarkers.size());
        assertEquals(1, trackedMarkers.get(0).id);
        assertEquals(100f, trackedMarkers.get(0).pos.x);
        assertEquals(200f, trackedMarkers.get(0).pos.y);
    }

    @Test
    void shouldIncrementMissingCounter() {
        raw.add(
            new Marker(new PVector(100, 200), 0, 1)
        );

        tracker.updateTrackedMarkers(raw, tracked);

        raw.clear();
        tracker.updateTrackedMarkers(raw, tracked);

        assertEquals(1, tracked.get(0).getMissedFrames());
    }

    //JUnit executes the test five times, each with a different value for missingUpdates. Parameterized testing is especially useful for equivalence partitioning and boundary value analysis.
    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10. 15, 20})
    void shouldPersistWithinMissingTolerence(int missingUpdates) { 

        // First detection
        raw.add(new Marker(new PVector(100, 200), 0, 1));
        tracker.updateTrackedMarkers(raw, tracked);

        // Marker disappears for X updates
        raw.clear();
        for (int i = 0; i < missingUpdates; i++) {
            tracker.updateTrackedMarkers(raw, tracked);
        }

        // Verify if marker persists in tracked markers
        assertEquals(1, tracked.size());
    }

    @Test
    void shouldRemoveMarkerOn21ndMissingUpdate() {
        raw.add(new Marker(new PVector(100, 200), 0, 1));
        tracker.updateTrackedMarkers(raw, tracked);

        raw.clear();

        // After 20 missing updates, should still present
        for (int i = 0; i < 20; i++) {
            tracker.updateTrackedMarkers(raw, tracked);
        }
        assertEquals(1, tracked.size());

        // On the 21nd update: removed
        tracker.updateTrackedMarkers(raw, tracked);
        assertTrue(tracked.isEmpty());
    }

    @Test
    void shouldResetMissingCounterUponReappearanceWithinTolerence() {
        // Arrange: marker detected at (100, 200)
        raw.add(new Marker(new PVector(100, 200), 0, 1));
        tracker.updateTrackedMarkers(raw, tracked);

        // Act: disappear for 20 frames
        raw.clear();
        for (int i = 0; i < 20; i++) {
            tracker.updateTrackedMarkers(raw, tracked);
        }

        // Assert: marker remains tracked
        assertEquals(1, tracked.size());
        assertEquals(20, tracked.get(0).getMissedFrames());

        //Act: marker reappears at a new position
        raw.add(new Marker(new PVector(150, 250), 0, 1));
        tracker.updateTrackedMarkers(raw, tracked);

        // Assert: counter resets and position updates
        assertEquals(0, tracked.get(0).getMissedFrames());
        assertEquals(150, tracked.get(0).pos.x);
        assertEquals(250, tracked.get(0).pos.y);

        // Act: marker disappears again for 20 frames
        raw.clear();
        for (int i = 0; i < 20; i++) {
            tracker.updateTrackedMarkers(raw, tracked);
        } 

        // Assert: marker should remain tracked with 20 missing frames
        assertEquals(1, tracked.size());
        assertEquals(20, tracked.get(0).getMissedFrames());
        assertEquals(150f, tracked.get(0).pos.x);
        assertEquals(250f, tracked.get(0).pos.y);
    }

    @Test
    void shouldRemainSameObjUponReappearanceWithinTolerance() {
        // Arrange: marker detected at (100, 200)raw.add(new Marker(new PVector(100, 200), 0, 1));
        tracker.updateTrackedMarkers(raw, tracked);
        TrackedMarker original = tracked.get(0);

        // Act: disappear for 20 frames the reappears
        raw.clear();
        for (int i = 0; i < 20; i++) {
            tracker.updateTrackedMarkers(raw, tracked);
        }
        raw.add(new Marker(new PVector(150, 250), 0, 1));
        tracker.updateTrackedMarkers(raw, tracked);

        // Assert
        assertSame(original, tracked.get(0)); //`assertSame(a, b)` -> Both references point to the exact same object
    }
}
