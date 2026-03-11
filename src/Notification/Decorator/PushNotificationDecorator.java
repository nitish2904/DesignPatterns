package Notification.Decorator;

import Notification.Notifier;

public class PushNotificationDecorator extends BaseDecorator{
    public PushNotificationDecorator(Notifier notifier) {
        super(notifier);
    }

    @Override
    public void send(String message) {
        super.send(message);
        // pushNotificationService.sendNotification(message);
        System.out.println("Sending push notification ...");
    }
}
