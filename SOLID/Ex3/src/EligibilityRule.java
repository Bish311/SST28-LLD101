public interface EligibilityRule {
    /**
     * Evaluates the rule for the given student profile.
     * Returns a non-null reason string if the rule is violated, or null if it passes.
     */
    String evaluate(StudentProfile s);
}
