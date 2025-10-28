package notificationapp.service;

import java.util.*;
import notificationapp.model.*;
import notificationapp.utils.FileHandler;

public class NotificationManager extends Thread {
    private Queue<Notification> notificationQueue = new LinkedList<>();
    private List<Notification> missedNotifications = new ArrayList<>();
    private PriorityQueue<Notification> urgentQueue = new PriorityQueue<>(
            (a, b) -> Boolean.compare(b.isUrgent(), a.isUrgent())
    );

    private TimerManager timer;
    private FileHandler fileHandler = new FileHandler();

    public NotificationManager(TimerManager timer) {
        this.timer = timer;
    }

    public synchronized void receiveNotification(Notification notification) {
        if (timer.isActive()) {
            if (notification.isUrgent()) {
                // Handle urgent immediately
                System.out.println("URGENT alert from " + notification.getSender());
                notification.alertUser();
            } else {
                // Only store non-urgent notifications
                notificationQueue.offer(notification);
                System.out.println("DND active. Notification stored silently.");
            }
        } else {
            // DND inactive, show immediately
            notification.alertUser();
        }
    }


    @Override
    public void run() {
        while (timer.isAlive() || timer.isActive()) {
            // Process urgent notifications if any
            while (!urgentQueue.isEmpty()) {
                Notification urgent = urgentQueue.poll();
                urgent.alertUser();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        // After DND ends, show missed notifications
        System.out.println("\n Missed Notifications:");
        while (!notificationQueue.isEmpty()) {
            Notification n = notificationQueue.poll();
            missedNotifications.add(n);
            n.alertUser();
        }

        fileHandler.saveNotifications(missedNotifications);
    }
}
