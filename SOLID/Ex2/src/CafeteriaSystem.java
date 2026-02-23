import java.util.*;

public class CafeteriaSystem {
    private final Map<String, MenuItem> menu = new LinkedHashMap<>();
    private final TaxCalculator taxCalc;
    private final DiscountCalculator discCalc;
    private final InvoiceStore store;
    private final InvoiceFormatter formatter;
    private int invoiceSeq = 1000;

    public CafeteriaSystem(TaxCalculator taxCalc, DiscountCalculator discCalc, InvoiceStore store) {
        this.taxCalc = taxCalc;
        this.discCalc = discCalc;
        this.store = store;
        this.formatter = new InvoiceFormatter();
    }

    public void addToMenu(MenuItem i) { menu.put(i.id, i); }

    public void checkout(String customerType, List<OrderLine> lines) {
        String invId = "INV-" + (++invoiceSeq);

        double subtotal = 0.0;
        for (OrderLine l : lines) {
            MenuItem item = menu.get(l.itemId);
            subtotal += item.price * l.qty;
        }

        double taxPct = taxCalc.taxPercent(customerType);
        double tax = subtotal * (taxPct / 100.0);
        double discount = discCalc.discountAmount(customerType, subtotal, lines.size());
        double total = subtotal + tax - discount;

        String printable = formatter.format(invId, lines, menu, subtotal, taxPct, tax, discount, total);
        System.out.print(printable);

        store.save(invId, printable);
        System.out.println("Saved invoice: " + invId + " (lines=" + store.countLines(invId) + ")");
    }
}
