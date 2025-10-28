package notificationapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Notification {
    protected String sender;
    protected LocalDateTime time;
    protected boolean urgent;

    public Notification(String sender, boolean urgent) {
        this.sender = sender;
        this.urgent = urgent;
        this.time = LocalDateTime.now();
    }

    public boolean isUrgent() {
        return urgent;
    }

    public abstract void alertUser();

    // Add abstract method to return type description
    public abstract String getType();

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        String base = "[" + time.format(formatter) + "] " + getType() + " from: " + sender;
        return urgent ? base + " (URGENT)" : base;
    }

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}
}
