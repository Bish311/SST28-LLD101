public interface DiscountCalculator {
    double discountAmount(String customerType, double subtotal, int distinctLines);
}
