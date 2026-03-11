# 🔔 Decorator Pattern — Notification System

Implementation of the Decorator pattern to **stack notification channels** (SMS, Email, Push) on top of each other dynamically.

## Design

- **Notifier** — base interface with `send(message)`
- **SMSNotifier** — concrete base implementation
- **BaseDecorator** — abstract decorator wrapping a `Notifier`
- **EmailDecorator** / **PushNotificationDecorator** — concrete decorators adding behavior

Decorators can be stacked: `new PushNotificationDecorator(new EmailDecorator(new SMSNotifier()))` sends via all three channels.

## 📐 UML Class Diagram

```mermaid
classDiagram
    direction TB

    class Notifier{
        <<interface>>
        +send(message)
    }
    class SMSNotifier{
        +send(message)
    }
    class BaseDecorator{
        <<abstract>>
        #Notifier wrappee
        +BaseDecorator(wrappee)
        +send(message)
    }
    class EmailDecorator{
        +send(message)
    }
    class PushNotificationDecorator{
        +send(message)
    }

    Notifier <|.. SMSNotifier : implements
    Notifier <|.. BaseDecorator : implements
    BaseDecorator <|-- EmailDecorator : extends
    BaseDecorator <|-- PushNotificationDecorator : extends
    BaseDecorator --> Notifier : wraps
```

## 📂 Files

```
Notification/
├── Notifier.java                          # Base interface
├── Implementation/
│   └── SMSNotifier.java                   # Concrete component
└── Decorator/
    ├── BaseDecorator.java                 # Abstract decorator
    ├── EmailDecorator.java                # Adds email notification
    └── PushNotificationDecorator.java     # Adds push notification
```
