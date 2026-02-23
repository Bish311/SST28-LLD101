import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Hostel Fee Calculator ===");

        RoomPricing roomPricing = new StandardRoomPricing();
        AddOnPricing addOnPricing = new StandardAddOnPricing();

        BookingRequest req = new BookingRequest(LegacyRoomTypes.DOUBLE, List.of(AddOn.LAUNDRY, AddOn.MESS));
        HostelFeeCalculator calc = new HostelFeeCalculator(new FakeBookingRepo(), roomPricing, addOnPricing);
        calc.process(req);
    }
}
