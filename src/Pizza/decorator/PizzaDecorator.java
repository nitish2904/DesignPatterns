package Pizza.decorator;

import Pizza.BasePizza;

public abstract class PizzaDecorator implements BasePizza {
    private BasePizza pizza;

    public PizzaDecorator(BasePizza pizza) {
        this.pizza = pizza;
    }

    @Override
    public int getCost() {
        return pizza.getCost();
    }
}
