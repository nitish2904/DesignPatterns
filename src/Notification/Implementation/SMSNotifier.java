package Notification.Implementation;

import Notification.Notifier;

public class SMSNotifier implements Notifier {
    @Override
    public void send(String message) {
        System.out.println("sending SMS message : " + message);
    }
}
