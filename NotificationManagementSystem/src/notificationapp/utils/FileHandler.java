package notificationapp.utils;

import java.io.*;
import java.util.*;
import notificationapp.model.Notification;

public class FileHandler {

    public void saveNotifications(List<Notification> notifications) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("notifications_log.txt"))) {
            writer.write("===== Notification Log =====\n");
            for (Notification n : notifications) {
                writer.write(n.toString() + "\n");
            }
            writer.write("============================\n\n");
            System.out.println("Notifications saved to notifications_log.txt");
        } catch (IOException e) {
            System.out.println("Error writing file: " + e.getMessage());
        }
    }
}
