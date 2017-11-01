package no.rutebanken.anshar.subscription.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

@Entity
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    Instant lastHeartbeat;
    Instant lastDataReceived;
    Instant activated;
    Instant terminated;

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Instant getLastDataReceived() {
        return lastDataReceived;
    }

    public void setLastDataReceived(Instant lastDataReceived) {
        this.lastDataReceived = lastDataReceived;
    }

    public Instant getActivated() {
        return activated;
    }

    public void setActivated(Instant activated) {
        this.activated = activated;
    }

    public Instant getTerminated() {
        return terminated;
    }

    public void setTerminated(Instant terminated) {
        this.terminated = terminated;
    }
}
