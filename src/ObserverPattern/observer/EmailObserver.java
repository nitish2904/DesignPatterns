package ObserverPattern.observer;

import ObserverPattern.Subject;

public class EmailObserver implements Observer {
    private Subject subject;

    public EmailObserver(Subject subject) {
        this.subject = subject;
    }

    @Override
    public void update() {
        System.out.println("User notified via Email :)");
    }
}
