package Notification.Decorator;

import Notification.Notifier;

public abstract class BaseDecorator implements Notifier {
    protected Notifier notifier;

    public BaseDecorator(Notifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public void send(String message) {
        System.out.println("Base Decorator called for Object " + notifier);
        this.notifier.send(message);
    }
}
