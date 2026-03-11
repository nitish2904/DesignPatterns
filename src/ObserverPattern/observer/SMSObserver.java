package ObserverPattern.observer;

import ObserverPattern.Subject;

public class SMSObserver implements Observer {
    private Subject subject;

    public SMSObserver(Subject subject) {
        this.subject = subject;
    }

    @Override
    public void update() {
        System.out.println("User notified via SMS :)");
    }
}
