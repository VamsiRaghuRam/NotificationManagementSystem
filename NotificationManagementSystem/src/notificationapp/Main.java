package notificationapp;

import java.util.Scanner;
import notificationapp.model.*;
import notificationapp.service.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter DND duration in minutes (simulated as seconds): ");
        int time = sc.nextInt();
        sc.nextLine();

        TimerManager timer = new TimerManager(time);
        NotificationManager manager = new NotificationManager(timer);

        timer.start();

        // Wait until DND actually starts before prompting user
        while (!timer.isStarted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        manager.start();

        System.out.println("\nInstructions:");
        System.out.println("1. Type 'exit' as sender name to stop the simulation.");
        System.out.println("2. Send a message containing 'urgent' to notify the user immediately.");
        System.out.println("3. Type 'stopDND' as sender name to end DND early.");

        while (timer.isAlive()) {
            System.out.print("\nEnter sender name: ");
            String sender = sc.nextLine();
            if (sender.equalsIgnoreCase("exit")) 
            	break;
            if (sender.equalsIgnoreCase("stopDND")) {
                timer.stopDND();
                continue;
            }
            System.out.print("Is it a Call or Message (C/M)? ");
            char type = sc.nextLine().toUpperCase().charAt(0);

            if (type == 'C') {
                manager.receiveNotification(new CallNotification(sender, false));
            } else if (type == 'M') {
                System.out.print("Enter message text: ");
                String msg = sc.nextLine();
                manager.receiveNotification(new MessageNotification(sender, msg));
            }
        }

        sc.close();
        System.out.println("Simulation ended.");
    }
}
