package notificationapp.model;

public class CallNotification extends Notification {

    public CallNotification(String sender, boolean urgent) {
        super(sender, urgent);
    }

    @Override
    public void alertUser() {
        System.out.println("Missed Call from " + sender + (urgent ? " (URGENT)" : ""));
    }

    @Override
    public String getType() {
        return "Call";
    }
}
