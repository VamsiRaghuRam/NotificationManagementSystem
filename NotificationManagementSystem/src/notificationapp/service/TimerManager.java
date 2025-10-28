package notificationapp.service;

public class TimerManager extends Thread {
    private int durationMinutes;
    private volatile boolean active;   // use volatile for safe thread access
    private volatile boolean started;

    public TimerManager(int durationMinutes) {
        this.durationMinutes = durationMinutes;
        this.active = false;
        this.started = false;
    }

    @Override
    public void run() {
        active = true;
        started = true;
        System.out.println("DND started for " + durationMinutes + " seconds...");
        try {
            Thread.sleep(durationMinutes * 1000L); // simulate minutes as seconds
        } catch (InterruptedException e) {
            System.out.println("DND stopped manually before time.");
        }
        active = false;
        System.out.println("DND period ended. You are now available.");
    }

    public boolean isActive() {
        return active;
    }

    public boolean isStarted() {
        return started;
    }

    // Method to stop DND manually
    public void stopDND() {
        active = false;
        this.interrupt(); // stops sleep and ends DND immediately
    }
}
