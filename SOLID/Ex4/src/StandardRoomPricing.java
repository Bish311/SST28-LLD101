import java.util.*;

public class StandardRoomPricing implements RoomPricing {
    private final Map<Integer, Double> prices = new HashMap<>();

    public StandardRoomPricing() {
        prices.put(LegacyRoomTypes.SINGLE, 14000.0);
        prices.put(LegacyRoomTypes.DOUBLE, 15000.0);
        prices.put(LegacyRoomTypes.TRIPLE, 12000.0);
        prices.put(LegacyRoomTypes.DELUXE, 16000.0);
    }

    @Override
    public double basePrice(int roomType) {
        return prices.getOrDefault(roomType, 16000.0);
    }
}
