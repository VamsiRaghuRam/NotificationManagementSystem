package notificationapp.model;

import java.time.format.DateTimeFormatter;

public class MessageNotification extends Notification {
    private String message;

    public MessageNotification(String sender, String message) {
        super(sender, message.toLowerCase().contains("urgent"));
        this.message = message;
    }

    @Override
    public void alertUser() {
        System.out.println("Message from " + sender + ": " + message + (urgent ? " (URGENT)" : ""));
    }

    @Override
    public String getType() {
        return "Message";
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        // Include message text in the log
        DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");
        String base = "[" + time.format(formatter) + "] " + getType() + " from: " + sender + " - " + message;
        return urgent ? base + " (URGENT)" : base;
    }
}
