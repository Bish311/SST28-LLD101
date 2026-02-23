import java.util.*;

public class HostelFeeCalculator {
    private final FakeBookingRepo repo;
    private final RoomPricing roomPricing;
    private final AddOnPricing addOnPricing;

    public HostelFeeCalculator(FakeBookingRepo repo, RoomPricing roomPricing, AddOnPricing addOnPricing) {
        this.repo = repo;
        this.roomPricing = roomPricing;
        this.addOnPricing = addOnPricing;
    }

    public void process(BookingRequest req) {
        Money monthly = calculateMonthly(req);
        Money deposit = new Money(5000.00);

        ReceiptPrinter.print(req, monthly, deposit);

        String bookingId = "H-" + (7000 + new Random(1).nextInt(1000));
        repo.save(bookingId, req, monthly, deposit);
    }

    private Money calculateMonthly(BookingRequest req) {
        double base = roomPricing.basePrice(req.roomType);

        double add = 0.0;
        for (AddOn a : req.addOns) {
            add += addOnPricing.price(a);
        }

        return new Money(base + add);
    }
}
