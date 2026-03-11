package ObserverPattern;

import ObserverPattern.observer.Observer;

import java.util.ArrayList;
import java.util.List;

public class Iphone implements Subject {

    List<Observer> observers = new ArrayList<>();
    int state = 0;

    @Override
    public void add(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void updateState(int state) {
        this.state = state;
        notifyObservers();
    }

    private void notifyObservers() {
        for(Observer observer : observers) {
            observer.update();
        }
    }


}
