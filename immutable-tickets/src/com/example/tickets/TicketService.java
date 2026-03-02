package com.example.tickets;

import java.util.ArrayList;
import java.util.List;

public class TicketService {

    public IncidentTicket createTicket(String id, String reporterEmail, String title) {
        return new IncidentTicket.Builder()
                .id(id)
                .reporterEmail(reporterEmail)
                .title(title)
                .priority("MEDIUM")
                .source("CLI")
                .customerVisible(false)
                .tags(List.of("NEW"))
                .build();
    }

    public IncidentTicket escalateToCritical(IncidentTicket t) {
        List<String> updated = new ArrayList<>(t.getTags());
        updated.add("ESCALATED");
        return t.toBuilder()
                .priority("CRITICAL")
                .tags(updated)
                .build();
    }

    public IncidentTicket assign(IncidentTicket t, String assigneeEmail) {
        return t.toBuilder()
                .assigneeEmail(assigneeEmail)
                .build();
    }
}
