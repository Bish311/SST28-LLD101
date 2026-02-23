/**
 * Contract:
 * - Must accept any non-null Notification and never throw exceptions.
 * - Each sender uses the fields relevant to its channel.
 * - If a required field is missing or invalid for the channel, the sender
 *   handles the error gracefully (prints an error message, adds to audit).
 * - All senders add an audit entry for every send attempt.
 */
public abstract class NotificationSender {
    protected final AuditLog audit;
    protected NotificationSender(AuditLog audit) { this.audit = audit; }
    public abstract void send(Notification n);
}
