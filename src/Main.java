import Notification.Decorator.EmailDecorator;
import Notification.Decorator.PushNotificationDecorator;
import Notification.Implementation.SMSNotifier;
import Notification.Notifier;
import ObserverPattern.Iphone;
import ObserverPattern.Subject;
import ObserverPattern.observer.EmailObserver;
import ObserverPattern.observer.Observer;
import ObserverPattern.observer.SMSObserver;
import Pizza.BasePizza;
import Pizza.MargaritaPizza;
import Pizza.decorator.ExtraCheese;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        System.out.println("--------------------------------------------------");
        Notifier notifier = new PushNotificationDecorator(
                new EmailDecorator(
                        new SMSNotifier()
                )
        );
        notifier.send("Notification Enabled");
        System.out.println("--------------------------------------------------");

        BasePizza pizza = new ExtraCheese(new MargaritaPizza());
        System.out.println("Pizza cost : " + pizza.getCost());
        System.out.println("--------------------------------------------------");

        System.out.println("Observer Pattern tests : ");
        Subject iphone = new Iphone();
        Observer observer1 = new EmailObserver(iphone);
        Observer observer2 = new SMSObserver(iphone);
        iphone.add(observer1);
        iphone.add(observer2);
        iphone.updateState(10);
        System.out.println("--------------------------------------------------");
    }
}