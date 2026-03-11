package Notification.Decorator;


import Notification.Notifier;

public class EmailDecorator extends BaseDecorator {
    public EmailDecorator(Notifier notifier) {
        super(notifier);
    }

    @Override
    public void send(String message) {
        super.send(message);
        System.out.println("Sending Email message ...");
    }
}
