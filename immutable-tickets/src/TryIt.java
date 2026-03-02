import com.example.tickets.IncidentTicket;
import com.example.tickets.TicketService;

import java.util.List;

public class TryIt {

    public static void main(String[] args) {
        TicketService service = new TicketService();

        // build a ticket via Builder
        IncidentTicket t = service.createTicket("TCK-1001", "reporter@example.com", "Payment failing on checkout");
        System.out.println("Created: " + t);

        // "update" returns a new instance; original stays unchanged
        IncidentTicket assigned = service.assign(t, "agent@example.com");
        IncidentTicket escalated = service.escalateToCritical(assigned);
        System.out.println("\nOriginal after updates: " + t);
        System.out.println("Assigned copy:         " + assigned);
        System.out.println("Escalated copy:        " + escalated);

        // tags list is not mutable from outside
        List<String> tags = t.getTags();
        try {
            tags.add("HACKED_FROM_OUTSIDE");
            System.out.println("\nERROR: mutation succeeded (should not happen)");
        } catch (UnsupportedOperationException e) {
            System.out.println("\nTags are immutable — external add blocked.");
        }

        System.out.println("Original still intact: " + t);
    }
}
