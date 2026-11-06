package adris.altoclef.util.progresscheck;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PickupProgressMonitorTest {
    private final PickupProgressMonitor monitor = new PickupProgressMonitor(25_000, 40_000, 0.25);
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test void stationaryPickupIsAbandonedAtTheExistingIdleBudget() {
        assertFalse(monitor.stalled(a, 0, 16));
        assertFalse(monitor.stalled(a, 24_999, 16));
        assertTrue(monitor.stalled(a, 25_000, 16));
    }

    @Test void actualProgressResetsIdleButNotTheTotalPursuitBudget() {
        assertFalse(monitor.stalled(a, 0, 100));
        assertFalse(monitor.stalled(a, 20_000, 64));
        assertFalse(monitor.stalled(a, 39_999, 36));
        assertTrue(monitor.stalled(a, 40_000, 16));
    }

    @Test void oscillatingOrRestartingPathSearchesAreNotPhysicalProgress() {
        assertFalse(monitor.stalled(a, 0, 9));
        assertFalse(monitor.stalled(a, 10_000, 16));
        assertFalse(monitor.stalled(a, 20_000, 9));
        assertTrue(monitor.stalled(a, 25_000, 9));
    }

    @Test void switchingTargetsCannotResetAnEarlierPursuit() {
        assertFalse(monitor.stalled(a, 0, 16));
        assertFalse(monitor.stalled(b, 10_000, 9));
        assertTrue(monitor.stalled(a, 25_000, 16));
        assertFalse(monitor.stalled(b, 25_000, 4));
    }

    @Test void completedOrDeferredDropsGetANewBudgetWhenExplicitlyRetried() {
        assertFalse(monitor.stalled(a, 0, 16));
        assertTrue(monitor.stalled(a, 25_000, 16));
        monitor.forget(a);
        assertFalse(monitor.stalled(a, 70_000, 16));
    }

    @Test void changingWorldsClearsThePreviousWorldsPursuits() {
        assertFalse(monitor.stalled(a, 0, 16));
        monitor.clear();
        assertFalse(monitor.stalled(a, 25_000, 16));
    }

    @Test void oldUnvisitedTargetsAreRetired() {
        assertFalse(monitor.stalled(a, 0, 16));
        assertFalse(monitor.stalled(b, 50_000, 9));
        assertFalse(monitor.stalled(a, 50_001, 16));
    }
}
