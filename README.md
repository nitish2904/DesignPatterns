# Design Patterns in Java

A collection of classic design pattern implementations in Java, with detailed UML diagrams and clean layered architecture.

## 📂 Patterns Implemented

| Pattern | Folder | Description |
|---------|--------|-------------|
| [Elevator LLD](src/Elevator/) | `src/Elevator/` | **State + Strategy + Factory** — Full elevator system with layered architecture |
| [Observer Pattern](src/ObserverPattern/) | `src/ObserverPattern/` | **Observer** — Subject-Observer notification system |
| [Decorator Pattern (Notification)](src/Notification/) | `src/Notification/` | **Decorator** — Stackable notification channels |
| [Decorator Pattern (Pizza)](src/Pizza/) | `src/Pizza/` | **Decorator** — Pizza with stackable toppings |

## 🏗️ How to Run

```bash
# Compile (from project root)
javac -d out src/Elevator/model/*.java src/Elevator/state/*.java src/Elevator/strategy/*.java src/Elevator/repository/*.java src/Elevator/service/*.java src/Elevator/controller/*.java src/Elevator/ElevatorMain.java

# Run
cd out && java Elevator.ElevatorMain
```

## 📐 Architecture Overview

Each pattern folder contains its own `README.md` with detailed UML diagrams rendered using Mermaid (GitHub native support).
