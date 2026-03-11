package Pizza.decorator;

import Pizza.BasePizza;

public class ExtraCheese extends PizzaDecorator{

    public ExtraCheese(BasePizza pizza) {
        super(pizza);
    }

    @Override
    public int getCost() {
        extraTimeMethod();
        return super.getCost() + 50;
    }

    private void extraTimeMethod() {
        for(int i = 0; i < 10000000; i++) {
            System.out.println(i);
        }
    }
}
