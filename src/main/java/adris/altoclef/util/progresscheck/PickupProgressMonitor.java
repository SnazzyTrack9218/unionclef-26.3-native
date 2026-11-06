package adris.altoclef.util.progresscheck;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks physical progress per drop across task reconstruction and target switches. */
public final class PickupProgressMonitor {
    private final long idleBudgetMs;
    private final long totalBudgetMs;
    private final double improvementSquared;
    private final Map<UUID, Attempt> attempts = new HashMap<>();

    public PickupProgressMonitor(long idleBudgetMs, long totalBudgetMs, double improvementSquared) {
        this.idleBudgetMs = idleBudgetMs;
        this.totalBudgetMs = totalBudgetMs;
        this.improvementSquared = improvementSquared;
    }

    public boolean stalled(UUID target, long nowMs, double distanceSquared) {
        attempts.entrySet().removeIf(entry -> nowMs - entry.getValue().lastSeenMs > totalBudgetMs);
        Attempt attempt = attempts.computeIfAbsent(target, ignored -> new Attempt(nowMs, distanceSquared));
        attempt.lastSeenMs = nowMs;
        if (distanceSquared < attempt.bestDistanceSquared - improvementSquared) {
            attempt.bestDistanceSquared = distanceSquared;
            attempt.lastProgressMs = nowMs;
        }
        return nowMs - attempt.startedMs >= totalBudgetMs
                || nowMs - attempt.lastProgressMs >= idleBudgetMs;
    }

    public void forget(UUID target) { attempts.remove(target); }
    public void clear() { attempts.clear(); }

    private static final class Attempt {
        final long startedMs;
        long lastProgressMs;
        long lastSeenMs;
        double bestDistanceSquared;

        Attempt(long nowMs, double distanceSquared) {
            startedMs = lastProgressMs = lastSeenMs = nowMs;
            bestDistanceSquared = distanceSquared;
        }
    }
}
