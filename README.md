# NotificationManagementSystem

## **1. Project Description**

The Notification Management System is a Java-based console application that allows users to activate a “Do Not Disturb (DND)” mode. While in DND, incoming calls and messages are temporarily stored, and urgent messages can bypass DND to alert the user immediately. The system also logs missed notifications to a file with clear, human-readable timestamps, and allows the user to stop DND manually before the timer ends.

**Key Features:**

* Activate DND for a specified duration.
* Queue non-urgent notifications and display them after DND ends.
* Immediate alert for messages marked as "urgent".
* Manual termination of DND before the set duration.
* Logs all notifications for the current session in a clean, readable format.

---

## **2. Problem Statement**

In daily life, users are often interrupted by unwanted calls or messages during focused work, meetings, or rest periods. Existing DND functionalities in phones either block all notifications or require manual handling.

**The problem:** Users need a system to selectively manage notifications:

1. Queue non-urgent messages/calls during DND.
2. Allow urgent notifications to bypass DND.
3. Provide clear logs of missed notifications.
4. Enable early termination of DND if necessary.

---

## **3. System Objectives**

The objectives of this system are:

1. **Manage Notifications Effectively:**

   * Temporarily store non-urgent notifications.
   * Alert urgent notifications immediately.

2. **Flexible DND Control:**

   * Start DND for a specified duration.
   * Stop DND manually before the timer ends.

3. **Logging & Tracking:**

   * Maintain session-specific logs with timestamp, sender, type, and message.

4. **Demonstrate OOP Concepts:**

   * Use inheritance, polymorphism, encapsulation, and abstraction.

5. **Use Collections and Multithreading:**

   * Queue and PriorityQueue for notifications.
   * Threads for DND timer and notification handling.

---

## **4. System Design & Architecture**

### **4.1 Architecture Overview**

```
          +-----------------+
          |     Main.java   |
          |  User Interface |
          +--------+--------+
                   |
                   v
          +-----------------+
          | NotificationMgr |
          |  Handles queue  |
          |  Alerts & Logs  |
          +--------+--------+
                   |
        --------------------------
        |                        |
        v                        v
+----------------+       +----------------+
|  TimerManager  |       |  FileHandler   |
|  DND Timer     |       | Writes logs    |
+----------------+       +----------------+
        |
        v
Non-urgent notifications queued
Urgent notifications alerted immediately
```

**Explanation:**

* `Main.java` handles user input and simulates incoming notifications.
* `NotificationManager` queues notifications, alerts urgent messages, and writes logs.
* `TimerManager` controls DND timing and allows early stopping.
* `FileHandler` stores session notifications to a text file.

---

### **4.2 Class Hierarchy**

```
Notification (abstract)
├── CallNotification
└── MessageNotification

NotificationManager
TimerManager
FileHandler
Main
```

* `Notification` defines common fields: sender, time, urgent, abstract methods: `alertUser()`, `getType()`.
* `CallNotification` and `MessageNotification` extend `Notification`.
* `NotificationManager` handles queues and alert logic.
* `TimerManager` handles DND timer with multithreading.
* `FileHandler` saves notifications to a file.

---

### **4.3 UML Diagram**

```
+---------------------+
|   Notification      | <<abstract>>
+---------------------+
| - sender: String    |
| - time: LocalDateTime|
| - urgent: boolean   |
+---------------------+
| + alertUser(): void |
| + getType(): String |
| + toString(): String|
+---------------------+
           ^
           |
   ---------------------
   |                   |
+----------------+  +-------------------+
| CallNotification|  | MessageNotification|
+----------------+  +-------------------+
|                |  | - message: String |
| + alertUser()  |  | + alertUser()     |
| + getType()    |  | + getType()       |
+----------------+  +-------------------+
```

```
+---------------------+         +------------------+
| NotificationManager |         | TimerManager     |
+---------------------+         +------------------+
| - notificationQueue |         | - durationMinutes|
| - missedList        |         | - active         |
+---------------------+         +------------------+
| + receiveNotification()       | + run()          |
| + run()                       | + stopDND()      |
+---------------------+         +------------------+
           |
           v
      FileHandler
+------------------+
| + saveNotifications() |
+------------------+
```

---

## **5. Implementation Details**

### **5.1 Packages**

```
notificationapp/
 ├── Main.java
 ├── model/
 │    ├── Notification.java
 │    ├── CallNotification.java
 │    └── MessageNotification.java
 ├── service/
 │    ├── NotificationManager.java
 │    └── TimerManager.java
 └── utils/
      └── FileHandler.java
```

---

### **5.2 Key Features in Implementation**

1. **OOP Concepts:**

   * **Abstraction:** `Notification` is abstract, implemented by Call and Message.
   * **Inheritance & Polymorphism:** Different notifications share interface but behave differently.
   * **Encapsulation:** Fields are private/protected, accessed via methods.

2. **Collections Framework:**

   * `Queue<Notification>` for non-urgent notifications.
   * `PriorityQueue` for urgent messages (if implemented).
   * `ArrayList` for logging.

3. **Multithreading:**

   * `TimerManager` runs as a separate thread.
   * `NotificationManager` runs to check and process notifications.

4. **File I/O:**

   * Logs stored in `notifications_log.txt` **overwritten** each run.

5. **DND Control:**

   * Manual stop via `stopDND` command.
   * Urgent messages bypass DND automatically.

---

### **5.3 Sample Console Instructions**

```
Enter DND duration in minutes (simulated as seconds): 30
DND started for 30 minutes...

Instructions:
1. Type 'exit' as sender name to stop the simulation.
2. Send a message containing 'urgent' to notify the user immediately.
3. Type 'stopDND' as sender name to end DND early.

Enter sender name: Vamsi
Is it a Call or Message (C/M)? M
Enter message text: urgent
URGENT alert from Vamsi
Message from Vamsi: urgent

Enter sender name: stopDND
DND period ended. You are now available.
Missed notifications processed.
Notifications saved to notifications_log.txt
```

---

### **5.4 Notification Log Example**

```
===== Notification Log =====
[26-10-25 17:31:18] Call from: Vamsi
[26-10-25 17:31:26] Message from: Madhu - Hello
[26-10-25 17:31:39] Call from: Vineesha
============================
```

* Shows **type** (Call/Message)
* Shows **sender**
* Shows **message content** (if Message)
* Shows **timestamp** in readable format
* Only contains **current session notifications**

---


Do you want me to provide that next?
