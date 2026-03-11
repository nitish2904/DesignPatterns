package ObserverPattern;

import ObserverPattern.observer.Observer;

public interface Subject {
    void add(Observer observer);
    void updateState(int state);

}
