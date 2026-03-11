# 👁️ Observer Pattern

Implementation of the Observer design pattern where a **Subject** (observable) notifies multiple **Observers** when its state changes.

## Design

- **Subject** holds a list of observers and notifies them on state change
- **Observer** interface defines the `update()` contract
- **Concrete Observers**: `EmailObserver`, `SMSObserver`
- **Concrete Subject**: `Iphone` (product being observed)

## 📐 UML Class Diagram

```mermaid
classDiagram
    direction TB

    class Observer{
        <<interface>>
        +update(message)
    }
    class EmailObserver{
        -String email
        +update(message)
    }
    class SMSObserver{
        -String phoneNumber
        +update(message)
    }

    class Subject{
        -List~Observer~ observers
        +addObserver(observer)
        +removeObserver(observer)
        +notifyObservers(message)
    }
    class Iphone{
        -String productName
        -boolean inStock
        +setInStock(inStock)
    }

    Observer <|.. EmailObserver : implements
    Observer <|.. SMSObserver : implements
    Subject <|-- Iphone : extends
    Subject --> Observer : notifies
```

## 📂 Files

```
ObserverPattern/
├── Subject.java          # Abstract subject with observer management
├── Iphone.java           # Concrete subject (product)
└── observer/
    ├── Observer.java      # Observer interface
    ├── EmailObserver.java # Notifies via email
    └── SMSObserver.java   # Notifies via SMS
```
