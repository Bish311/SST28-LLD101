import java.util.*;

public class StandardAddOnPricing implements AddOnPricing {
    private final Map<AddOn, Double> prices = new EnumMap<>(AddOn.class);

    public StandardAddOnPricing() {
        prices.put(AddOn.MESS, 1000.0);
        prices.put(AddOn.LAUNDRY, 500.0);
        prices.put(AddOn.GYM, 300.0);
    }

    @Override
    public double price(AddOn addOn) {
        return prices.getOrDefault(addOn, 0.0);
    }
}
